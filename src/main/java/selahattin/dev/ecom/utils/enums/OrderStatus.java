package selahattin.dev.ecom.utils.enums;

public enum OrderStatus {
    PENDING, PAID, PREPARING, SHIPPED, DELIVERED, CANCELLED, RETURN_REQUESTED, RETURNED;

    public static OrderStatus fromValue(String value) {
        for (OrderStatus status : OrderStatus.values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }
}