import java.time.LocalDate;
import java.util.List;

public class WorklistDemo {
    public static void main(String[] args) {
        LocalDate date = LocalDate.parse(args.length == 0 ? "2026-11-01" : args[0]);
        WorkSchedule schedule = new WorkSchedule(List.of(
            new BatchOperation("W1", "B1", "T1", LocalDate.parse("2026-10-31"), null),
            new BatchOperation("W2", "B1", "T2", LocalDate.parse("2026-11-01"), null),
            new BatchOperation("W3", "B1", "T3", LocalDate.parse("2026-11-02"), null),
            new BatchOperation("W4", "B1", "T1", LocalDate.parse("2026-10-30"), LocalDate.parse("2026-10-30")),
            new BatchOperation("W5", "B2", "T1", LocalDate.parse("2026-11-02"), null)
        ));
        for (WorkSchedule.Entry entry : schedule.buildWorklist(date)) {
            System.out.println(entry.operationId() + " — " + entry.status());
        }
    }
}
