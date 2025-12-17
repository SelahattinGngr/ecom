package selahattin.dev.ecom.service.domain;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.request.CreateAddressRequest;
import selahattin.dev.ecom.dto.response.AddressResponse;
import selahattin.dev.ecom.entity.AddressEntity;
import selahattin.dev.ecom.entity.CityEntity;
import selahattin.dev.ecom.entity.DistrictEntity;
import selahattin.dev.ecom.entity.UserEntity;
import selahattin.dev.ecom.exception.auth.UnauthorizedException;
import selahattin.dev.ecom.exception.user.ResourceNotFoundException;
import selahattin.dev.ecom.repository.AddressRepository;
import selahattin.dev.ecom.repository.CityRepository;
import selahattin.dev.ecom.repository.DistrictRepository;

@Service
@RequiredArgsConstructor
public class UserAddressService {

    private final UserService userService;
    private final AddressRepository addressRepository;
    private final CityRepository cityRepository;
    private final DistrictRepository districtRepository;

    public List<AddressResponse> getMyAddresses() {
        UserEntity currentUser = userService.getCurrentUser();

        List<AddressEntity> addresses = addressRepository.findAllByUserId(currentUser.getId());

        return addresses.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AddressResponse createAddress(CreateAddressRequest request) {
        UserEntity currentUser = userService.getCurrentUser();

        CityEntity city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException("Şehir bulunamadı"));

        DistrictEntity district = districtRepository.findById(request.getDistrictId())
                .orElseThrow(() -> new ResourceNotFoundException("İlçe bulunamadı"));

        // İlçe o şehre mi ait kontrolü (Validasyon)
        if (!district.getCity().getId().equals(city.getId())) {
            throw new IllegalArgumentException("Seçilen ilçe, seçilen şehre ait değil!");
        }

        AddressEntity address = AddressEntity.builder()
                .user(currentUser)
                .title(request.getTitle())
                .city(city)
                .district(district)
                .neighborhood(request.getNeighborhood())
                .fullAddress(request.getFullAddress())
                .contactName(request.getContactName())
                .contactPhone(request.getContactPhone())
                .build();

        AddressEntity saved = addressRepository.save(address);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteAddress(UUID addressId) {
        UserEntity currentUser = userService.getCurrentUser();
        AddressEntity address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Adres bulunamadı"));

        // Başkasının adresini silemesin! (Güvenlik)
        if (!address.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("Bu adresi silmeye yetkiniz yok.");
        }

        addressRepository.delete(address);
    }

    // Entity -> DTO Çevirici
    private AddressResponse mapToResponse(AddressEntity entity) {
        return AddressResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .cityName(entity.getCity().getName())
                .districtName(entity.getDistrict().getName())
                .neighborhood(entity.getNeighborhood())
                .fullAddress(entity.getFullAddress())
                .contactName(entity.getContactName())
                .contactPhone(entity.getContactPhone())
                .build();
    }
}