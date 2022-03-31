package ca.bc.gov.educ.api.dataconversion.constant;

import java.util.Optional;

public enum ConversionResultType {
    SUCCESS("Y"),
    FAILURE("F"),
    WARNING("W");

    private final String value;

    ConversionResultType(String value) {
        this.value = value;
    }

    public static Optional<ConversionResultType> fromValue(String value) {
        for (ConversionResultType crt : ConversionResultType.values()) {
            if (String.valueOf(crt.value).equalsIgnoreCase(value)) {
                return Optional.of(crt);
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
