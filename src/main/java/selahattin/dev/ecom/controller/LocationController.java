package selahattin.dev.ecom.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.response.CityResponse;
import selahattin.dev.ecom.dto.response.CountryResponse;
import selahattin.dev.ecom.dto.response.DistrictResponse;
import selahattin.dev.ecom.entity.location.CityEntity;
import selahattin.dev.ecom.entity.location.DistrictEntity;
import selahattin.dev.ecom.repository.CityRepository;
import selahattin.dev.ecom.repository.CountryRepository;
import selahattin.dev.ecom.repository.DistrictRepository;
import selahattin.dev.ecom.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class LocationController {

    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;
    private final DistrictRepository districtRepository;

    @GetMapping("/countries")
    public ResponseEntity<ApiResponse<List<CountryResponse>>> getAllCountries() {
        List<CountryResponse> countries = countryRepository.findAll().stream()
                .map(country -> CountryResponse.builder()
                        .id(country.getId())
                        .name(country.getName())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("", countries));
    }

    @GetMapping("/cities/{countryId}")
    public ResponseEntity<ApiResponse<List<CityResponse>>> getAllCities(@PathVariable Integer countryId) {
        List<CityEntity> cities = cityRepository.findAllByCountryIdOrderByNameAsc(countryId);
        List<CityResponse> response = cities.stream()
                .map(city -> CityResponse.builder()
                        .id(city.getId())
                        .name(city.getName())
                        // .countryId(city.getCountry().getId())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("", response));
    }

    @GetMapping("/districts/{cityId}")
    public ResponseEntity<ApiResponse<List<DistrictResponse>>> getDistrictsByCity(@PathVariable Integer cityId) {
        List<DistrictEntity> districts = districtRepository.findAllByCityIdOrderByNameAsc(cityId);
        List<DistrictResponse> response = districts.stream()
                .map(district -> DistrictResponse.builder()
                        .id(district.getId())
                        .name(district.getName())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("", response));
    }
}