// 실제 Firebase SDK로 이벤트 메시지를 구성하고 HTTP 요청과 응답 변환을 검증한다.
package com.sopt.nearby.companion.adapter.out.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sopt.nearby.companion.port.out.PushDeliveryResult;
import com.sopt.nearby.companion.port.out.PushMessage;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class FirebasePushSenderTest {

    private FirebaseApp app;

    @AfterEach
    void deleteFirebaseApp() {
        if (app != null) {
            app.delete();
        }
    }

    @Test
    void sendsEventNotificationWithRequiredApnsPayload() throws Exception {
        MockHttpTransport transport = transport(200,
                "{\"name\":\"projects/test-project/messages/test-message\"}");
        FirebasePushSender sender = sender(transport);
        PushMessage message = message();

        List<PushDeliveryResult> results = sender.send(List.of(message));

        assertEquals(List.of(new PushDeliveryResult(
                31L, PushDeliveryResult.Outcome.SENT,
                "projects/test-project/messages/test-message", null, null
        )), results);
        assertEquals("https://fcm.googleapis.com/v1/projects/test-project/messages:send",
                transport.getLowLevelHttpRequest().getUrl());
        JsonObject payload = JsonParser.parseString(
                transport.getLowLevelHttpRequest().getContentAsString()
        ).getAsJsonObject().getAsJsonObject("message");
        assertEquals(message.token(), payload.get("token").getAsString());
        assertEquals(message.title(), payload.getAsJsonObject("notification").get("title").getAsString());
        assertEquals(message.body(), payload.getAsJsonObject("notification").get("body").getAsString());
        assertEquals(JsonParser.parseString("""
                {"notificationId":"138","targetType":"COMPANION_APPLICATION","targetId":"80"}
                """), payload.getAsJsonObject("data"));
        JsonObject apns = payload.getAsJsonObject("apns");
        assertEquals(new JsonObject(), apns.getAsJsonObject("payload").getAsJsonObject("aps"));
        assertEquals(String.valueOf(message.expiresAt().toEpochSecond(ZoneOffset.UTC)),
                apns.getAsJsonObject("headers").get("apns-expiration").getAsString());
        String ttl = payload.getAsJsonObject("android").get("ttl").getAsString();
        assertTrue(ttl.endsWith("s"));
        double ttlSeconds = Double.parseDouble(ttl.substring(0, ttl.length() - 1));
        assertTrue(ttlSeconds > 0 && ttlSeconds <= 300);
    }

    @Test
    void returnsFcmErrorAfterMessageConstructionSucceeds() {
        FirebasePushSender sender = sender(transport(404, """
                {"error":{"code":404,"message":"Requested entity was not found.","status":"NOT_FOUND",
                "details":[{"@type":"type.googleapis.com/google.firebase.fcm.v1.FcmError",
                "errorCode":"UNREGISTERED"}]}}
                """));

        List<PushDeliveryResult> results = sender.send(List.of(message()));

        assertEquals(List.of(new PushDeliveryResult(
                31L, PushDeliveryResult.Outcome.PERMANENT_FAILURE, null, "UNREGISTERED", null
        )), results);
    }

    private FirebasePushSender sender(final MockHttpTransport transport) {
        app = FirebaseApp.initializeApp(FirebaseOptions.builder()
                .setProjectId("test-project")
                .setCredentials(GoogleCredentials.create(new AccessToken(
                        "test-access-token", Date.from(Instant.now().plusSeconds(3600))
                )))
                .setHttpTransport(transport)
                .build());
        return new FirebasePushSender("test-project", 5000, 10000, 10000);
    }

    private MockHttpTransport transport(final int status, final String body) {
        return new MockHttpTransport.Builder()
                .setLowLevelHttpResponse(new MockLowLevelHttpResponse()
                        .setStatusCode(status)
                        .setContentType("application/json; charset=UTF-8")
                        .setContent(body))
                .build();
    }

    private PushMessage message() {
        return new PushMessage(
                31L, "test-device-token", "새로운 동행 신청", "새로운 동행 신청이 도착했어요.",
                Map.of("notificationId", "138", "targetType", "COMPANION_APPLICATION", "targetId", "80"),
                LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5)
        );
    }
}
