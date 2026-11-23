import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class BatchRegistry {
    private final List<Batch> batches;

    BatchRegistry(List<Batch> batches) {
        this.batches = new ArrayList<>(batches);
    }

    public void registerBatch(Batch batch) {
        batch.validateForRegistration();
        if (batches.stream().anyMatch(existing -> existing.id().equals(batch.id()))) {
            throw new IllegalArgumentException("Партия с номером " + batch.id() + " уже существует.");
        }
        batches.add(batch);
    }

    public Map<String, List<String>> selectChambers(List<Chamber> chambers) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Batch batch : batches) {
            if (batch.status().equals("awaiting")) {
                List<String> candidates = new ArrayList<>();
                for (Chamber chamber : chambers) {
                    if (batch.accepts(chamber)) candidates.add(chamber.id());
                }
                result.put(batch.id(), candidates);
            }
        }
        return result;
    }
}
