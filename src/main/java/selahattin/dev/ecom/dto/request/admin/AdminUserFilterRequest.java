package selahattin.dev.ecom.dto.request.admin;

import lombok.Getter;
import lombok.Setter;

import selahattin.dev.ecom.utils.enums.UserStatus;

@Getter
@Setter
public class AdminUserFilterRequest {
    private String roleName;
    private UserStatus status = UserStatus.ACTIVE;
    // yarın yeni filtre gelirse buraya ekle
}