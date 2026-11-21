import java.nio.file.*;
import java.time.*;
import java.util.*;

public class PracticeChecks {
    private static void check(boolean passed, String reason) {
        if (!passed) throw new AssertionError(reason);
    }

    public static void main(String[] args) throws Exception {
        PracticeData data = new PracticeData(Path.of("demo.json"));
        LocalDate today = LocalDate.parse("2026-11-01");
        check(data.schedule().buildWorklist(today).equals(List.of(
                new WorkSchedule.Entry("W1", "просрочено"), new WorkSchedule.Entry("W2", "сегодня"))), "Сроки работ");
        check(data.schedule().buildWorklist(today.minusDays(4)).isEmpty(), "Пустое расписание");
        Path card = Path.of("new_batch.json");
        check(data.register(card).equals("B4"), "Регистрация");
        var selected = data.selectChambers();
        check(new ArrayList<>(selected.keySet()).equals(List.of("B2","B3","B4")), "Порядок партий");
        check(selected.equals(Map.of("B2",List.of("C1","C3"), "B3",List.of("C2"), "B4",List.of())), "Подбор камер");
        try {
            data.register(card);
            throw new AssertionError("Повторная регистрация разрешена");
        } catch (IllegalArgumentException expected) {
            check(selected.equals(data.selectChambers()), "Состояние после отказа");
        }
        Batch batch = new Batch("X","Проверка",1,today,today.plusDays(3),16,18,60,65,null,"awaiting");
        ClimateReading low = new Chamber("C","Камера",16,60);
        ClimateReading high = new Measurement("M","X",today.atStartOfDay(),18,65);
        check(batch.accepts(low) && batch.accepts(high), "Границы диапазона и обе реализации интерфейса");
        check(!batch.accepts(new Chamber("C","Камера",18.1,65)), "Выход за диапазон");
        check(data.eventSummaries().size() == 2 && data.eventSummaries().get(0).contains("°C")
                && data.eventSummaries().get(1).startsWith("continue:"), "Сводки двух видов событий");
        Path saved = Files.createTempFile("cheese-check-", ".json");
        try {
            data.save(saved);
            check(new PracticeData(saved).selectChambers().equals(selected), "Повторное чтение результата");
        } finally {
            Files.deleteIfExists(saved);
        }
        System.out.println("Все проверки пройдены.");
    }
}
