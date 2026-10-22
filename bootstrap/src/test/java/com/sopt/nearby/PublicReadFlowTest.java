// 비회원 조회와 회원 저장 상태 및 인증 경계를 실제 HTTP·DB 흐름으로 검증한다.
package com.sopt.nearby;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sopt.nearby.place.port.in.ResolvePlaceCacheCommand;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfile;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileStatus;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.port.out.CompanionProfileRepository;
import com.sopt.nearby.place.port.in.ResolvePlaceCacheUseCase;
import com.sopt.nearby.place.port.in.ResolvePlaceImageUseCase;
import com.sopt.nearby.place.port.in.ResolvedPlaceImage;
import com.sopt.nearby.place.port.out.SoloDiningPlaceDetailsPort;
import com.sopt.nearby.place.port.out.SoloDiningPlaceDetailsResult;
import com.sopt.nearby.user.domain.model.UserAccount;
import com.sopt.nearby.user.domain.model.UserAccountStatus;
import com.sopt.nearby.user.domain.model.UserOnboardingStatus;
import com.sopt.nearby.user.domain.model.UserRole;
import com.sopt.nearby.user.port.out.UserAccountRepository;
import java.math.BigDecimal;
import java.net.URI;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:publicRead246;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate", "management.health.redis.enabled=false"
})
@ActiveProfiles("local")
@AutoConfigureMockMvc
class PublicReadFlowTest {

    @Autowired private MockMvc mvc;
    @Autowired private ResolvePlaceCacheUseCase resolvePlace;
    @Autowired private UserAccountRepository users;
    @Autowired private CompanionProfileRepository profiles;
    @Autowired private DataSource dataSource;
    @Autowired private JwtEncoder encoder;
    @MockitoBean private ResolvePlaceImageUseCase images;
    @MockitoBean private SoloDiningPlaceDetailsPort details;

    private long placeId;
    private long memberId;
    private final List<Long> createdUsers = new ArrayList<>();

