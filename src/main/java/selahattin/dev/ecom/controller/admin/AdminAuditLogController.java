package selahattin.dev.ecom.controller.admin;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.response.admin.AuditLogResponse;
import selahattin.dev.ecom.response.ApiResponse;
import selahattin.dev.ecom.service.domain.admin.AuditLogService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/logs")
public class AdminAuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping("/audit")
    @PreAuthorize("hasAuthority('system:manage')")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAuditLogs(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<AuditLogResponse> logs = auditLogService.getAuditLogs(userId, entityType, action, pageable);
        return ResponseEntity.ok(ApiResponse.success("OK", logs));
    }
}
