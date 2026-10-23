package cc.infrai.edtech.domain;

import java.util.List;

public record CourseDeliveryResult(
        String roomName,
        String educatorRtcToken,
        List<LearnerAdmission> learners,
        String artefactBucket,
        String artefactKey
) {
}
