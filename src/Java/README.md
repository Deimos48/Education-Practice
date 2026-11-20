# Список работ по выдержке партий

Первый этап Java-реализации. Пример использует пять работ W1–W5 из demo.json КТ2. В список на выбранную дату входят незавершённые работы с наступившим сроком в исходном порядке.

Сборка и запуск из src/java с JDK 21 в PowerShell:

```powershell
javac -encoding UTF-8 -d out BatchOperation.java WorkSchedule.java WorklistDemo.java
java -cp out WorklistDemo 2026-11-01
```

Ожидаемый вывод:

```text
W1 — просрочено
W2 — сегодня
```

На дату 2026-10-29 список пуст. Если дату опустить, используется 2026-11-01.
