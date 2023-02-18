package com.monstarbill.masters.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.monstarbill.masters.models.Project;

public interface ProjectRepository extends JpaRepository<Project, String>{
	
	public Optional<Project> findByIdAndIsDeleted(Long id, boolean isDeleted);
	
	public List<Project> findByIsDeleted(boolean isDeleted);
	
	default Map<Long, String> findIdAndNameMap(boolean isDeleted) {
        return this.findByIsDeleted(isDeleted).stream().collect(Collectors.toMap(Project::getId, Project::getName));
    }

	@Query("select count(id) from Project where lower(name) = lower(:name) AND isDeleted = false ")
	public Long getCountByName(@Param("name") String name);

	@Query("select p from Project p where subsidiaryId = :subsidiaryId AND (schedulingEndDate is null or to_char(schedulingEndDate, 'yyyy-MM-dd') >= :endDate) order by id desc ")
	public List<Project> getProjectBySubsidiaryEndDate(Long subsidiaryId, String endDate);

	public List<Project> findBySubsidiaryIdAndIsDeleted(Long subsidiaryId, boolean isDeleted);

}
