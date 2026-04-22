package selahattin.dev.ecom.service.infra;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;
import selahattin.dev.ecom.exception.BusinessException;
import selahattin.dev.ecom.exception.ErrorCode;

@Slf4j
@Service
public class FileStorageService {

    private static final List<String> ALLOWED_MIME_TYPES = List.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp", ".gif");

    private final Path rootLocation = Paths.get("assets/public/products");

    public FileStorageService() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.DIRECTORY_CREATION_ERROR);
        }
    }

    public String save(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.EMPTY_FILE);
        }

        String rawFileName = file.getOriginalFilename();

        if (rawFileName == null) {
            throw new BusinessException(ErrorCode.INVALID_FILE_NAME, "Dosya adı eksik.");
        }

        String originalFileName = StringUtils.cleanPath(rawFileName);

        if (originalFileName.contains("..")) {
            throw new BusinessException(ErrorCode.INVALID_FILE_NAME,
                    "Dosya adı güvenlik sınırlarını aşıyor: " + originalFileName);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.INVALID_FILE_NAME, "Desteklenmeyen dosya türü. İzin verilenler: JPEG, PNG, WEBP, GIF");
        }

        String extension = originalFileName.contains(".")
                ? originalFileName.substring(originalFileName.lastIndexOf('.')).toLowerCase()
                : "";
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.INVALID_FILE_NAME, "Desteklenmeyen dosya uzantısı: " + extension);
        }

        try {
            String fileName = UUID.randomUUID().toString() + "_" + originalFileName;

            Files.copy(file.getInputStream(), this.rootLocation.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);

            return "/assets/public/products/" + fileName;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR, "Dosya yüklenemedi.");
        }
    }

    public void delete(String fileUrl) {
        try {
            String fileName = Paths.get(fileUrl).getFileName().toString();
            Files.deleteIfExists(rootLocation.resolve(fileName));
        } catch (IOException e) {
            log.warn("[FILE] Fiziksel dosya silinemedi: {}", fileUrl, e);
        }
    }
}
