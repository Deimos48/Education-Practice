import java.time.LocalDate;

record BatchOperation(String id, String batchId, String operationTypeId,
                      LocalDate plannedDate, LocalDate completedDate) {

    boolean isDueOn(LocalDate date) {
        return completedDate == null && !plannedDate.isAfter(date);
    }
}
