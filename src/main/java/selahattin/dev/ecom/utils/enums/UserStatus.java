package selahattin.dev.ecom.utils.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum UserStatus {
    ACTIVE,
    DELETED,
    ALL;

    @JsonCreator
    public static UserStatus fromString(String value) {
        if (value == null || value.trim().isBlank()) {
            return UserStatus.ACTIVE;
        }
        try {
            return UserStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Geçersiz UserStatus değeri: " + value);
        }
    }
}