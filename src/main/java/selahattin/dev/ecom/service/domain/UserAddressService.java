package selahattin.dev.ecom.service.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.request.CreateAddressRequest;
import selahattin.dev.ecom.dto.response.AddressResponse;
import selahattin.dev.ecom.entity.auth.UserEntity;
import selahattin.dev.ecom.entity.location.AddressEntity;
import selahattin.dev.ecom.entity.location.CityEntity;
import selahattin.dev.ecom.entity.location.CountryEntity;
import selahattin.dev.ecom.entity.location.DistrictEntity;
import selahattin.dev.ecom.exception.BusinessException;
import selahattin.dev.ecom.exception.ErrorCode;
import selahattin.dev.ecom.repository.location.AddressRepository;
import selahattin.dev.ecom.repository.location.CityRepository;
import selahattin.dev.ecom.repository.location.CountryRepository;
import selahattin.dev.ecom.repository.location.DistrictRepository;

@Service
@RequiredArgsConstructor
public class UserAddressService {

    private final UserService userService;
    private final AddressRepository addressRepository;
    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;
    private final DistrictRepository districtRepository;

    public List<AddressResponse> getMyAddresses() {
        UserEntity currentUser = userService.getCurrentUser();
        List<AddressEntity> addresses = addressRepository.findAllByUserId(currentUser.getId());
        return addresses.stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public AddressResponse createAddress(CreateAddressRequest request) {
        UserEntity currentUser = userService.getCurrentUser();

        CountryEntity country = countryRepository.findById(request.getCountryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COUNTRY_NOT_FOUND));

        CityEntity city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CITY_NOT_FOUND));

        DistrictEntity district = districtRepository.findById(request.getDistrictId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DISTRICT_NOT_FOUND));

        if (!district.getCity().getId().equals(city.getId())) {
            throw new BusinessException(ErrorCode.DISTRICT_CITY_MISMATCH);
        }

        AddressEntity address = AddressEntity.builder()
                .user(currentUser)
                .title(request.getTitle())
                .country(country)
                .city(city)
                .district(district)
                .neighborhood(request.getNeighborhood())
                .fullAddress(request.getFullAddress())
                .postalCode(request.getZipCode())
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
                .orElseThrow(() -> new BusinessException(ErrorCode.ADDRESS_NOT_FOUND));

        if (!address.getUser().getId().equals(currentUser.getId())) {
            throw new BusinessException(ErrorCode.ADDRESS_ACCESS_DENIED, "Bu adresi silmeye yetkiniz yok.");
        }

        addressRepository.delete(address);
    }

    @Transactional
    public AddressResponse updateAddress(UUID addressId, CreateAddressRequest request) {
        UserEntity currentUser = userService.getCurrentUser();
        AddressEntity address = addressRepository.findById(addressId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADDRESS_NOT_FOUND));

        if (!address.getUser().getId().equals(currentUser.getId())) {
            throw new BusinessException(ErrorCode.ADDRESS_ACCESS_DENIED, "Bu adresi güncellemeye yetkiniz yok.");
        }

        // Country update eklendi
        if (!address.getCountry().getId().equals(request.getCountryId())) {
            CountryEntity country = countryRepository.findById(request.getCountryId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.COUNTRY_NOT_FOUND));
            address.setCountry(country);
        }

        boolean cityChanged = !address.getCity().getId().equals(request.getCityId());
        boolean districtChanged = !address.getDistrict().getId().equals(request.getDistrictId());

        if (cityChanged || districtChanged) {
            CityEntity city = cityRepository.findById(request.getCityId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.CITY_NOT_FOUND));

            DistrictEntity district = districtRepository.findById(request.getDistrictId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.DISTRICT_NOT_FOUND));

            if (!district.getCity().getId().equals(city.getId())) {
                throw new BusinessException(ErrorCode.DISTRICT_CITY_MISMATCH);
            }

            address.setCity(city);
            address.setDistrict(district);
        }

        address.setTitle(request.getTitle());
        address.setNeighborhood(request.getNeighborhood());
        address.setFullAddress(request.getFullAddress());
        address.setContactName(request.getContactName());
        address.setContactPhone(request.getContactPhone());
        address.setPostalCode(request.getZipCode());

        AddressEntity updatedAddress = addressRepository.save(address);
        return mapToResponse(updatedAddress);
    }

    public AddressResponse getAddress(UUID addressId) {
        UserEntity currentUser = userService.getCurrentUser();
        AddressEntity address = addressRepository.findById(addressId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADDRESS_NOT_FOUND));

        if (!address.getUser().getId().equals(currentUser.getId())) {
            throw new BusinessException(ErrorCode.ADDRESS_ACCESS_DENIED);
        }
        return mapToResponse(address);
    }

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