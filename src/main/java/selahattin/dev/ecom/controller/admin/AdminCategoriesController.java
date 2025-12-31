package selahattin.dev.ecom.controller.admin;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/**
 * - `GET /api/v1/admin/categories` - Kategorileri listele (admin)
 * - `POST /api/v1/admin/categories` - Yeni kategori oluştur
 * - `PATCH /api/v1/admin/categories/:id` - Kategoriyi güncelle
 * - `DELETE /api/v1/admin/categories/:id` - Kategoriyi sil (soft delete)
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/Categories")
public class AdminCategoriesController {

}
