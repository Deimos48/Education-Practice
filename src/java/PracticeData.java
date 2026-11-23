import com.google.gson.*;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.function.Function;

final class PracticeData {
    private static final Gson JSON = new GsonBuilder().setStrictness(Strictness.STRICT)
            .serializeNulls().setPrettyPrinting().disableHtmlEscaping().create();
    private final JsonObject document;
    private final BatchRegistry registry;
    private final List<Chamber> chambers;
    private final WorkSchedule schedule;
    private final List<BatchEvent> events;

    PracticeData(Path path) throws IOException {
        document = readObject(path);
        if (!document.keySet().equals(Set.of("batches", "chambers", "operation_types",
                "batch_operations", "measurements", "quality_checks"))) {
            throw new IllegalArgumentException("В JSON нужны шесть наборов сущностей КТ2.");
        }
        var chamberMap = rows("chambers", r -> new Chamber(string(r,"id"), string(r,"name"),
                number(r,"temperature"), number(r,"humidity")));
        chambers = List.copyOf(chamberMap.values());
        var batches = rows("batches", PracticeData::batch);
        for (Batch batch : batches.values()) {
            if (batch.chamberId() != null) requireId(chamberMap, batch.chamberId(), "камера");
        }
        registry = new BatchRegistry(List.copyOf(batches.values()));
        var types = rows("operation_types", r -> new OperationType(string(r,"id"), string(r,"name")));
        var works = rows("batch_operations", r -> {
            String typeId = string(r,"operation_type_id");
            requireId(types, typeId, "вид операции");
            String completed = nullableString(r,"completed_date");
            return new BatchOperation(string(r,"id"), batchId(r,batches), typeId,
                    LocalDate.parse(string(r,"planned_date")), completed == null ? null : LocalDate.parse(completed));
        });
        schedule = new WorkSchedule(List.copyOf(works.values()));
        events = new ArrayList<>();
        events.addAll(rows("measurements", r -> new Measurement(string(r,"id"), batchId(r,batches),
                LocalDateTime.parse(string(r,"measured_at")), number(r,"temperature"), number(r,"humidity"))).values());
        events.addAll(rows("quality_checks", r -> new QualityCheck(string(r,"id"), batchId(r,batches),
                LocalDateTime.parse(string(r,"checked_at")), string(r,"decision"), string(r,"comment"))).values());
    }

    WorkSchedule schedule() { return schedule; }

    Map<String, List<String>> selectChambers() { return registry.selectChambers(chambers); }

    List<String> eventSummaries() {
        return events.stream().map(BatchEvent::summary).toList();
    }

    String register(Path card) throws IOException {
        JsonObject row = readObject(card);
        Batch newBatch = batch(row);
        registry.registerBatch(newBatch);
        document.getAsJsonArray("batches").add(row);
        return newBatch.id();
    }

    void save(Path file) throws IOException {
        Path target = file.toAbsolutePath();
        Path temp = Files.createTempFile(target.getParent(), "batches-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temp)) {
                JSON.toJson(document, writer);
            }
            try {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static JsonObject readObject(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject object = JSON.fromJson(reader, JsonObject.class);
            if (object == null) throw new IllegalArgumentException("Ожидался объект JSON в " + path);
            return object;
        }
    }

    private <T> Map<String,T> rows(String name, Function<JsonObject,T> convert) {
        Map<String,T> result = new LinkedHashMap<>();
        for (JsonElement item : document.get(name).getAsJsonArray()) {
            JsonObject row = item.getAsJsonObject();
            String id = string(row,"id");
            if (result.putIfAbsent(id, convert.apply(row)) != null) {
                throw new IllegalArgumentException("Повтор номера " + id + " в " + name);
            }
        }
        return result;
    }

    private static void requireId(Map<String,?> rows, String id, String kind) {
        if (!rows.containsKey(id)) throw new IllegalArgumentException("Не найдена ссылка (" + kind + "): " + id);
    }

    private static String batchId(JsonObject row, Map<String,Batch> batches) {
        String id = string(row,"batch_id");
        requireId(batches, id, "партия");
        return id;
    }

    private static Batch batch(JsonObject row) {
        if (!row.keySet().equals(Set.of("id", "name", "head_count", "start_date", "target_date",
                "temp_min", "temp_max", "humidity_min", "humidity_max", "chamber_id", "status"))) {
            throw new IllegalArgumentException("Проверьте состав полей карточки партии.");
        }
        JsonElement count = row.get("head_count");
        if (!count.isJsonPrimitive() || !count.getAsJsonPrimitive().isNumber()
                || !count.toString().matches("-?[0-9]+")) {
            throw new IllegalArgumentException("Количество голов должно быть целым числом.");
        }
        return new Batch(string(row,"id"), string(row,"name"), new BigDecimal(count.getAsString()).intValueExact(),
                LocalDate.parse(string(row,"start_date")), LocalDate.parse(string(row,"target_date")),
                number(row,"temp_min"), number(row,"temp_max"), number(row,"humidity_min"), number(row,"humidity_max"),
                nullableString(row,"chamber_id"), string(row,"status"));
    }

    private static String nullableString(JsonObject row, String key) {
        if (row.has(key) && row.get(key).isJsonNull()) return null;
        return string(row,key);
    }

    private static String string(JsonObject row, String key) {
        JsonElement value = row.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("В поле " + key + " ожидается строка.");
        }
        return value.getAsString();
    }

    private static double number(JsonObject row, String key) {
        JsonElement value = row.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()
                || !Double.isFinite(value.getAsDouble())) {
            throw new IllegalArgumentException("В поле " + key + " ожидается конечное число.");
        }
        return value.getAsDouble();
    }
}

