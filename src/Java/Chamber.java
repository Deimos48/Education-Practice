record Chamber(String id, String name, double temperature, double humidity) implements ClimateReading {
    Chamber {
        if (!Double.isFinite(temperature) || !Double.isFinite(humidity) || humidity < 0 || humidity > 100) {
            throw new IllegalArgumentException("Недопустимые показания камеры " + id);
        }
    }
}
