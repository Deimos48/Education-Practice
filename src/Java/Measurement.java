import java.time.LocalDateTime;

final class Measurement extends BatchEvent implements ClimateReading {
    private final double temperature;
    private final double humidity;

    Measurement(String id, String batchId, LocalDateTime recordedAt, double temperature, double humidity) {
        super(id, batchId, recordedAt);
        if (!Double.isFinite(temperature) || !Double.isFinite(humidity) || humidity < 0 || humidity > 100) {
            throw new IllegalArgumentException("Недопустимые показания замера " + id);
        }
        this.temperature = temperature;
        this.humidity = humidity;
    }

    @Override public double temperature() { return temperature; }
    @Override public double humidity() { return humidity; }

    @Override public String summary() {
        return temperature + " °C, " + humidity + " %";
    }
}
