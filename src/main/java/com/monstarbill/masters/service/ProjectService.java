package com.monstarbill.masters.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;

import com.monstarbill.masters.models.Project;
import com.monstarbill.masters.models.ProjectHistory;
import com.monstarbill.masters.payload.request.PaginationRequest;
import com.monstarbill.masters.payload.response.PaginationResponse;

public interface ProjectService {
	
	public Project save(Project project);
	
	public Project getProjectById(Long Id);
	
	public PaginationResponse findAll(PaginationRequest paginationRequest);
	
	public List<ProjectHistory> findHistoryById(Long id, Pageable pageable);
	
	public boolean deleteById(Long id);

	public Map<Long, String> getAllProjects();

	public Boolean getValidateName(String name);

	public List<Project> getProjectBySubsidiaryEndDate(Long subsidiaryId, String endDate);
	
	public List<Project> getProjectBySubsidiaryId(Long SubsidiaryId);


}
