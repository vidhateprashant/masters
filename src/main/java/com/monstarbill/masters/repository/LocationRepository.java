package com.monstarbill.masters.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.monstarbill.masters.models.Location;

public interface LocationRepository extends JpaRepository<Location, String> {

	public Optional<Location> findByIdAndIsDeleted(Long id, boolean isDeleted);

	@Query("select new com.monstarbill.masters.models.Location(locationName, id) from Location Where subsidiaryId = :subsidiaryId AND isDeleted = :isDeleted ")
	public List<Location> getLocationsBySubsidiary(@Param("subsidiaryId") Long subsidiaryId, @Param("isDeleted") boolean isDeleted);

	default Map<Long, String> findIdAndNameMap(Long subsidiaryId, boolean isDeleted) {
        return this.getLocationsBySubsidiary(subsidiaryId, isDeleted).stream().collect(Collectors.toMap(Location::getId, Location::getLocationName));
    }

	@Query("select count(id) from Location where lower(locationName) = lower(:name) AND isDeleted = false ")
	public Long getCountByName(@Param("name") String name);

	
	@Query("select new com.monstarbill.masters.models.Location(l.locationName, l.id) from Location l Where l.subsidiaryId in :subsidiaryId AND isDeleted = :isDeleted ")
	public List<Location> getAllLocationBySubsidiaryId(@Param("subsidiaryId") List<Long> subsidiaryId, @Param("isDeleted") boolean isDeleted);

	@Query("select new com.monstarbill.masters.models.Location(locationName, id) from Location Where id IN :ids AND isDeleted = false ")
	public List<Location> getLocationsByIds(List<Long> ids);
	
	public List<Location> findByLocationNameAndIsDeleted(String locationName, boolean isDeleted);
}
