import com.google.gson.JsonParseException;
import java.io.IOException;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.*;

public class WorklistDemo {
    public static void main(String[] args) {
        try {
            boolean worklistOnly = args.length == 1 && !args[0].startsWith("--");
            Map<String,String> options = new HashMap<>();
            if (worklistOnly) {
                options.put("--date", args[0]);
            } else {
                for (int i = 0; i < args.length; i += 2) {
                    if (!Set.of("--data", "--batch", "--date", "--save").contains(args[i])
                            || i + 1 == args.length || args[i+1].startsWith("--")) {
                        throw new IllegalArgumentException("Параметры: --data файл --batch файл --date YYYY-MM-DD --save файл");
                    }
                    options.put(args[i], args[i+1]);
                }
            }
            LocalDate date = LocalDate.parse(options.getOrDefault("--date", "2026-11-01"));
            PracticeData data = new PracticeData(Path.of(options.getOrDefault("--data", "demo.json")));
            var worklist = data.schedule().buildWorklist(date);
            if (worklistOnly) {
                for (var entry : worklist) System.out.println(entry.operationId() + " — " + entry.status());
                return;
            }
            String id = data.register(Path.of(options.getOrDefault("--batch", "new_batch.json")));
            System.out.println("Зарегистрирована партия: " + id);
            System.out.println("\nРаботы на " + date);
            for (var entry : worklist) System.out.println(entry.operationId() + ": " + entry.status());
            if (worklist.isEmpty()) System.out.println("Наступивших и просроченных работ нет.");
            System.out.println("\nПодходящие камеры для ожидающих партий");
            data.selectChambers().forEach((batch, ids) ->
                    System.out.println(batch + ": " + (ids.isEmpty() ? "подходящих камер нет" : String.join(", ", ids))));
            if (options.containsKey("--save")) {
                Path file = Path.of(options.get("--save"));
                data.save(file);
                System.out.println("\nДанные сохранены: " + file);
            }
        } catch (IOException | JsonParseException | IllegalArgumentException | DateTimeException
                 | IllegalStateException | ArithmeticException e) {
            System.err.println("Ошибка: " + e.getMessage());
            System.exit(1);
        }
    }
}

