package com.monstarbill.masters.service.impl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monstarbill.masters.commons.AppConstants;
import com.monstarbill.masters.commons.CommonUtils;
import com.monstarbill.masters.commons.CustomException;
import com.monstarbill.masters.commons.CustomMessageException;
import com.monstarbill.masters.commons.FilterNames;
import com.monstarbill.masters.dao.impl.LocationDaoImpl;
import com.monstarbill.masters.enums.Operation;
import com.monstarbill.masters.feignclient.SetupServiceClient;
import com.monstarbill.masters.models.Location;
import com.monstarbill.masters.models.LocationAddress;
import com.monstarbill.masters.models.LocationHistory;
import com.monstarbill.masters.models.Subsidiary;
import com.monstarbill.masters.payload.request.PaginationRequest;
import com.monstarbill.masters.payload.response.PaginationResponse;
import com.monstarbill.masters.repository.LocationAddressRepository;
import com.monstarbill.masters.repository.LocationHistoryRepository;
import com.monstarbill.masters.repository.LocationRepository;
import com.monstarbill.masters.service.LocationService;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class LocationServiceImpl implements LocationService {

	@Autowired
	private LocationRepository locationRepository;

	@Autowired
	private LocationAddressRepository locationAddressRepository;

	@Autowired
	private LocationHistoryRepository locationHistoryRepository;

	@Autowired
	private LocationDaoImpl locationDao;
	
	@Autowired
	private SetupServiceClient setupServiceClient;

	@Override
	public Location save(Location location) {
		Optional<Location> oldLocation = Optional.ofNullable(null);
		if (location.getId() == null) {
			location.setCreatedBy(CommonUtils.getLoggedInUsername());
		} else {
			// Get the existing object using the deep copy
			oldLocation = this.locationRepository.findByIdAndIsDeleted(location.getId(), false);
			if (oldLocation.isPresent()) {
				try {
					oldLocation = Optional.ofNullable((Location) oldLocation.get().clone());
				} catch (CloneNotSupportedException e) {
					log.error("Error while Cloning the object. Please contact administrator.");
					throw new CustomException("Error while Cloning the object. Please contact administrator.");
				}
			}
		}
		location.setLastModifiedBy(CommonUtils.getLoggedInUsername());
		Location locationSaved;
		try {
			locationSaved = this.locationRepository.save(location);
		} catch (DataIntegrityViolationException e) {
			log.error("Location unique constrain violetd." + e.getMostSpecificCause());
			throw new CustomException("Location unique constrain violetd :" + e.getMostSpecificCause());
		}
		if (locationSaved == null) {
			log.info("Error while saving the Location.");
			throw new CustomMessageException("Error while saving the location.");
		}
		// update the data in account history table
		this.updateLocationHistory(locationSaved, oldLocation);

		// ---------------- SAVING THE LOCATION ADDRESS :: STARTED -----------------------------
		LocationAddress locationAddress = location.getLocationAddress();
		locationAddress.setLocationId(locationSaved.getId());
		locationSaved.setLocationAddress(this.saveAddress(locationAddress));
		// ---------------- SAVInG THE LOCATION ADDRESS :: FINISHED -----------------------------

		return locationSaved;
	}

	@Override
	public Location getLocationById(Long id) {
		Optional<Location> location = Optional.empty();
		location = this.locationRepository.findByIdAndIsDeleted(id, false);
		if (!location.isPresent()) {
			log.info("Location is not found given id : " + id);
			throw new CustomMessageException("Location is not found given id : " + id);
		}
		
		// set parent location
		if (location.get().getParentLocationId() != null) {
			Optional<Location> parentLocation = this.locationRepository.findByIdAndIsDeleted(location.get().getParentLocationId(), false);
			if (parentLocation.isPresent()) {
				location.get().setParentLocationName(parentLocation.get().getLocationName());
			}
		}
		
		// set subsidiary Name
		if (location.get().getSubsidiaryId() != null) {
			Subsidiary subsidiary = this.setupServiceClient.getsubsidiaryById(location.get().getSubsidiaryId());
			if (subsidiary != null) {
				location.get().setSubsidiaryName(subsidiary.getName());				
			}
		}

		Optional<LocationAddress> locationAddress = this.locationAddressRepository.findByLocationIdAndIsDeleted(location.get().getId(), false);
		if(locationAddress.isPresent()) location.get().setLocationAddress(locationAddress.get());
		
		return location.get();
	}

	@Override
	public PaginationResponse findAll(PaginationRequest paginationRequest) {
		List<Location> location = new ArrayList<Location>();

		// preparing where clause
		String whereClause = this.prepareWhereClause(paginationRequest).toString();

		// get list
		location = this.locationDao.findAll(whereClause, paginationRequest);

		// getting count
		Long totalRecords = this.locationDao.getCount(whereClause);

		return CommonUtils.setPaginationResponse(paginationRequest.getPageNumber(), paginationRequest.getPageSize(),
				location, totalRecords);

	}

	private Object prepareWhereClause(PaginationRequest paginationRequest) {
		Long subsidiaryId = null;
		String effectiveFrom = null;
		String effectiveTo = null;
		Map<String, ?> filters = paginationRequest.getFilters();
		
		if (filters.containsKey(FilterNames.SUBSIDIARY_ID))
			subsidiaryId = ((Number) filters.get(FilterNames.SUBSIDIARY_ID)).longValue();
		if (filters.containsKey(FilterNames.EFFECTIVE_FROM)) 
			effectiveFrom = (String) filters.get(FilterNames.EFFECTIVE_FROM);
		if (filters.containsKey(FilterNames.EFFECTIVE_TO)) 
			effectiveTo = (String) filters.get(FilterNames.EFFECTIVE_TO);
		StringBuilder whereClause = new StringBuilder(" AND b.isDeleted is false ");
		if (subsidiaryId != null && subsidiaryId != 0) {
			whereClause.append(" AND b.subsidiaryId = ").append(subsidiaryId);
		}
		if (effectiveFrom!= null) {
			whereClause.append(" AND to_char(b.effectiveFrom, 'yyyy-MM-dd') like '%").append(effectiveFrom).append("%'");
		}
		if (effectiveTo!= null) {
			whereClause.append(" AND to_char(b.effectiveTo, 'yyyy-MM-dd') like '%").append(effectiveTo).append("%'");
		}
		return whereClause;
	}

	@Override
	public LocationAddress saveAddress(LocationAddress locationAddress) {
		Optional<LocationAddress> oldLocationAddress = Optional.ofNullable(null);

		if (locationAddress.getId() == null) {
			locationAddress.setCreatedBy(CommonUtils.getLoggedInUsername());
		} else {
			// Get the existing object using the deep copy
			oldLocationAddress = this.locationAddressRepository.findByIdAndIsDeleted(locationAddress.getId(), false);
			if (oldLocationAddress.isPresent()) {
				try {
					oldLocationAddress = Optional.ofNullable((LocationAddress) oldLocationAddress.get().clone());
				} catch (CloneNotSupportedException e) {
					log.error("Error while Cloning the object. Please contact administrator.");
					throw new CustomException("Error while Cloning the object. Please contact administrator.");
				}
			}
		}
		locationAddress.setLastModifiedBy(CommonUtils.getLoggedInUsername());
		locationAddress = this.locationAddressRepository.save(locationAddress);

		if (locationAddress == null) {
			log.info("Error while saving the subsidiary address.");
			throw new CustomMessageException("Error while saving the subsidiary Address.");
		}

		if (oldLocationAddress.isPresent()) {
			// insert the updated fields in history table
			List<LocationHistory> locationHistories = new ArrayList<LocationHistory>();
			try {
				locationHistories = oldLocationAddress.get().compareFields(locationAddress);
				if (CollectionUtils.isNotEmpty(locationHistories)) {
					this.locationHistoryRepository.saveAll(locationHistories);
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				log.error("Error while comparing the new and old objects. Please contact administrator.");
				throw new CustomException(
						"Error while comparing the new and old objects. Please contact administrator.");
			}
			log.info("Supplied History is updated successfully");
		} else {
			// Insert in history table as Operation - INSERT
			this.locationHistoryRepository.save(this.prepareLocationHistory(locationAddress.getLocationId(),
					locationAddress.getId(), AppConstants.LOCATION_ADDRESS, Operation.CREATE.toString(),
					locationAddress.getLastModifiedBy(), null, String.valueOf(locationAddress.getId())));
		}

		log.info("Subsidiary Address saved successfully.");
		return locationAddress;
	}

	@Override
	public LocationAddress getAddressById(Long id) {
		Optional<LocationAddress> locationAddress = Optional.ofNullable(new LocationAddress());
		locationAddress = this.locationAddressRepository.findByIdAndIsDeleted(id, false);

		if (!locationAddress.isPresent()) {
			log.info("Address not found against the locationId : " + id);
			throw new CustomMessageException("Address not found against the location : " + id);
		}
		return locationAddress.get();
	}

	@Override
	public boolean deleteById(Long id) {
		Location location = new Location();
		location = this.getLocationById(id);
		location.setDeleted(true);
		location = this.locationRepository.save(location);
		if (location == null) {
			log.error("Error while deleting the location : " + id);
			throw new CustomMessageException("Error while deleting the location : " + id);
		}
		// update the operation in the history
		this.locationHistoryRepository.save(this.prepareLocationHistory(location.getId(), null, AppConstants.LOCATION,
				Operation.DELETE.toString(), location.getLastModifiedBy(), String.valueOf(location.getId()), null));
		return true;
	}

	private void updateLocationHistory(Location location, Optional<Location> oldLocation) {
		if (oldLocation.isPresent()) {
			// insert the updated fields in history table
			List<LocationHistory> locationHistories = new ArrayList<LocationHistory>();
			try {
				locationHistories = oldLocation.get().compareFields(location);
				if (CollectionUtils.isNotEmpty(locationHistories)) {
					this.locationHistoryRepository.saveAll(locationHistories);
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				log.error("Error while comparing the new and old objects. Please contact administrator.");
				throw new CustomException(
						"Error while comparing the new and old objects. Please contact administrator.");
			}
			log.info("location History is updated successfully");
		} else {
			// Insert in history table as Operation - INSERT
			this.locationHistoryRepository.save(this.prepareLocationHistory(location.getId(), null,
					AppConstants.LOCATION, Operation.CREATE.toString(), location.getLastModifiedBy(), null,
					String.valueOf(location.getId())));
		}
	}

	/**
	 * Prepares the history for the location
	 * 
	 * @param accountId
	 * @param moduleName
	 * @param operation
	 * @param lastModifiedBy
	 * @param oldValue
	 * @param newValue
	 * @return
	 */
	public LocationHistory prepareLocationHistory(Long locationId, Long childId, String moduleName, String operation,
			String lastModifiedBy, String oldValue, String newValue) {
		LocationHistory locationHistory = new LocationHistory();
		locationHistory.setLocationId(locationId);
		locationHistory.setChildId(childId);
		locationHistory.setModuleName(moduleName);
		locationHistory.setChangeType(AppConstants.UI);
		locationHistory.setOperation(operation);
		locationHistory.setOldValue(oldValue);
		locationHistory.setNewValue(newValue);
		locationHistory.setLastModifiedBy(lastModifiedBy);
		return locationHistory;
	}

	@Override
	public List<LocationHistory> findHistoryById(Long id, Pageable pageable) {
		return this.locationHistoryRepository.findByLocationId(id, pageable);
	}

	@Override
	public List<Location> getParentLocationNames(Long subsidiaryId) {
		return this.locationRepository.getLocationsBySubsidiary(subsidiaryId, false);
	}

	/**
	 * To get list(id, name) only to display in the Dropdown
	 */
	@Override
	public Map<Long, String> getAllLocations(Long subsidiaryId) {
		return this.locationRepository.findIdAndNameMap(subsidiaryId, false);
	}
	
	@Override
	public Boolean getValidateName(String name) {
		// if name is empty then name is not valid
		if (StringUtils.isEmpty(name)) return false;
		
		Long countOfRecordsWithSameName = this.locationRepository.getCountByName(name.trim());
		// if we we found the count greater than 0 then it is not valid. If it is zero then it is valid string
		if (countOfRecordsWithSameName > 0) return false; else return true;
	}

	@Override
	public List<Location> getLocationNames(List<Long> subsidiaryId) {
		List<Location> locations = new ArrayList<Location>();
		locations = this.locationRepository.getAllLocationBySubsidiaryId(subsidiaryId, false);
		log.info("Get all location  by multiple subsidary id and type ." + locations);
		return locations;
	}

	@Override
	public List<Location> getLocationsByLocationName(String locationName) {
		List<Location> locations = new ArrayList<Location>();
		locations = this.locationRepository.findByLocationNameAndIsDeleted(locationName, false);
		log.info("Get all location by Location name : " + locations);
		return locations;
	}
	
	@Override
	public List<Location> findLocationNamesByIds(List<Long> locationIds) {
		return this.locationRepository.getLocationsByIds(locationIds);
	}
}
