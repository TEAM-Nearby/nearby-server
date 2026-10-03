// 장소 검색의 접근 정책과 동행글 저장·상세 조회 연결을 검증한다.
package com.sopt.nearby.place;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfile;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileStatus;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.port.out.CompanionProfileRepository;
import com.sopt.nearby.place.application.SearchPlacesCommand;
import com.sopt.nearby.place.domain.exception.PlaceSearchFailedException;
import com.sopt.nearby.place.domain.exception.PlaceSearchRateLimitedException;
import com.sopt.nearby.place.domain.exception.PlaceSearchTimeoutException;
import com.sopt.nearby.place.domain.model.PlaceSearchPage;
import com.sopt.nearby.place.domain.model.PlaceSearchPage.Category;
import com.sopt.nearby.place.port.out.PlaceTextSearchPort;
import com.sopt.nearby.place.port.out.PlaceCacheRepository;
import com.sopt.nearby.user.domain.model.UserAccount;
import com.sopt.nearby.user.domain.model.UserAccountStatus;
import com.sopt.nearby.user.domain.model.UserOnboardingStatus;
import com.sopt.nearby.user.domain.model.UserRole;
import com.sopt.nearby.user.port.out.UserAccountRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:search217;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate", "management.health.redis.enabled=false"
})
@ActiveProfiles("local")
@AutoConfigureMockMvc
class PlaceSearchFlowTest {
    private static final String PATH = "/api/companion-places/search";
    @Autowired private MockMvc mvc;
    @Autowired private UserAccountRepository users;
    @Autowired private CompanionProfileRepository profiles;
    @Autowired private PlaceCacheRepository places;
    @Autowired private DataSource dataSource;
    @MockitoBean private PlaceTextSearchPort searchPort;

