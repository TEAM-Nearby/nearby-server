// 동행 글 등록과 혼밥 지도 등록이 HTTP·저장소 흐름에서 분리되는지 검증한다.
package com.sopt.nearby.place;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfile;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileStatus;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.port.out.CompanionProfileRepository;
import com.sopt.nearby.place.port.in.ResolvePlaceCacheCommand;
import com.sopt.nearby.place.port.in.ResolvePlaceCacheUseCase;
import com.sopt.nearby.place.port.out.PlaceCacheRepository;
import com.sopt.nearby.place.port.out.PlaceImageLookupPort;
import com.sopt.nearby.user.domain.model.UserAccount;
import com.sopt.nearby.user.domain.model.UserAccountStatus;
import com.sopt.nearby.user.domain.model.UserOnboardingStatus;
import com.sopt.nearby.user.domain.model.UserRole;
import com.sopt.nearby.user.port.out.UserAccountRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:soloDining231;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate", "management.health.redis.enabled=false"
})
@ActiveProfiles("local")
@AutoConfigureMockMvc
class SoloDiningPlaceRegistrationFlowTest {

    @Autowired private MockMvc mvc;
    @Autowired private UserAccountRepository users;
    @Autowired private CompanionProfileRepository profiles;
    @Autowired private PlaceCacheRepository places;
    @Autowired private ResolvePlaceCacheUseCase resolvePlace;
    @Autowired private DataSource dataSource;
    @MockitoBean private PlaceImageLookupPort imageLookup;

