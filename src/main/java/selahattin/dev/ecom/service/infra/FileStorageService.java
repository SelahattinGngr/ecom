package selahattin.dev.ecom.service.infra;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import selahattin.dev.ecom.exception.BusinessException;
import selahattin.dev.ecom.exception.ErrorCode;

@Service
public class FileStorageService {

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

        try {
            String fileName = UUID.randomUUID().toString() + "_" + originalFileName;

            Files.copy(file.getInputStream(), this.rootLocation.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);

            return "/assets/public/products/" + fileName;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR, "Hata detayı: " + e.getMessage());
        }
    }
}