package cc.infrai.edtech.domain;

import java.time.Instant;
import java.util.List;

public record CourseDeliveryRequest(
        String courseCode,
        String lessonSlug,
        Instant lessonStart,
        String educatorId,
        String educatorName,
        List<LearnerDeadline> learners
) {
}
