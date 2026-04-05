package selahattin.dev.ecom.utils.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ProductStatus {
    ACTIVE("active"),
    DELETED("deleted"),
    ALL("all");

    private final String value;

    ProductStatus(String value) {
        this.value = value;
    }

    @JsonCreator
    public static ProductStatus fromValue(String value) {
        for (ProductStatus status : ProductStatus.values()) {
            if (status.value.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }
}
