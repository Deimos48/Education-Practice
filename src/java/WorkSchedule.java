import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

final class WorkSchedule {
    record Entry(String operationId, String status) {}

    private final List<BatchOperation> operations;

    WorkSchedule(List<BatchOperation> operations) {
        this.operations = List.copyOf(operations);
    }

    List<Entry> buildWorklist(LocalDate date) {
        List<Entry> result = new ArrayList<>();
        for (BatchOperation work : operations) {
            if (work.isDueOn(date)) {
                String status = work.plannedDate().isBefore(date) ? "просрочено" : "сегодня";
                result.add(new Entry(work.id(), status));
            }
        }
        return result;
    }
}
