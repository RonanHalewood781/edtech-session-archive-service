package cc.infrai.edtech;

import cc.infrai.edtech.config.AppConfig;
import cc.infrai.edtech.domain.CourseDeliveryRequest;
import cc.infrai.edtech.domain.CourseDeliveryResult;
import cc.infrai.edtech.domain.LearnerAdmission;
import cc.infrai.edtech.domain.LearnerDeadline;
import cc.infrai.edtech.infra.InfraiClient;
import cc.infrai.edtech.service.CourseDeliveryService;

import java.time.Instant;
import java.util.List;

public class CourseDeliveryCli {
    public static void main(String[] args) {
        AppConfig config = AppConfig.fromEnv();
        InfraiClient infrai = new InfraiClient(config);
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
        System.out.println("room=" + result.roomName());
        System.out.println("educatorTokenIssued=" + (result.educatorRtcToken() != null && !result.educatorRtcToken().isBlank()));
        System.out.println("artefactKey=" + result.artefactKey());
        for (LearnerAdmission learner : result.learners()) {
            System.out.println("learner=" + learner.learnerId() + " status=" + learner.status() + " tokenIssued=" + (learner.rtcToken() != null));
        }
    }
}
