"""Проверки: python test_checks.py. Без сторонних библиотек и собственных классов."""
import json
from pathlib import Path
from copy import deepcopy
from algorithms import register_batch, build_worklist, select_chambers
from main import load_data


def expect_error(function, *args):
    try:
        function(*args)
    except (ValueError, TypeError):
        return
    raise AssertionError("Ожидалась ошибка входных данных")


def main():
    folder = Path(__file__).resolve().parent
    data = load_data(folder / "demo.json")
    batch = json.loads((folder / "new_batch.json").read_text(encoding="utf-8"))
    original = deepcopy(data)
    added = register_batch(data["batches"], batch)
    assert len(added) == 4 and len(data["batches"]) == 3
    assert added[-1] == batch and added[-1] is not batch
    expect_error(register_batch, added, batch)
    expect_error(register_batch, [], dict(batch, target_date="2026-10-01"))
    expect_error(register_batch, [], dict(batch, temp_min=20, temp_max=10))
    expect_error(register_batch, [], dict(batch, humidity_min=-1))
    expect_error(register_batch, [], dict(batch, head_count=True))
    expect_error(register_batch, [], dict(batch, name=" "))
    expect_error(register_batch, [], dict(batch, temp_min=float("nan")))
    assert build_worklist([], "2026-11-01") == []
    assert build_worklist(data["batch_operations"], "2026-11-01") == [
        {"operation_id": "W1", "status": "просрочено"},
        {"operation_id": "W2", "status": "сегодня"}]
    assert build_worklist(data["batch_operations"], "2026-10-29") == []
    pending = [row for row in added if row["status"] == "awaiting"]
    assert select_chambers(pending, data["chambers"]) == [
        {"batch_id": "B2", "chamber_ids": ["C1", "C3"]},
        {"batch_id": "B3", "chamber_ids": ["C2"]},
        {"batch_id": "B4", "chamber_ids": []}]
    assert select_chambers([], data["chambers"]) == []
    assert select_chambers([batch], []) == [{"batch_id": "B4", "chamber_ids": []}]
    # Обе границы включаются; выход за любую из них исключает камеру.
    cameras = [
        {"id": "low", "temperature": 16, "humidity": 60},
        {"id": "high", "temperature": 18, "humidity": 65},
        {"id": "bad_t", "temperature": 18.1, "humidity": 62},
        {"id": "bad_h", "temperature": 17, "humidity": 65.1}]
    assert select_chambers([batch], cameras)[0]["chamber_ids"] == ["low", "high"]
    assert data == original, "Алгоритмы не должны менять входные коллекции"
    print("Все проверки пройдены: регистрация, сроки, границы режима, пустые данные, неизменность входа.")


if __name__ == "__main__":
    main()