    @Test
    void zoomingOutAndMovingMapChangesResultsWithoutChangingUserDistance() throws Exception {
        long viewer = user();
        long near = resolvePlace.resolve(new ResolvePlaceCacheCommand(
                "map-near-" + viewer, "가까운 혼밥 식당", "Paris",
                new BigDecimal("48.8567"), new BigDecimal("2.3523"), "RESTAURANT"
        )).placeId();
        long far = resolvePlace.resolve(new ResolvePlaceCacheCommand(
                "map-far-" + viewer, "확대한 지도 안 식당", "Paris",
                new BigDecimal("48.88"), new BigDecimal("2.38"), "RESTAURANT"
        )).placeId();
        executeUpdate("insert into solo_dining_place (place_id) values (?)", near);
        executeUpdate("insert into solo_dining_place (place_id) values (?)", far);

        var legacy = mvc.perform(get("/api/solo-dining/places")
                        .with(jwt().jwt(token -> token.subject(Long.toString(viewer))))
                        .param("latitude", "48.8566").param("longitude", "2.3522")
                        .param("category", "RESTAURANT"))
                .andExpect(status().isOk()).andReturn();
        List<Map<String, Object>> legacyPlaces = JsonPath.read(
                legacy.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.places");
        assertThat(legacyPlaces).anyMatch(place -> id(place) == near).noneMatch(place -> id(place) == far);

        var expanded = mvc.perform(get("/api/solo-dining/places")
                        .with(jwt().jwt(token -> token.subject(Long.toString(viewer))))
                        .param("latitude", "48.8566").param("longitude", "2.3522")
                        .param("category", "RESTAURANT")
                        .param("southLatitude", "48.83").param("northLatitude", "48.89")
                        .param("westLongitude", "2.30").param("eastLongitude", "2.40"))
                .andExpect(status().isOk()).andReturn();
        List<Map<String, Object>> expandedPlaces = JsonPath.read(
                expanded.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.places");
        assertThat(expandedPlaces).anyMatch(place -> id(place) == near).anyMatch(place -> id(place) == far);
        Map<String, Object> farPlace = expandedPlaces.stream()
                .filter(place -> id(place) == far).findFirst().orElseThrow();
        assertThat(((Number) farPlace.get("distanceMeters")).intValue()).isGreaterThan(1000);

        var moved = mvc.perform(get("/api/solo-dining/places")
                        .with(jwt().jwt(token -> token.subject(Long.toString(viewer))))
                        .param("latitude", "48.8566").param("longitude", "2.3522")
                        .param("category", "RESTAURANT")
                        .param("southLatitude", "48.87").param("northLatitude", "48.885")
                        .param("westLongitude", "2.37").param("eastLongitude", "2.39"))
                .andExpect(status().isOk()).andReturn();
        List<Map<String, Object>> movedPlaces = JsonPath.read(
                moved.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.places");
        assertThat(movedPlaces).anyMatch(place -> id(place) == far).noneMatch(place -> id(place) == near);
        assertThat(movedPlaces.stream().filter(place -> id(place) == far).findFirst().orElseThrow()
                .get("distanceMeters")).isEqualTo(farPlace.get("distanceMeters"));
    }

    @Test
    void excludesNewCompanionPlaceForAllUsersUntilExplicitlyRegistered() throws Exception {
        long host = user();
        long viewer = user();
        String googleId = "companion-only-" + host;
        assertThat(places.findByGooglePlaceId(googleId)).isEmpty();

        long placeId = createPost(host, googleId);
        assertThat(places.findByGooglePlaceId(googleId).orElseThrow().id()).isEqualTo(placeId);
        assertThat(createPost(host, googleId)).isEqualTo(placeId);

        for (long userId : new long[]{host, viewer}) {
            for (String category : new String[]{"", "RESTAURANT"}) {
                assertThat(mapPlaces(userId, category)).noneMatch(place -> id(place) == placeId);
            }
        }

        executeUpdate("insert into solo_dining_place (place_id) values (?)", placeId);
        assertThat(mapPlaces(viewer, "RESTAURANT")).anyMatch(place -> id(place) == placeId);
        assertThat(places.findByGooglePlaceId(googleId).orElseThrow().id()).isEqualTo(placeId);
    }

    @Test
    void keepsRegisteredPlaceCategoriesAndFavoriteAfterCompanionPostCreation() throws Exception {
        long host = user();
        String googleId = "registered-place-" + host;
        long placeId = resolvePlace.resolve(new ResolvePlaceCacheCommand(googleId, "기존 혼밥 장소", "Paris",
                new BigDecimal("48.86"), new BigDecimal("2.34"), "CAFE")).placeId();
        executeUpdate("insert into solo_dining_place (place_id) values (?)", placeId);
        executeUpdate("insert into place_category (place_id, category, category_order) values (?, 'CAFE', 0)", placeId);
        executeUpdate("insert into place_category (place_id, category, category_order) values (?, 'DESSERT', 1)", placeId);
        mvc.perform(put("/api/solo-dining/places/" + placeId + "/favorite")
                        .with(jwt().jwt(token -> token.subject(Long.toString(host)))))
                .andExpect(status().isOk());

        assertThat(createPost(host, googleId)).isEqualTo(placeId);

        for (String category : new String[]{"", "CAFE", "DESSERT"}) {
            Map<String, Object> registered = mapPlaces(host, category).stream()
                    .filter(place -> id(place) == placeId).findFirst().orElseThrow();
            assertThat(registered.get("categories")).isEqualTo(List.of("CAFE", "DESSERT"));
            assertThat(registered.get("isFavorite")).isEqualTo(true);
        }
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(
                "select count(*) from solo_dining_place where place_id = ?")) {
            statement.setLong(1, placeId);
            try (var result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isEqualTo(1);
            }
        }
    }

    private long createPost(final long host, final String googleId) throws Exception {
        var result = mvc.perform(post("/api/companion-posts")
                        .with(jwt().jwt(token -> token.subject(Long.toString(host))))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"place":{"googlePlaceId":"%s","name":"동행 선택 장소","address":"Paris",
                                 "latitude":48.86,"longitude":2.34,"category":"RESTAURANT"},
                                 "meetingTimeType":"UNDECIDED","recruitmentCapacity":1,
                                 "content":"같이 식사해요","openChatUrl":"https://open.kakao.com/o/test"}
                                """.formatted(googleId)))
                .andExpect(status().isOk()).andReturn();
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        Number postId = JsonPath.read(body, "$.data.postId");
        Number placeId = JsonPath.read(body, "$.data.place.placeId");
        mvc.perform(get("/api/companion-posts/" + postId)
                        .with(jwt().jwt(token -> token.subject(Long.toString(host)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.place.googlePlaceId").value(googleId));
        return placeId.longValue();
    }

    private List<Map<String, Object>> mapPlaces(final long userId, final String category) throws Exception {
        var request = get("/api/solo-dining/places")
                .with(jwt().jwt(token -> token.subject(Long.toString(userId))))
                .param("latitude", "48.86").param("longitude", "2.34");
        if (!category.isEmpty()) {
            request.param("category", category);
        }
        var result = mvc.perform(request).andExpect(status().isOk()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.places");
    }

    private long id(final Map<String, Object> place) {
        return ((Number) place.get("placeId")).longValue();
    }

    private void executeUpdate(final String sql, final long placeId) throws SQLException {
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, placeId);
            statement.executeUpdate();
        }
    }

    private long user() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 17, 0, 0);
        long id = users.save(new UserAccount(null, UserRole.USER, UserAccountStatus.ACTIVE,
                "01012345678", now, UserOnboardingStatus.COMPLETED, now, null)).id();
        profiles.save(new CompanionProfile(null, id, "지도" + id, UserGender.FEMALE, 1998,
                null, null, new BigDecimal("4.50"), 0, CompanionProfileStatus.ACTIVE));
        return id;
    }
}
