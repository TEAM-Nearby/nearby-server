// 동행 푸시 수신 대상을 멱등하게 등록·갱신하는 서비스다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.exception.InvalidCompanionPushEndpointException;
import com.sopt.nearby.companion.domain.model.notification.CompanionPushEndpoint;
import com.sopt.nearby.companion.port.in.RegisterCompanionPushEndpointUseCase;
import com.sopt.nearby.companion.port.out.CompanionPushEndpointRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;

public class RegisterCompanionPushEndpointService implements RegisterCompanionPushEndpointUseCase {

    private static final int MAX_INSTALLATION_ID_LENGTH = 200;
    private static final int MAX_TOKEN_LENGTH = 4096;

    private final CompanionPushEndpointRepository repository;
    private final Clock clock;

    public RegisterCompanionPushEndpointService(
            final CompanionPushEndpointRepository repository,
            final Clock clock
    ) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public RegisterCompanionPushEndpointResult register(final RegisterCompanionPushEndpointCommand command) {
        validate(command);
        LocalDateTime now = LocalDateTime.now(clock);
        repository.deactivateByTokenExceptUser(command.token(), command.userId(), now);
        CompanionPushEndpoint endpoint = repository.findByUserIdAndInstallationId(
                        command.userId(),
                        command.installationId()
                )
                .map(existing -> existing.activate(command.token(), command.platform(), now))
                .orElseGet(() -> new CompanionPushEndpoint(
                        null,
                        command.userId(),
                        command.installationId(),
                        command.token(),
                        command.platform(),
                        true,
                        1L,
                        now,
                        now,
                        now
                ));

        return RegisterCompanionPushEndpointResult.from(repository.save(endpoint));
    }

    private void validate(final RegisterCompanionPushEndpointCommand command) {
        if (command == null || command.userId() == null || command.userId() <= 0
                || blankOrTooLong(command.installationId(), MAX_INSTALLATION_ID_LENGTH)
                || blankOrTooLong(command.token(), MAX_TOKEN_LENGTH)
                || command.platform() == null) {
            throw new InvalidCompanionPushEndpointException();
        }
    }

    private boolean blankOrTooLong(final String value, final int maxLength) {
        return value == null || value.isBlank() || value.length() > maxLength;
    }
}
