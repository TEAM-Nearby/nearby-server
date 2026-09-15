// Firebase Cloud Messaging으로 동행 푸시를 발송하는 어댑터다.
package com.sopt.nearby.companion.adapter.out.notification;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.sopt.nearby.companion.port.out.PushDeliveryResult;
import com.sopt.nearby.companion.port.out.PushMessage;
import com.sopt.nearby.companion.port.out.PushSender;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("fcm")
public class FirebasePushSender implements PushSender {

    private final FirebaseMessaging messaging;

    public FirebasePushSender(@Value("${nearby.push.firebase.project-id:}") final String projectId) {
        this.messaging = FirebaseMessaging.getInstance(firebaseApp(projectId));
    }

    @Override
    public List<PushDeliveryResult> send(final List<PushMessage> messages) {
        if (messages.isEmpty()) {
            return List.of();
        }
        try {
            BatchResponse response = messaging.sendEach(messages.stream().map(this::toFirebaseMessage).toList());
            List<PushDeliveryResult> results = new ArrayList<>(messages.size());
            for (int index = 0; index < messages.size(); index++) {
                PushMessage message = messages.get(index);
                SendResponse sendResponse = response.getResponses().get(index);
                results.add(toResult(message, sendResponse));
            }
            return results;
        } catch (FirebaseMessagingException exception) {
            return messages.stream()
                    .map(message -> new PushDeliveryResult(
                            message.deliveryId(),
                            PushDeliveryResult.Outcome.RETRYABLE_FAILURE,
                            null,
                            errorCode(exception)
                    ))
                    .toList();
        }
    }

    private Message toFirebaseMessage(final PushMessage message) {
        return Message.builder()
                .setToken(message.token())
                .setNotification(Notification.builder()
                        .setTitle(message.title())
                        .setBody(message.body())
                        .build())
                .putAllData(message.data())
                .build();
    }

    private PushDeliveryResult toResult(final PushMessage message, final SendResponse response) {
        if (response.isSuccessful()) {
            return new PushDeliveryResult(
                    message.deliveryId(),
                    PushDeliveryResult.Outcome.SENT,
                    response.getMessageId(),
                    null
            );
        }
        FirebaseMessagingException exception = response.getException();
        String errorCode = errorCode(exception);
        return new PushDeliveryResult(
                message.deliveryId(),
                isPermanent(errorCode)
                        ? PushDeliveryResult.Outcome.PERMANENT_FAILURE
                        : PushDeliveryResult.Outcome.RETRYABLE_FAILURE,
                null,
                errorCode
        );
    }

    private String errorCode(final FirebaseMessagingException exception) {
        if (exception == null || exception.getMessagingErrorCode() == null) {
            return "FCM_UNKNOWN";
        }
        return exception.getMessagingErrorCode().name();
    }

    private boolean isPermanent(final String errorCode) {
        return "UNREGISTERED".equals(errorCode)
                || "INVALID_ARGUMENT".equals(errorCode)
                || "SENDER_ID_MISMATCH".equals(errorCode);
    }

    private FirebaseApp firebaseApp(final String projectId) {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }
        try {
            FirebaseOptions.Builder builder = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault());
            if (projectId != null && !projectId.isBlank()) {
                builder.setProjectId(projectId);
            }
            return FirebaseApp.initializeApp(builder.build());
        } catch (IOException exception) {
            throw new IllegalStateException("FCM 인증 정보를 초기화할 수 없습니다.", exception);
        }
    }
}
