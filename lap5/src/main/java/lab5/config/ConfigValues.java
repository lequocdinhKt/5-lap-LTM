package lab5.config;

import java.util.Map;
import java.util.Properties;

final class ConfigValues {
    private ConfigValues() { }

    static String text(Properties properties, Map<String, String> environment,
                       String property, String variable, String fallback) {
        String fromEnvironment = environment.get(variable);
        if (fromEnvironment != null && !fromEnvironment.isBlank()) {
            return fromEnvironment.trim();
        }
        return properties.getProperty(property, fallback).trim();
    }

    static int number(Properties properties, Map<String, String> environment,
                      String property, String variable, int fallback) {
        String value = text(properties, environment, property, variable, String.valueOf(fallback));
        try {
            int number = Integer.parseInt(value);
            if (number < 1 || number > 65535) {
                throw new IllegalArgumentException("Cổng không hợp lệ: " + property);
            }
            return number;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Cấu hình số không hợp lệ: " + property, error);
        }
    }

    static String required(Properties properties, Map<String, String> environment,
                           String property, String variable) {
        String value = text(properties, environment, property, variable, "");
        if (value.isBlank() || "replace-me".equalsIgnoreCase(value)) {
            throw new IllegalArgumentException("Thiếu cấu hình: " + property);
        }
        return value;
    }
}
