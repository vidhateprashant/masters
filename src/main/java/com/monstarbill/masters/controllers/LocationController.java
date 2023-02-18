package com.monstarbill.masters.controllers;

import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.monstarbill.masters.models.Location;
import com.monstarbill.masters.models.LocationAddress;
import com.monstarbill.masters.models.LocationHistory;
import com.monstarbill.masters.payload.request.PaginationRequest;
import com.monstarbill.masters.payload.response.PaginationResponse;
import com.monstarbill.masters.service.LocationService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/location")
@CrossOrigin(origins= "*", allowedHeaders = "*", maxAge = 4800, allowCredentials = "false" )
public class LocationController {

	@Autowired
	private LocationService locationService;

	/**
	 * This saves the location
	 * 
	 * @param location
	 * @return
	 */
	@PostMapping("/save")
	public ResponseEntity<Location> saveLocation(@Valid @RequestBody Location location) {
		log.info("Saving the location :: " + location.toString());
		location = locationService.save(location);
		log.info("location saved successfully");
		return ResponseEntity.ok(location);
	}

	/**
	 * get the location by id
	 * 
	 * @param id
	 * @return
	 */
	@GetMapping("/get")
	public ResponseEntity<Location> findById(@RequestParam Long id) {
		log.info("Get location for ID :: " + id);
		Location location = locationService.getLocationById(id);
		if (location == null) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		log.info("Returning from find by id location");
		return new ResponseEntity<>(location, HttpStatus.OK);
	}

	/**
	 * get the all location value
	 * 
	 * @return
	 */
	@PostMapping("/get/all")
		public ResponseEntity<PaginationResponse> findAll(@RequestBody PaginationRequest paginationRequest) {
			log.info("Get all location started.");
			PaginationResponse paginationResponse = new PaginationResponse();
			paginationResponse = locationService.findAll(paginationRequest);
			log.info("Get all location completed.");
			return new ResponseEntity<>(paginationResponse, HttpStatus.OK);
		}

	/**
	 * delete the id by location
	 * 
	 * @param id
	 * @return
	 */
	@GetMapping("/delete")
	public ResponseEntity<Boolean> deleteById(@RequestParam Long id) {
		log.info("Delete location by ID :: " + id);
		boolean isDeleted = false;
		isDeleted = locationService.deleteById(id);
		log.info("Delete location by ID Completed.");
		return new ResponseEntity<>(isDeleted, HttpStatus.OK);
	}

	/**
	 * saving the location address
	 * 
	 * @param locationAddress
	 * @return
	 */
	@Deprecated
	@PostMapping("/address/save")
	public ResponseEntity<LocationAddress> saveLocationAddress(@RequestBody LocationAddress locationAddress) {
		log.info("Saving the location Address :: " + locationAddress.toString());
		locationAddress = locationService.saveAddress(locationAddress);
		log.info("location address saved successfully");
		return ResponseEntity.ok(locationAddress);
	}

	/**
	 * get the address by id
	 * 
	 * @param id
	 * @return
	 */
	@GetMapping("/address/get")
	public ResponseEntity<LocationAddress> getLocationAddress(@RequestParam Long id) {
		return new ResponseEntity<>(locationService.getAddressById(id), HttpStatus.OK);
	}

	/**
	 * Find history by Location Id Supported for server side pagination
	 * 
	 * @param id
	 * @param pageSize
	 * @param pageNumber
	 * @return
	 */
	@GetMapping("/get/history")
	public ResponseEntity<List<LocationHistory>> findHistoryById(@RequestParam Long id,
			@RequestParam(defaultValue = "10") int pageSize, @RequestParam(defaultValue = "0") int pageNumber,
			@RequestParam(defaultValue = "id") String sortColumn) {
		log.info("Get location Audit  :: " + id);
		Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(sortColumn));
		List<LocationHistory> locationHistoris = this.locationService.findHistoryById(id, pageable);
		log.info("Returning from Location Audit by id.");
		return new ResponseEntity<>(locationHistoris, HttpStatus.OK);
	}

	/**
	 * get all locations based on subsidiary
	 * @return
	 */
	@GetMapping("/get-parent-location-names")
	public ResponseEntity<List<Location>> getParentName(@RequestParam Long subsidiaryId) {
		log.info("Get locations for subsidiary ID :: " + subsidiaryId);
		List<Location> locations = locationService.getParentLocationNames(subsidiaryId);
		if (locations == null) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		log.info("Returning from find by subsidiary Id");
		return new ResponseEntity<>(locations, HttpStatus.OK);
	}
	
	/**
	 * To get list(id, name only) to display in the Dropdown
	 * @return
	 */
	@GetMapping("/get/all/lov")
	public ResponseEntity<Map<Long, String>> getAllLocationsForLov(@RequestParam Long subsidiaryId) {
		return new ResponseEntity<>(this.locationService.getAllLocations(subsidiaryId), HttpStatus.OK);
	}
	
	@GetMapping("/is-valid-name")
	public ResponseEntity<Boolean> validateName(@RequestParam String name) {
		return new ResponseEntity<>(this.locationService.getValidateName(name), HttpStatus.OK);
	}
	

	/**
	 * get all locations based on multiple subsidiary
	 * @return
	 */
	@GetMapping("/get-location-names")
	public ResponseEntity<List<Location>> getParentName(@RequestParam List<Long> subsidiaryId) {
		log.info("Get locations for multiple subsidiary ID :: " + subsidiaryId);
		List<Location> locations = locationService.getLocationNames(subsidiaryId);
		if (locations == null) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		log.info("Returning from find by multiple subsidiary Id");
		return new ResponseEntity<>(locations, HttpStatus.OK);
	}
	
	@GetMapping("/get-locations-by-names")
	public ResponseEntity<List<Location>> getLocationsByLocationName(@RequestParam String name) {
		log.info("getLocationsByLocationName started");
		List<Location> locations = locationService.getLocationsByLocationName(name);
		log.info("getLocationsByLocationName finished");
		return new ResponseEntity<>(locations, HttpStatus.OK);
	}
	
	@PostMapping("/find-location-names-by-ids")
	public ResponseEntity<List<Location>> findLocationNamesByIds(@RequestBody List<Long> locationIds) {
		log.info("Get locations for multiple location IDs :: " + locationIds);
		List<Location> locations = locationService.findLocationNamesByIds(locationIds);
		log.info("Returning from find by multiple location IDs. ");
		return new ResponseEntity<>(locations, HttpStatus.OK);
	}
	
}
