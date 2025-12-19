package selahattin.dev.ecom.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.request.CreateAddressRequest;
import selahattin.dev.ecom.dto.request.UpdateProfileRequest;
import selahattin.dev.ecom.dto.response.AddressResponse;
import selahattin.dev.ecom.dto.response.CurrentUserResponse;
import selahattin.dev.ecom.response.ApiResponse;
import selahattin.dev.ecom.service.domain.UserAddressService;
import selahattin.dev.ecom.service.domain.UserService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final UserAddressService userAddressService;

    /**
     * Current User APIs
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> getCurrentUser() {
        return ResponseEntity
                .ok(ApiResponse.success("Kullanıcı bilgileri alındı.", userService.getCurrentUserInfo()));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity
                .ok(ApiResponse.success("Profil güncellendi.", userService.updateProfile(request)));
    }

    /**
     * User Address APIs
     */

    @GetMapping("/addresses")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getMyAddresses() {
        return ResponseEntity
                .ok(ApiResponse.success("Adresler listelendi.", userAddressService.getMyAddresses()));
    }

    @GetMapping("/addresses/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponse>> getAddress(@PathVariable UUID addressId) {
        return ResponseEntity
                .ok(ApiResponse.success("Adres bilgileri alındı.", userAddressService.getAddress(addressId)));
    }

    @PutMapping("/addresses/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @PathVariable UUID addressId,
            @Valid @RequestBody CreateAddressRequest request) {
        return ResponseEntity
                .ok(ApiResponse.success("Adres güncellendi.", userAddressService.updateAddress(addressId, request)));
    }

    @PostMapping("/addresses")
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(
            @Valid @RequestBody CreateAddressRequest request) {
        return ResponseEntity
                .ok(ApiResponse.success("Adres eklendi.", userAddressService.createAddress(request)));
    }

    @DeleteMapping("/addresses/{addressId}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(@PathVariable UUID addressId) {
        userAddressService.deleteAddress(addressId);
        return ResponseEntity.ok(ApiResponse.success("Adres silindi."));
    }

}
