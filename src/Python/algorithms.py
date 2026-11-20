"""Процедурные операции контроля созревания. Только стандартная библиотека."""
from datetime import date
from math import isfinite


def register_batch(batches, batch):
    """Проверить карточку и вернуть новый список партий, не изменяя исходный."""
    fields = ("id", "name", "head_count", "start_date", "target_date",
              "temp_min", "temp_max", "humidity_min", "humidity_max",
              "chamber_id", "status")
    if set(batch) != set(fields):
        raise ValueError("Карточка должна содержать все атрибуты партии из ER-модели.")
    if not isinstance(batch["id"], str) or not batch["id"].strip():
        raise ValueError("Идентификатор партии не должен быть пустым.")
    if any(item["id"] == batch["id"] for item in batches):
        raise ValueError("Партия с таким идентификатором уже существует.")
    if not isinstance(batch["name"], str) or not batch["name"].strip():
        raise ValueError("Укажите наименование партии.")
    if type(batch["head_count"]) is not int or batch["head_count"] <= 0:
        raise ValueError("Количество голов должно быть положительным целым числом.")
    start = date.fromisoformat(batch["start_date"])
    target = date.fromisoformat(batch["target_date"])
    if target <= start:
        raise ValueError("Плановая дата завершения должна быть позже начала.")
    for field in ("temp_min", "temp_max", "humidity_min", "humidity_max"):
        if type(batch[field]) not in (int, float) or not isfinite(batch[field]):
            raise ValueError("Границы режима должны быть конечными числами.")
    if batch["temp_min"] > batch["temp_max"]:
        raise ValueError("Нижняя граница температуры больше верхней.")
    if not 0 <= batch["humidity_min"] <= batch["humidity_max"] <= 100:
        raise ValueError("Влажность должна быть в пределах 0–100%.")
    if batch["chamber_id"] is not None or batch["status"] != "awaiting":
        raise ValueError("Новая партия ожидает размещения и ещё не имеет камеры.")
    return batches + [batch.copy()]


def build_worklist(operations, today):
    """Алгоритм 1: O(n) по времени и O(n) по дополнительной памяти."""
    result = []
    for work in operations:
        if work["completed_date"] is None:
            if work["planned_date"] <= today:
                if work["planned_date"] < today:
                    status = "просрочено"
                else:
                    status = "сегодня"
                result.append({"operation_id": work["id"], "status": status})
    return result


def select_chambers(batches, chambers):
    """Алгоритм 2: O(n*m) по времени; результат занимает O(n + n*m)."""
    result = []
    for batch in batches:
        candidates = []
        for chamber in chambers:
            if (batch["temp_min"] <= chamber["temperature"] <= batch["temp_max"]
                    and batch["humidity_min"] <= chamber["humidity"] <= batch["humidity_max"]):
                candidates.append(chamber["id"])
        result.append({"batch_id": batch["id"], "chamber_ids": candidates})
    return result