    @BeforeEach
    void setUp() throws SQLException {
        memberId = user(UserOnboardingStatus.COMPLETED);
        profiles.save(new CompanionProfile(null, memberId, "회원" + memberId, UserGender.FEMALE, 1998,
                null, null, new BigDecimal("4.50"), 0, CompanionProfileStatus.ACTIVE));
        placeId = resolvePlace.resolve(new ResolvePlaceCacheCommand(
                "guest-flow-" + UUID.randomUUID(), "공개 식당", "Paris",
                new BigDecimal("48.86"), new BigDecimal("2.34"), "RESTAURANT"
        )).placeId();
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement("insert into solo_dining_place (place_id) values (?)")) {
            statement.setLong(1, placeId);
            statement.executeUpdate();
        }
        when(images.resolve(any())).thenReturn(new ResolvedPlaceImage(
                "https://example.com/place.jpg", ResolvedPlaceImage.GOOGLE_MAPS, List.of()));
        when(details.findByGooglePlaceId(anyString())).thenReturn(new SoloDiningPlaceDetailsResult(
                "guest-flow-place", "공개 식당", "Paris", null, null, null, null, null,
                null, null, List.of(), null, null, null, List.of(), null));
    }

    @AfterEach
    void cleanUp() throws SQLException {
        try (var connection = dataSource.getConnection()) {
            for (String table : List.of("solo_dining_favorite", "solo_dining_place", "place_category", "place_cache")) {
                String column = table.equals("place_cache") ? "id" : "place_id";
                try (var statement = connection.prepareStatement("delete from " + table + " where " + column + " = ?")) {
                    statement.setLong(1, placeId);
                    statement.executeUpdate();
                }
            }
            for (Long id : createdUsers) {
                try (var statement = connection.prepareStatement("delete from companion_profile where user_id = ?")) {
                    statement.setLong(1, id);
                    statement.executeUpdate();
                }
                try (var statement = connection.prepareStatement("delete from user_account where id = ?")) {
                    statement.setLong(1, id);
                    statement.executeUpdate();
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/companion-posts", "/api/companion-%70osts"})
    void keepsCompanionProfileRequirementForAuthenticatedUsers(final String path) throws Exception {
        long userWithoutProfile = user(UserOnboardingStatus.COMPANION_PROFILE_SKIPPED);
        mvc.perform(get(URI.create(path)).param("latitude", "48.86").param("longitude", "2.34")
                        .header("Authorization", "Bearer " + token(userWithoutProfile, false)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMPANION_PROFILE_REQUIRED"));
    }

    @Test
    void guestReadsCompanionPostsThroughEncodedPath() throws Exception {
        mvc.perform(get(URI.create("/api/companion-%70osts"))
                        .param("latitude", "48.86").param("longitude", "2.34"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.posts").isArray());
    }

    @Test
    void guestReadsCompanionPostsButAuthenticatedUserStillNeedsOnboarding() throws Exception {
        mvc.perform(get("/api/companion-posts").param("latitude", "48.86").param("longitude", "2.34"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.posts").isArray());
        mvc.perform(get("/api/companion-posts").param("latitude", "48.86").param("longitude", "2.34")
                        .header("Authorization", "Bearer " + token(memberId, false)))
                .andExpect(status().isOk());
        long incompleteUser = user(UserOnboardingStatus.STARTED);
        mvc.perform(get("/api/companion-posts").param("latitude", "48.86").param("longitude", "2.34")
                        .header("Authorization", "Bearer " + token(incompleteUser, false)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ONBOARDING_REQUIRED"));
    }

    @Test
    void guestsSeeFalseAndMembersSeeTheirFavoriteInBothListsAndDetail() throws Exception {
        String bearer = "Bearer " + token(memberId, false);
        mvc.perform(put("/api/solo-dining/places/{placeId}/favorite", placeId).header("Authorization", bearer))
                .andExpect(status().isOk());

        for (boolean bounded : List.of(false, true)) {
            var guest = get("/api/solo-dining/places").param("latitude", "48.86").param("longitude", "2.34");
            if (bounded) {
                guest.param("southLatitude", "48.85").param("northLatitude", "48.87")
                        .param("westLongitude", "2.33").param("eastLongitude", "2.35");
            }
            mvc.perform(guest).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.places[0].placeId").value(placeId))
                    .andExpect(jsonPath("$.data.places[0].isFavorite").value(false));
            mvc.perform(guest.header("Authorization", bearer)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.places[0].isFavorite").value(true));
        }
        var detail = get("/api/solo-dining/places/{placeId}", placeId)
                .param("latitude", "48.86").param("longitude", "2.34");
        mvc.perform(detail).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.placeId").value(placeId))
                .andExpect(jsonPath("$.data.isFavorite").value(false));
        mvc.perform(detail.header("Authorization", bearer)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isFavorite").value(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/companion-posts", "/api/solo-dining/places", "/api/solo-dining/places/1"})
    void rejectsInvalidAndExpiredTokensEvenOnPublicPaths(final String path) throws Exception {
        for (String accessToken : List.of("invalid-token", token(memberId, true))) {
            mvc.perform(get(path).param("latitude", "48.86").param("longitude", "2.34")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }
    }

    @ParameterizedTest
    @CsvSource({
            "GET,/api/companion-posts/1", "POST,/api/companion-posts",
            "GET,/api/companion-%70osts/1", "POST,/api/companion-%70osts",
            "POST,/api/companion-posts/1/companion-requests",
            "PUT,/api/solo-dining/places/1/favorite", "DELETE,/api/solo-dining/places/1/favorite",
            "GET,/api/solo-dining/favorites", "GET,/api/users/me/mypage"
    })
    void keepsMemberOnlyRoutesProtected(final String method, final String path) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method), URI.create(path)))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/companion-posts", "/api/solo-dining/places", "/api/solo-dining/places/1"})
    void validatesGuestCoordinates(final String path) throws Exception {
        mvc.perform(get(path).param("latitude", "91").param("longitude", "2.34"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsNotFoundForMissingPlaceWithoutAuthentication() throws Exception {
        mvc.perform(get("/api/solo-dining/places/9223372036854775807")
                        .param("latitude", "48.86").param("longitude", "2.34"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PLACE_NOT_FOUND"));
    }

    private long user(final UserOnboardingStatus onboardingStatus) {
        LocalDateTime now = LocalDateTime.now();
        long id = users.save(new UserAccount(null, UserRole.USER, UserAccountStatus.ACTIVE,
                null, null, onboardingStatus, now, null)).id();
        createdUsers.add(id);
        return id;
    }

    private String token(final long userId, final boolean expired) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("nearby").subject(Long.toString(userId))
                .issuedAt(now.minusSeconds(3600)).expiresAt(expired ? now.minusSeconds(600) : now.plusSeconds(3600))
                .claim("role", "USER").build();
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
