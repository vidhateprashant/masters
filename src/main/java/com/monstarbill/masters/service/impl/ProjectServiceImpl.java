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
import com.monstarbill.masters.dao.ProjectDao;
import com.monstarbill.masters.enums.Operation;
import com.monstarbill.masters.models.Project;
import com.monstarbill.masters.models.ProjectHistory;
import com.monstarbill.masters.payload.request.PaginationRequest;
import com.monstarbill.masters.payload.response.PaginationResponse;
import com.monstarbill.masters.repository.ProjectHistoryRepository;
import com.monstarbill.masters.repository.ProjectRepository;
import com.monstarbill.masters.service.ProjectService;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class ProjectServiceImpl implements ProjectService{
	
	@Autowired
	private ProjectRepository projectRepository;
	
	@Autowired
	private ProjectDao projectDao;
	
	@Autowired
	private ProjectHistoryRepository projectHistoryRepository;
	
	@Override
	public Project save(Project project) {
		Optional<Project> oldProject = Optional.ofNullable(null);
		
		if (project.getId() == null) {
			project.setCreatedBy(CommonUtils.getLoggedInUsername());
		} else {
			// Get the existing object using the deep copy
			oldProject = this.projectRepository.findByIdAndIsDeleted(project.getId(), false);
			if (oldProject.isPresent()) {
				try {
					oldProject = Optional.ofNullable((Project) oldProject.get().clone());
				} catch (CloneNotSupportedException e) {
					log.error("Error while Cloning the object. Please contact administrator.");
					throw new CustomException("Error while Cloning the object. Please contact administrator.");
				}
			}
		}

		project.setLastModifiedBy(CommonUtils.getLoggedInUsername());
		try {
			project = this.projectRepository.save(project);
		} catch (DataIntegrityViolationException e) {
			log.error(" Project unique constrain violetd." + e.getMostSpecificCause());
			throw new CustomException("Project unique constrain violetd :" + e.getMostSpecificCause());
		}
		
		if (project == null) {
			log.info("Error while saving the Project.");
			throw new CustomMessageException("Error while saving the Project.");
		}
		
		// update the data in project history table
		this.updateProjectHistory(project, oldProject);
		
		return project;
	}
	
	/**
	 * This method save the data in history table
	 * Add entry as a Insert if Project is new 
	 * Add entry as a Update if Project is exists
	 * @param Project
	 * @param oldProject
	 */
	private void updateProjectHistory(Project project, Optional<Project> oldProject) {
		if (oldProject.isPresent()) {
			// insert the updated fields in history table
			List<ProjectHistory> projectHistories = new ArrayList<ProjectHistory>();
			try {
				projectHistories = oldProject.get().compareFields(project);
				if (CollectionUtils.isNotEmpty(projectHistories)) {
					this.projectHistoryRepository.saveAll(projectHistories);
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				log.error("Error while comparing the new and old objects. Please contact administrator.");
				throw new CustomException("Error while comparing the new and old objects. Please contact administrator.");
			}
			log.info("Project History is updated successfully");
		} else {
			// Insert in history table as Operation - INSERT 
			this.projectHistoryRepository.save(this.prepareProjectHistory(project.getId(), null, AppConstants.PROJECT, Operation.CREATE.toString(), project.getLastModifiedBy(), null, String.valueOf(project.getId())));
		}
	}

	/**
	 * Prepares the history for the Project
	 * @param projectId
	 * @param moduleName
	 * @param operation
	 * @param lastModifiedBy
	 * @param oldValue
	 * @param newValue
	 * @return
	 */
	public ProjectHistory prepareProjectHistory(Long projectId, Long childId, String moduleName, String operation, String lastModifiedBy, String oldValue, String newValue) {
		ProjectHistory projectHistory = new ProjectHistory();
		projectHistory.setProjectId(projectId);
		projectHistory.setChildId(childId);
		projectHistory.setModuleName(moduleName);
		projectHistory.setChangeType(AppConstants.UI);
		projectHistory.setOperation(operation);
		projectHistory.setOldValue(oldValue);
		projectHistory.setNewValue(newValue);
		projectHistory.setLastModifiedBy(lastModifiedBy);
		return projectHistory;
	}
	
	@Override
	public List<ProjectHistory> findHistoryById(Long id, Pageable pageable) {
		List<ProjectHistory> histories = this.projectHistoryRepository.findByProjectIdOrderById(id, pageable);
		String createdBy = histories.get(0).getLastModifiedBy();
		histories.forEach(e->{
			e.setCreatedBy(createdBy);
		});
		return histories;
	}
	
	@Override
	public Project getProjectById(Long id) {
		Optional<Project> project = Optional.ofNullable(new Project());
		project = this.projectRepository.findByIdAndIsDeleted(id, false);
		if (!project.isPresent()) {
			log.info("Project is not found given id : " + id);
			throw new CustomMessageException("Project is not found given id : " + id);
		}

		return project.get();
	}
	
	@Override
	public PaginationResponse findAll(PaginationRequest paginationRequest) { 
		
		List<Project> projects = new ArrayList<Project>();
		
		// preparing where clause
		String whereClause = this.prepareWhereClause(paginationRequest).toString();
		
		// get list
		projects = this.projectDao.findAll(whereClause, paginationRequest);
		
		// getting count
		Long totalRecords = this.projectDao.getCount(whereClause);
		
		return CommonUtils.setPaginationResponse(paginationRequest.getPageNumber(), paginationRequest.getPageSize(), projects, totalRecords);
	}

	private StringBuilder prepareWhereClause(PaginationRequest paginationRequest) {
		Long subsidiaryId = null;
		String projectId = null;
		String projectName = null;
		String startDate = null;
		
		Map<String, ?> filters = paginationRequest.getFilters();
		
		if (filters.containsKey(FilterNames.SUBSIDIARY_ID))
			subsidiaryId = ((Number) filters.get(FilterNames.SUBSIDIARY_ID)).longValue();
		if (filters.containsKey(FilterNames.PROJECT_ID))
			projectId = (String) filters.get(FilterNames.PROJECT_ID);
		if (filters.containsKey(FilterNames.NAME))
			projectName = (String) filters.get(FilterNames.NAME);
		if (filters.containsKey(FilterNames.SCHEDULING_START_DATE)) 
			startDate = (String) filters.get(FilterNames.SCHEDULING_START_DATE);
		
		StringBuilder whereClause = new StringBuilder(" AND p.isDeleted is false ");
		
		if (subsidiaryId != null && subsidiaryId != 0) {
			whereClause.append(" AND p.subsidiaryId = ").append(subsidiaryId);
		}
		if (StringUtils.isNotEmpty(projectId)) {
			whereClause.append(" AND lower(p.projectId) like lower ('%").append(projectId).append("%')");
		}
		if (StringUtils.isNotEmpty(projectName)) {
			whereClause.append(" AND lower(p.name) like lower ('%").append(projectName).append("%')");
		}
		if (startDate != null) {
			whereClause.append(" AND to_char(p.schedulingStartDate, 'yyyy-MM-dd') like '%").append(startDate).append("%'");
		}

		return whereClause;
	}
	
	@Override
	public boolean deleteById(Long id) {
		Project project = new Project();
		project = this.getProjectById(id);
		project.setDeleted(true);
		project = this.projectRepository.save(project);
		
		if (project == null) {
			log.error("Error while deleting the project : " + id);
			throw new CustomMessageException("Error while deleting the Project : " + id);
		}
		// update the operation in the history
		this.projectHistoryRepository.save(this.prepareProjectHistory(project.getId(), null, AppConstants.PROJECT, Operation.DELETE.toString(), project.getLastModifiedBy(), String.valueOf(project.getId()), null));

	    return true;
	}

	@Override
	public Map<Long, String> getAllProjects() {
		return this.projectRepository.findIdAndNameMap(false);
	}
	
	@Override
	public Boolean getValidateName(String name) {
		// if name is empty then name is not valid
		if (StringUtils.isEmpty(name)) return false;
		
		Long countOfRecordsWithSameName = this.projectRepository.getCountByName(name.trim());
		// if we we found the count greater than 0 then it is not valid. If it is zero then it is valid string
		if (countOfRecordsWithSameName > 0) return false; else return true;
	}

	@Override
	public List<Project> getProjectBySubsidiaryEndDate(Long subsidiaryId, String endDate) {
		return this.projectRepository.getProjectBySubsidiaryEndDate(subsidiaryId, endDate);
	}
	
	@Override
	public List<Project> getProjectBySubsidiaryId(Long SubsidiaryId) {
		List<Project> projects = new ArrayList<Project>();
		projects = this.projectRepository.findBySubsidiaryIdAndIsDeleted(SubsidiaryId, false);
		if (projects.isEmpty()) {
			log.info("Project is not found given SubsidiaryId : " + SubsidiaryId);
			throw new CustomMessageException("Project is not found given SubsidiaryId : " + SubsidiaryId);
		}

		return projects;
	}
}
