package selahattin.dev.ecom.service.infra.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private final Path rootLocation = Paths.get("assets/public/products");

    public FileStorageService() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Upload dizini oluşturulamadı!");
        }
    }

    public String save(MultipartFile file) {
        try {
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Files.copy(file.getInputStream(), this.rootLocation.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            return "/assets/public/products/" + fileName; // client'e döneceğimiz URL
        } catch (Exception e) {
            throw new RuntimeException("Dosya yüklenemedi: " + e.getMessage());
        }
    }
}