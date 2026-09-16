// Firebase Cloud Messaging으로 동행 푸시를 발송하는 어댑터다.
package com.sopt.nearby.companion.adapter.out.notification;

import com.google.api.client.http.HttpResponseException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.ApnsConfig;
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
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("fcm")
public class FirebasePushSender implements PushSender {

    private final FirebaseMessaging messaging;

    public FirebasePushSender(
            @Value("${nearby.push.firebase.project-id:}") final String projectId,
            @Value("${nearby.push.firebase.connect-timeout-ms:5000}") final int connectTimeoutMs,
            @Value("${nearby.push.firebase.read-timeout-ms:10000}") final int readTimeoutMs,
            @Value("${nearby.push.firebase.write-timeout-ms:10000}") final int writeTimeoutMs
    ) {
        this.messaging = FirebaseMessaging.getInstance(firebaseApp(
                projectId,
                connectTimeoutMs,
                readTimeoutMs,
                writeTimeoutMs
        ));
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
                results.add(toResult(messages.get(index), response.getResponses().get(index)));
            }
            return results;
        } catch (FirebaseMessagingException exception) {
            return messages.stream()
                    .map(message -> new PushDeliveryResult(
                            message.deliveryId(),
                            PushDeliveryResult.Outcome.RETRYABLE_FAILURE,
                            null,
                            errorCode(exception),
                            retryAfterSeconds(exception)
                    ))
                    .toList();
        }
    }

    private Message toFirebaseMessage(final PushMessage message) {
        long ttlSeconds = Math.max(0L, Math.min(
                Duration.between(Instant.now(), message.expiresAt().toInstant(ZoneOffset.UTC)).getSeconds(),
                2_419_200L
        ));
        return Message.builder()
                .setToken(message.token())
                .setNotification(Notification.builder()
                        .setTitle(message.title())
                        .setBody(message.body())
                        .build())
                .setAndroidConfig(AndroidConfig.builder().setTtl(ttlSeconds * 1_000L).build())
                .setApnsConfig(ApnsConfig.builder()
                        .putHeader("apns-expiration", String.valueOf(message.expiresAt().toEpochSecond(ZoneOffset.UTC)))
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
                    null,
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
                errorCode,
                retryAfterSeconds(exception)
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
                || "SENDER_ID_MISMATCH".equals(errorCode)
                || "THIRD_PARTY_AUTH_ERROR".equals(errorCode);
    }

    private FirebaseApp firebaseApp(
            final String projectId,
            final int connectTimeoutMs,
            final int readTimeoutMs,
            final int writeTimeoutMs
    ) {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }
        try {
            FirebaseOptions.Builder builder = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .setConnectTimeout(Math.max(1, connectTimeoutMs))
                    .setReadTimeout(Math.max(1, readTimeoutMs))
                    .setWriteTimeout(Math.max(1, writeTimeoutMs));
            if (projectId != null && !projectId.isBlank()) {
                builder.setProjectId(projectId);
            }
            return FirebaseApp.initializeApp(builder.build());
        } catch (IOException exception) {
            throw new IllegalStateException("FCM 인증 정보를 초기화할 수 없습니다.", exception);
        }
    }

    private Long retryAfterSeconds(final FirebaseMessagingException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof HttpResponseException httpException) {
                return parseRetryAfter(httpException.getHeaders().getRetryAfter());
            }
            cause = cause.getCause();
        }
        return null;
    }

    private Long parseRetryAfter(final String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        try {
            return Math.max(0L, Long.parseLong(header.trim()));
        } catch (NumberFormatException ignored) {
            try {
                return Math.max(0L, Duration.between(
                        Instant.now(),
                        ZonedDateTime.parse(header, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()
                ).getSeconds());
            } catch (RuntimeException ignoredDate) {
                return null;
            }
        }
    }
}
