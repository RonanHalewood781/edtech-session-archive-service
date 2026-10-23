package cc.infrai.edtech.service;

import cc.infrai.edtech.config.AppConfig;
import cc.infrai.edtech.domain.CourseDeliveryRequest;
import cc.infrai.edtech.domain.CourseDeliveryResult;
import cc.infrai.edtech.domain.LearnerAdmission;
import cc.infrai.edtech.domain.LearnerDeadline;
import cc.infrai.edtech.infra.Envelope;
import cc.infrai.edtech.infra.InfraiClient;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class CourseDeliveryService {
    private final InfraiClient infrai;
    private final AppConfig config;

    public CourseDeliveryService(InfraiClient infrai, AppConfig config) {
        this.infrai = infrai;
        this.config = config;
    }

    public CourseDeliveryResult scheduleAndArchive(CourseDeliveryRequest request) {
        String roomName = request.lessonSlug() + "-" + request.lessonStart().toString().substring(0, 10);
        String roomCreateBody = "{" +
                "\"name\":\"" + Json.escape(roomName) + "\"," +
                "\"max_participants\":" + (request.learners().size() + 1) + "," +
                "\"empty_timeout_s\":600," +
                "\"region\":\"global\"" +
                "}";
        infrai.rtcRoomCreate(roomCreateBody);

        String educatorToken = extractToken(infrai.rtcTokenIssue(tokenIssueBody(roomName, request.educatorId(), request.educatorName(), true, true)));

        List<LearnerAdmission> admissions = new ArrayList<>();
        for (LearnerDeadline learner : request.learners()) {
            if (learner.assignmentDeadline().isBefore(request.lessonStart())) {
                admissions.add(new LearnerAdmission(learner.learnerId(), learner.learnerName(), "BLOCKED_DEADLINE_PASSED", null));
            } else {
                String token = extractToken(infrai.rtcTokenIssue(tokenIssueBody(roomName, learner.learnerId(), learner.learnerName(), true, true)));
                admissions.add(new LearnerAdmission(learner.learnerId(), learner.learnerName(), "ADMITTED", token));
            }
        }

        String artefactKey = "attendance/" + request.lessonSlug() + "/" + request.lessonStart().toString() + ".json";
        String attendanceJson = buildAttendanceArtefact(request, roomName, admissions, Instant.now());
        String storageBody = "{" +
                "\"content_type\":\"application/json\"," +
                "\"data_base64\":\"" + Base64.getEncoder().encodeToString(attendanceJson.getBytes(StandardCharsets.UTF_8)) + "\"" +
                "}";
        infrai.storageObjectPut(config.storageBucket(), artefactKey, storageBody);

        return new CourseDeliveryResult(roomName, educatorToken, admissions, config.storageBucket(), artefactKey);
    }

    private String tokenIssueBody(String roomName, String identity, String displayName, boolean canPublish, boolean canSubscribe) {
        return "{" +
                "\"room\":\"" + Json.escape(roomName) + "\"," +
                "\"identity\":\"" + Json.escape(identity) + "\"," +
                "\"display_name\":\"" + Json.escape(displayName) + "\"," +
                "\"ttl_s\":3600," +
                "\"can_publish\":" + canPublish + "," +
                "\"can_subscribe\":" + canSubscribe +
                "}";
    }

    private String extractToken(String responseJson) {
        return Envelope.extractString(responseJson, "token");
    }

    private String buildAttendanceArtefact(CourseDeliveryRequest request, String roomName, List<LearnerAdmission> admissions, Instant createdAt) {
        StringBuilder builder = new StringBuilder();
        builder.append("{")
                .append("\"course_code\":\"").append(Json.escape(request.courseCode())).append("\",")
                .append("\"lesson_slug\":\"").append(Json.escape(request.lessonSlug())).append("\",")
                .append("\"lesson_start\":\"").append(request.lessonStart()).append("\",")
                .append("\"room_name\":\"").append(Json.escape(roomName)).append("\",")
                .append("\"educator_id\":\"").append(Json.escape(request.educatorId())).append("\",")
                .append("\"created_at\":\"").append(createdAt).append("\",")
                .append("\"learners\":[");
        for (int i = 0; i < admissions.size(); i++) {
            LearnerAdmission admission = admissions.get(i);
            if (i > 0) {
                builder.append(',');
            }
            builder.append("{")
                    .append("\"learner_id\":\"").append(Json.escape(admission.learnerId())).append("\",")
                    .append("\"learner_name\":\"").append(Json.escape(admission.learnerName())).append("\",")
                    .append("\"status\":\"").append(admission.status()).append("\"")
                    .append("}");
        }
        builder.append("]}");
        return builder.toString();
    }
}
