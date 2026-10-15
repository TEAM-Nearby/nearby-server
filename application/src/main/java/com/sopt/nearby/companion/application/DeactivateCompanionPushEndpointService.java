// 동행 푸시 수신 대상을 비활성화하는 서비스다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.exception.InvalidCompanionPushEndpointException;
import com.sopt.nearby.companion.port.in.DeactivateCompanionPushEndpointUseCase;
import com.sopt.nearby.companion.port.out.CompanionPushEndpointRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;

public class DeactivateCompanionPushEndpointService implements DeactivateCompanionPushEndpointUseCase {

    private static final int MAX_INSTALLATION_ID_LENGTH = 200;

    private final CompanionPushEndpointRepository repository;
    private final Clock clock;

    public DeactivateCompanionPushEndpointService(
            final CompanionPushEndpointRepository repository,
            final Clock clock
    ) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void deactivate(final DeactivateCompanionPushEndpointCommand command) {
        if (command == null || command.userId() == null || command.userId() <= 0
                || command.installationId() == null
                || command.installationId().isBlank()
                || command.installationId().length() > MAX_INSTALLATION_ID_LENGTH) {
            throw new InvalidCompanionPushEndpointException();
        }
        repository.findByUserIdAndInstallationId(command.userId(), command.installationId())
                .map(endpoint -> endpoint.deactivate(LocalDateTime.now(clock)))
                .ifPresent(repository::save);
    }
}
