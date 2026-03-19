package selahattin.dev.ecom.utils.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum UserStatus {
    ACTIVE("active"),
    DELETED("deleted"),
    ALL("all");

    private final String value;

    UserStatus(String value) {
        this.value = value;
    }

    @JsonCreator
    public static UserStatus fromValue(String value) {
        for (UserStatus status : UserStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid user status: " + value);
    }
}