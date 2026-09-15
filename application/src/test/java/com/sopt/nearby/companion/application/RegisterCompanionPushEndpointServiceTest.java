// 동행 푸시 수신 대상 등록·갱신 규칙을 검증한다.
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sopt.nearby.companion.domain.model.notification.CompanionPushEndpoint;
import com.sopt.nearby.companion.domain.model.notification.CompanionPushPlatform;
import com.sopt.nearby.companion.port.out.CompanionPushEndpointRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RegisterCompanionPushEndpointServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-15T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void createsActiveEndpoint() {
        FakeRepository repository = new FakeRepository();
        RegisterCompanionPushEndpointService service = new RegisterCompanionPushEndpointService(repository, CLOCK);

        RegisterCompanionPushEndpointResult result = service.register(new RegisterCompanionPushEndpointCommand(
                7L,
                "installation-1",
                "token-1",
                CompanionPushPlatform.ANDROID
        ));

        assertEquals(1L, result.endpointId());
        assertEquals("installation-1", result.installationId());
        assertTrue(result.active());
        assertEquals(1, repository.endpoints.size());
    }

    @Test
    void reactivatesAndUpdatesExistingEndpoint() {
        FakeRepository repository = new FakeRepository();
        repository.endpoint = new CompanionPushEndpoint(
                11L,
                7L,
                "installation-1",
                "old-token",
                CompanionPushPlatform.ANDROID,
                false,
                LocalDateTime.MIN,
                LocalDateTime.MIN,
                LocalDateTime.MIN
        );
        RegisterCompanionPushEndpointService service = new RegisterCompanionPushEndpointService(repository, CLOCK);

        RegisterCompanionPushEndpointResult result = service.register(new RegisterCompanionPushEndpointCommand(
                7L,
                "installation-1",
                "new-token",
                CompanionPushPlatform.IOS
        ));

        assertEquals(11L, result.endpointId());
        assertEquals(CompanionPushPlatform.IOS, result.platform());
        assertTrue(result.active());
        assertEquals("new-token", repository.endpoint.token());
    }

    private static final class FakeRepository implements CompanionPushEndpointRepository {

        private final List<CompanionPushEndpoint> endpoints = new ArrayList<>();
        private CompanionPushEndpoint endpoint;

        @Override
        public Optional<CompanionPushEndpoint> findByUserIdAndInstallationId(Long userId, String installationId) {
            return Optional.ofNullable(endpoint).filter(found -> found.userId().equals(userId)
                    && found.installationId().equals(installationId));
        }

        @Override
        public List<CompanionPushEndpoint> findActiveByUserId(Long userId) {
            return endpoints.stream().filter(found -> found.userId().equals(userId) && found.active()).toList();
        }

        @Override
        public int deactivateByTokenExceptUser(String token, Long userId, LocalDateTime now) { return 0; }

        @Override
        public int deactivateById(Long endpointId, LocalDateTime now) { return 0; }

        @Override
        public CompanionPushEndpoint save(CompanionPushEndpoint endpoint) {
            if (endpoint.id() == null) {
                endpoint = new CompanionPushEndpoint(
                        1L,
                        endpoint.userId(),
                        endpoint.installationId(),
                        endpoint.token(),
                        endpoint.platform(),
                        endpoint.active(),
                        endpoint.lastSeenAt(),
                        endpoint.createdAt(),
                        endpoint.updatedAt()
                );
            }
            CompanionPushEndpoint saved = endpoint;
            this.endpoint = saved;
            endpoints.removeIf(found -> found.id().equals(saved.id()));
            endpoints.add(saved);
            return saved;
        }
    }
}
