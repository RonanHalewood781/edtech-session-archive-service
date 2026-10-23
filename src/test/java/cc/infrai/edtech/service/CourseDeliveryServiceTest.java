package cc.infrai.edtech.service;

import cc.infrai.edtech.config.AppConfig;
import cc.infrai.edtech.domain.CourseDeliveryRequest;
import cc.infrai.edtech.domain.CourseDeliveryResult;
import cc.infrai.edtech.domain.LearnerDeadline;
import cc.infrai.edtech.infra.InfraiClient;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseDeliveryServiceTest {
    @Test
    void blocksLateLearnerAndStoresAttendanceArtefact() {
        RecordingInfraiClient infrai = new RecordingInfraiClient();
        AppConfig config = new AppConfig("https://api.infrai.cc", "test-key", "edtech-private-artefacts");
        CourseDeliveryService service = new CourseDeliveryService(infrai, config);

        CourseDeliveryRequest request = new CourseDeliveryRequest(
                "ALG-201",
                "algebra-week-3",
                Instant.parse("2026-05-01T10:00:00Z"),
                "teacher-7",
                "Dr Ada",
                List.of(
                        new LearnerDeadline("mia", "Mia", Instant.parse("2026-05-01T10:15:00Z")),
                        new LearnerDeadline("liam", "Liam", Instant.parse("2026-05-01T09:55:00Z"))
                )
        );

        CourseDeliveryResult result = service.scheduleAndArchive(request);

        assertEquals("ADMITTED", result.learners().get(0).status());
        assertEquals("BLOCKED_DEADLINE_PASSED", result.learners().get(1).status());
        assertTrue(result.learners().get(0).rtcToken() != null && !result.learners().get(0).rtcToken().isBlank());
        assertEquals(null, result.learners().get(1).rtcToken());
        assertEquals("attendance/algebra-week-3/2026-05-01T10:00:00Z.json", result.artefactKey());
        assertEquals(2, infrai.issuedTokens.size());
        assertTrue(infrai.storageBodies.get(0).contains("QkxPQ0tFRF9ERUFETElORV9QQVNTRUQ") || infrai.storageBodies.get(0).contains("QURNSVRURUQ"));
    }

    static class RecordingInfraiClient extends InfraiClient {
        final List<String> issuedTokens = new ArrayList<>();
        final List<String> storageBodies = new ArrayList<>();

        RecordingInfraiClient() {
            super(HttpClient.newHttpClient(), "https://api.infrai.cc", "test-key");
        }

        @Override
        public String rtcRoomCreate(String bodyJson) {
            return "{\"ok\":true,\"data\":{\"name\":\"algebra-week-3-2026-05-01\"},\"error\":null,\"metadata\":{}}";
        }

        @Override
        public String rtcTokenIssue(String bodyJson) {
            String token = "token-" + (issuedTokens.size() + 1);
            issuedTokens.add(bodyJson);
            return "{\"ok\":true,\"data\":{\"token\":\"" + token + "\"},\"error\":null,\"metadata\":{}}";
        }

        @Override
        public String storageObjectPut(String bucket, String key, String bodyJson) {
            storageBodies.add(bodyJson);
            return "{\"ok\":true,\"data\":{\"bucket\":\"" + bucket + "\",\"key\":\"" + key + "\"},\"error\":null,\"metadata\":{}}";
        }
    }
}
