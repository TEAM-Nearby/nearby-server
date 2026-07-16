// 동행 신청자의 처리 결과 조회 규칙을 구현하는 서비스
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.exception.CompanionMatchNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionRequestNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionRequestResultNotReadableException;
import com.sopt.nearby.companion.domain.exception.CompanionRequestResultNotReadyException;
import com.sopt.nearby.companion.domain.model.match.AcceptedCompanionRequestDetail;
import com.sopt.nearby.companion.domain.model.match.CompanionApplication;
import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.port.in.ReadCompanionRequestResultUseCase;
import com.sopt.nearby.companion.port.out.AcceptedCompanionRequestDetailQueryPort;
import com.sopt.nearby.companion.port.out.CompanionApplicationRepository;
import org.springframework.transaction.annotation.Transactional;

public class ReadCompanionRequestResultService implements ReadCompanionRequestResultUseCase {

    private final CompanionApplicationRepository applicationRepository;
    private final AcceptedCompanionRequestDetailQueryPort detailQueryPort;

    public ReadCompanionRequestResultService(
            final CompanionApplicationRepository applicationRepository,
            final AcceptedCompanionRequestDetailQueryPort detailQueryPort
    ) {
        this.applicationRepository = applicationRepository;
        this.detailQueryPort = detailQueryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public CompanionRequestResult read(final ReadCompanionRequestResultCommand command) {
        validate(command);

        CompanionApplication application = applicationRepository.findById(command.applicationId())
                .filter(found -> found.applicantUserId().equals(command.requesterUserId()))
                .orElseThrow(CompanionRequestNotFoundException::new);

        if (application.status() == CompanionApplicationStatus.REJECTED) {
            return new CompanionRequestResult(application.id(), application.status(), null);
        }
        if (application.status() != CompanionApplicationStatus.ACCEPTED) {
            throw new CompanionRequestResultNotReadyException();
        }

        AcceptedCompanionRequestDetail detail = detailQueryPort.findByApplicationIdAndRequesterUserId(
                        application.id(),
                        command.requesterUserId()
                )
                .orElseThrow(CompanionMatchNotFoundException::new);
        validateMatchStatus(detail.matchStatus());

        return new CompanionRequestResult(application.id(), application.status(), detail);
    }

    private void validate(final ReadCompanionRequestResultCommand command) {
        if (command == null
                || command.requesterUserId() == null
                || command.requesterUserId() <= 0
                || command.applicationId() == null
                || command.applicationId() <= 0) {
            throw new CompanionRequestNotFoundException();
        }
    }

    private void validateMatchStatus(final CompanionMatchStatus status) {
        if (status == CompanionMatchStatus.CANCELED) {
            throw new CompanionRequestResultNotReadableException();
        }
    }
}
