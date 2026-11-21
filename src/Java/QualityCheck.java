import java.time.LocalDateTime;

final class QualityCheck extends BatchEvent {
    private final String decision;
    private final String comment;

    QualityCheck(String id, String batchId, LocalDateTime recordedAt, String decision, String comment) {
        super(id, batchId, recordedAt);
        this.decision = decision;
        this.comment = comment;
    }

    @Override public String summary() {
        return decision + ": " + comment;
    }
}