    @Test
    void normalizesAndForwardsSearchConditionsAndPagination() throws Exception {
        long userId = profile(CompanionProfileStatus.ACTIVE);
        when(searchPort.search(any())).thenReturn(new PlaceSearchPage(List.of(), "next"));
        mvc.perform(get(PATH).with(jwt().jwt(t -> t.subject("" + userId)))
                        .param("query", "  시우다드   콘달  ").param("latitude", "41.3874").param("longitude", "2.1686")
                        .param("radiusMeters", "5000").param("languageCode", " ES ")
                        .param("pageSize", "5").param("pageToken", "previous"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("PLACES_SEARCHED"))
                .andExpect(jsonPath("$.data.places").isEmpty())
                .andExpect(jsonPath("$.data.nextPageToken").value("next"));
        verify(searchPort).search(new SearchPlacesCommand("시우다드 콘달",
                new BigDecimal("41.3874"), new BigDecimal("2.1686"), 5000, "es", 5, "previous"));
    }

    @Test
    void preservesSelectedPlaceThroughRepeatedPostCreationAndDetailRead() throws Exception {
        long userId = profile(CompanionProfileStatus.ACTIVE);
        String googleId = "selected-place-" + userId;
        var place = new PlaceSearchPage.Place(googleId, "시우다드 콘달", "Barcelona, Spain",
                new BigDecimal("41.38901"), new BigDecimal("2.16502"), Category.RESTAURANT, List.of());
        when(searchPort.search(any())).thenReturn(new PlaceSearchPage(List.of(place), null));
        var search = mvc.perform(request(userId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.places[0].googlePlaceId").value(googleId))
                .andExpect(jsonPath("$.data.places[0].category").value("RESTAURANT")).andReturn();
        assertThat(places.findByGooglePlaceId(googleId)).isEmpty();
        Map<String, Object> selected = JsonPath.read(search.getResponse().getContentAsString(StandardCharsets.UTF_8),
                "$.data.places[0]");
        selected.remove("attributions");
        String placeJson = com.jayway.jsonpath.Configuration.defaultConfiguration().jsonProvider().toJson(selected);
        Long savedPlaceId = null;
        for (int i = 0; i < 2; i++) {
            var created = mvc.perform(post("/api/companion-posts").with(jwt().jwt(t -> t.subject("" + userId)))
                            .contentType(MediaType.APPLICATION_JSON).content("""
                                    {"place":%s,"meetingTimeType":"UNDECIDED","recruitmentCapacity":1,
                                     "content":"같이 식사해요","openChatUrl":"https://open.kakao.com/o/test"}
                                    """.formatted(placeJson)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.place.googlePlaceId").value(googleId)).andReturn();
            Number postId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.postId");
            Number placeId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.place.placeId");
            if (savedPlaceId != null) {
                assertThat(placeId.longValue()).isEqualTo(savedPlaceId);
            }
            savedPlaceId = placeId.longValue();
            mvc.perform(get("/api/companion-posts/" + postId).with(jwt().jwt(t -> t.subject("" + userId))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.place.googlePlaceId").value(googleId))
                    .andExpect(jsonPath("$.data.place.name").value(place.name()))
                    .andExpect(jsonPath("$.data.place.address").value(place.address()))
                    .andExpect(jsonPath("$.data.place.latitude").value(41.38901))
                    .andExpect(jsonPath("$.data.place.longitude").value(2.16502))
                    .andExpect(jsonPath("$.data.place.category").value("RESTAURANT"));
        }
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(
                "select count(*) from place_cache where google_place_id = ?")) {
            statement.setString(1, googleId);
            try (var result = statement.executeQuery()) {
                result.next();
                assertThat(result.getInt(1)).isEqualTo(1);
            }
        }
    }

    @Test
    void rejectsUnauthenticatedIncompleteSkippedMissingAndInactiveProfilesBeforeExternalCalls() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        long incomplete = user(UserOnboardingStatus.STARTED);
        long skipped = user(UserOnboardingStatus.COMPANION_PROFILE_SKIPPED);
        long missing = user(UserOnboardingStatus.COMPLETED);
        long inactive = profile(CompanionProfileStatus.INACTIVE);
        for (long id : new long[]{incomplete, skipped, missing, inactive}) {
            mvc.perform(request(id)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code")
                    .value(id == incomplete ? "ONBOARDING_REQUIRED" : "COMPANION_PROFILE_REQUIRED"));
        }
        mvc.perform(request(Long.MAX_VALUE)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
        verifyNoInteractions(searchPort);
    }

    @Test
    void rejectsBadSearchParametersBeforeExternalCalls() throws Exception {
        long id = profile(CompanionProfileStatus.ACTIVE);
        for (var entry : Map.of("query", " ", "latitude", "NaN", "longitude", "181",
                "radiusMeters", "50001", "languageCode", "xx", "pageSize", "21").entrySet()) {
            Map<String, String> params = new HashMap<>(Map.of("query", "시우다드 콘달", "latitude", "41.3874", "longitude", "2.1686"));
            params.put(entry.getKey(), entry.getValue());
            var request = get(PATH).with(jwt().jwt(t -> t.subject("" + id)));
            params.forEach(request::param);
            mvc.perform(request)
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_PLACE_SEARCH_REQUEST"));
        }
        mvc.perform(get(PATH).with(jwt().jwt(t -> t.subject("" + id))))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(searchPort);
    }

    @Test
    void distinguishesEmptyResultsFromFailureQuotaAndTimeout() throws Exception {
        long id = profile(CompanionProfileStatus.ACTIVE);
        when(searchPort.search(any())).thenReturn(new PlaceSearchPage(List.of(), null));
        mvc.perform(request(id)).andExpect(status().isOk()).andExpect(jsonPath("$.data.places").isEmpty());
        for (var entry : Map.of(502, new PlaceSearchFailedException(),
                503, new PlaceSearchRateLimitedException(), 504, new PlaceSearchTimeoutException()).entrySet()) {
            reset(searchPort);
            when(searchPort.search(any())).thenThrow(entry.getValue());
            mvc.perform(request(id)).andExpect(status().is(entry.getKey()))
                    .andExpect(jsonPath("$.code").value(entry.getValue().getErrorCode().name()));
        }
    }

    @Test
    void documentsRequestSuccessAndErrorContracts() throws Exception {
        var docs = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        for (String code : List.of("200", "400", "401", "403", "404", "502", "503", "504")) {
            jsonPath("$.paths['" + PATH + "'].get.responses['" + code + "']").exists().match(docs);
        }
        jsonPath("$.paths['" + PATH + "'].get.responses['403'].content['application/json'].examples.COMPANION_PROFILE_REQUIRED.value.code")
                .value("COMPANION_PROFILE_REQUIRED").match(docs);
    }

    private MockHttpServletRequestBuilder request(final long id) {
        return get(PATH).with(jwt().jwt(t -> t.subject("" + id))).param("query", "시우다드 콘달")
                .param("latitude", "41.3874").param("longitude", "2.1686");
    }

    private long profile(final CompanionProfileStatus status) {
        long id = user(UserOnboardingStatus.COMPLETED);
        profiles.save(new CompanionProfile(null, id, "검색" + id, UserGender.FEMALE, 1998,
                null, null, new BigDecimal("4.50"), 0, status));
        return id;
    }

    private long user(final UserOnboardingStatus status) {
        LocalDateTime now = LocalDateTime.of(2026, 10, 3, 0, 0);
        return users.save(new UserAccount(null, UserRole.USER, UserAccountStatus.ACTIVE,
                "01012345678", now, status, now, null)).id();
    }
}
