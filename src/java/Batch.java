import java.time.LocalDate;

record Batch(String id, String name, int headCount, LocalDate startDate, LocalDate targetDate,
             double tempMin, double tempMax, double humidityMin, double humidityMax,
             String chamberId, String status) {

    Batch {
        if (id.isBlank() || name.isBlank() || headCount <= 0) {
            throw new IllegalArgumentException("Для партии нужны номер, название и положительное количество голов.");
        }
        if (!targetDate.isAfter(startDate)) {
            throw new IllegalArgumentException("Дата завершения должна быть позже начала.");
        }
        if (!Double.isFinite(tempMin) || !Double.isFinite(tempMax) || tempMin > tempMax
                || !(0 <= humidityMin && humidityMin <= humidityMax && humidityMax <= 100)) {
            throw new IllegalArgumentException("Недопустимые границы режима партии " + id);
        }
        boolean validStatus = switch (status) {
            case "awaiting" -> chamberId == null;
            case "ripening" -> chamberId != null;
            case "released" -> true;
            default -> false;
        };
        if (!validStatus) throw new IllegalArgumentException("Проверьте статус и камеру партии " + id);
    }

    public void validateForRegistration() {
        if (!status.equals("awaiting") || chamberId != null) {
            throw new IllegalArgumentException("Новая партия должна ожидать размещения.");
        }
    }

    public boolean accepts(ClimateReading climate) {
        return tempMin <= climate.temperature() && climate.temperature() <= tempMax
                && humidityMin <= climate.humidity() && climate.humidity() <= humidityMax;
    }
}
