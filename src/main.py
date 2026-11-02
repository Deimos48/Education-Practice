"""Запуск: python main.py. Можно передать --date, --data, --batch и --save."""
import argparse
import json
from math import isfinite
from datetime import date
from pathlib import Path
from algorithms import register_batch, build_worklist, select_chambers


def load_data(path):
    """Загрузить JSON и проверить ключи, ссылки и даты перед алгоритмами."""
    with open(path, encoding="utf-8") as file:
        data = json.load(file)
    entities = ("batches", "chambers", "operation_types", "batch_operations",
                "measurements", "quality_checks")
    if set(data) != set(entities):
        raise ValueError("В JSON должны находиться шесть наборов сущностей ER-модели.")
    ids = {}
    for entity in entities:
        ids[entity] = {row["id"] for row in data[entity]}
        if len(ids[entity]) != len(data[entity]):
            raise ValueError("Повтор первичного ключа: " + entity)
    for chamber in data["chambers"]:
        for field in ("temperature", "humidity"):
            if type(chamber[field]) not in (int, float) or not isfinite(chamber[field]):
                raise ValueError("Показания камеры должны быть конечными числами.")
        if not 0 <= chamber["humidity"] <= 100:
            raise ValueError("Влажность камеры должна быть в пределах 0–100%.")
    for batch in data["batches"]:
        # Общие ограничения режима проверяются той же функцией регистрации.
        candidate = dict(batch, chamber_id=None, status="awaiting")
        register_batch([], candidate)
        if batch["chamber_id"] is not None and batch["chamber_id"] not in ids["chambers"]:
            raise ValueError("У партии указана неизвестная камера.")
        if batch["status"] not in ("awaiting", "ripening", "released"):
            raise ValueError("Неизвестный статус партии.")
        if batch["status"] == "awaiting" and batch["chamber_id"] is not None:
            raise ValueError("Ожидающая партия не должна иметь назначенную камеру.")
        if batch["status"] == "ripening" and batch["chamber_id"] is None:
            raise ValueError("Для созревающей партии требуется камера.")
    for entity in ("batch_operations", "measurements", "quality_checks"):
        for row in data[entity]:
            if row["batch_id"] not in ids["batches"]:
                raise ValueError("Ссылка на неизвестную партию: " + entity)
    for work in data["batch_operations"]:
        if work["planned_date"] is None:
            raise ValueError("Для работы требуется плановая дата.")
        if work["operation_type_id"] not in ids["operation_types"]:
            raise ValueError("Ссылка на неизвестный вид операции.")
        for field in ("planned_date", "completed_date"):
            value = work[field]
            if value is not None and date.fromisoformat(value).isoformat() != value:
                raise ValueError("Используйте даты в формате YYYY-MM-DD.")
    return data


def main():
    folder = Path(__file__).resolve().parent
    parser = argparse.ArgumentParser(description="Контроль созревания партий сыра")
    parser.add_argument("--data", type=Path, default=folder / "demo.json")
    parser.add_argument("--batch", type=Path, default=folder / "new_batch.json")
    parser.add_argument("--date", default="2026-11-01")
    parser.add_argument("--save", type=Path, help="Сохранить данные с новой партией в JSON")
    args = parser.parse_args()
    try:
        today = date.fromisoformat(args.date).isoformat()
        data = load_data(args.data)
        with open(args.batch, encoding="utf-8") as file:
            new_batch = json.load(file)
        data["batches"] = register_batch(data["batches"], new_batch)
        print("Зарегистрирована партия:", new_batch["id"])
        print("\nРаботы на", today)
        worklist = build_worklist(data["batch_operations"], today)
        for row in worklist:
            print(row["operation_id"] + ": " + row["status"])
        if not worklist:
            print("Наступивших и просроченных работ нет.")
        pending = [batch for batch in data["batches"] if batch["status"] == "awaiting"]
        print("\nПодходящие камеры для ожидающих партий")
        for row in select_chambers(pending, data["chambers"]):
            print(row["batch_id"] + ": " + (", ".join(row["chamber_ids"]) or "подходящих камер нет"))
        if args.save:
            with open(args.save, "w", encoding="utf-8") as file:
                json.dump(data, file, ensure_ascii=False, indent=2)
            print("\nДанные сохранены:", args.save)
    except (OSError, ValueError, KeyError, TypeError) as error:
        parser.exit(1, "Ошибка: " + str(error) + "\n")


if __name__ == "__main__":
    main()
