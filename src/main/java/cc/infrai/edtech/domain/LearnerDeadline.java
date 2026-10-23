package cc.infrai.edtech.domain;

import java.time.Instant;

public record LearnerDeadline(String learnerId, String learnerName, Instant assignmentDeadline) {
}
