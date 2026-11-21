import java.time.LocalDateTime;

abstract class BatchEvent {
    private final String id;
    private final String batchId;
    private final LocalDateTime recordedAt;

    BatchEvent(String id, String batchId, LocalDateTime recordedAt) {
        this.id = id;
        this.batchId = batchId;
        this.recordedAt = recordedAt;
    }

    public abstract String summary();
}
