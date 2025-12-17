package selahattin.dev.ecom.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.entity.CityEntity;
import selahattin.dev.ecom.entity.DistrictEntity;
import selahattin.dev.ecom.repository.CityRepository;
import selahattin.dev.ecom.repository.DistrictRepository;
import selahattin.dev.ecom.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class LocationController {

    private final CityRepository cityRepository;
    private final DistrictRepository districtRepository;

    @GetMapping("/cities")
    public ResponseEntity<ApiResponse<List<CityEntity>>> getAllCities() {
        return ResponseEntity.ok(ApiResponse.success("", cityRepository.findAllByOrderByNameAsc()));
    }

    @GetMapping("/districts")
    public ResponseEntity<ApiResponse<List<DistrictEntity>>> getDistrictsByCity(@RequestParam Integer cityId) {
        return ResponseEntity.ok(ApiResponse.success("", districtRepository.findAllByCityIdOrderByNameAsc(cityId)));
    }
}