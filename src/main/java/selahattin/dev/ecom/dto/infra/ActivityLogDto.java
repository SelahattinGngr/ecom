package selahattin.dev.ecom.dto.infra;

import java.io.Serializable;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogDto implements Serializable {
    private UUID userId;
    private String deviceId;
    private String ipAddress;
    private String method;
    private String endpoint;
    private int statusCode;
}
