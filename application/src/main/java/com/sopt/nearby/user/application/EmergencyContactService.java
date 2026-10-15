// 사용자별 비상 연락망을 한 건만 유지하며 저장한다.
package com.sopt.nearby.user.application;

import com.sopt.nearby.user.domain.model.EmergencyContact;
import com.sopt.nearby.user.exception.UserNotFoundException;
import com.sopt.nearby.user.port.in.EmergencyContactUseCase;
import com.sopt.nearby.user.port.out.EmergencyContactRepository;
import com.sopt.nearby.user.port.out.UserAccountRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmergencyContactService implements EmergencyContactUseCase {
    private final UserAccountRepository users;
    private final EmergencyContactRepository contacts;

    public EmergencyContactService(final UserAccountRepository users, final EmergencyContactRepository contacts) {
        this.users = users;
        this.contacts = contacts;
    }

    @Override
    @Transactional
    public EmergencyContact save(final SaveEmergencyContactCommand command) {
        users.findByIdForUpdate(command.userId()).orElseThrow(UserNotFoundException::new);
        Long contactId = contacts.findByUserId(command.userId()).map(EmergencyContact::id).orElse(null);
        return contacts.save(new EmergencyContact(contactId, command.userId(), command.name(), command.phoneNumber()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EmergencyContact> read(final Long userId) {
        users.findById(userId).orElseThrow(UserNotFoundException::new);
        return contacts.findByUserId(userId);
    }
}
