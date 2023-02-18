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

import com.monstarbill.masters.models.Project;
import com.monstarbill.masters.models.ProjectHistory;
import com.monstarbill.masters.payload.request.PaginationRequest;
import com.monstarbill.masters.payload.response.PaginationResponse;
import com.monstarbill.masters.service.ProjectService;

import lombok.extern.slf4j.Slf4j;

/**
 * All WS's of the Project and it's child components 
 * @author Ajay
 * 15-07-2022
 */

@CrossOrigin(origins= "*", allowedHeaders = "*", maxAge = 4800, allowCredentials = "false" )

@RestController
@RequestMapping("/project")
@Slf4j
public class ProjectController {
	
	@Autowired
	private ProjectService projectService;	
	
	/**
	 * This saves the project
	 * @param project
	 * @return
	 */
	
	@PostMapping("/save")
	public ResponseEntity<Project> saveProject(@Valid @RequestBody Project project) {
		log.info("Saving the Project :: " + project.toString());
		project = projectService.save(project);
		log.info("Project saved successfully");
		return ResponseEntity.ok(project);
	}
	
	/**
	 * get Project based on it's id
	 * @param id
	 * @return Project
	 */
	
	@GetMapping("/get")
	public ResponseEntity<Project> getProject(@RequestParam Long id) {
		return new ResponseEntity<>(projectService.getProjectById(id), HttpStatus.OK);
	}
	
	/**
	 * get list of Projects with/without Filter 
	 * @return
	 */
	@PostMapping("/get/all")
	public ResponseEntity<PaginationResponse> findAll(@RequestBody PaginationRequest paginationRequest) {
		log.info("Get all Projects started.");
		PaginationResponse paginationResponse = new PaginationResponse();
		paginationResponse = projectService.findAll(paginationRequest);
		log.info("Get all Projects completed.");
		return new ResponseEntity<>(paginationResponse, HttpStatus.OK);
	}
	
	/**
	 * Find history by project Id
	 * Supported for server side pagination
	 * @param id
	 * @param pageSize
	 * @param pageNumber
	 * @return
	 */
	@GetMapping("/get/history")
	public ResponseEntity<List<ProjectHistory>> findHistoryById(@RequestParam Long id, @RequestParam(defaultValue = "10") int pageSize, @RequestParam(defaultValue = "0") int pageNumber, @RequestParam(defaultValue = "id") String sortColumn) {
		log.info("Get Project Audit  :: " + id);
		Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(sortColumn));
		List<ProjectHistory> projectHistoris = this.projectService.findHistoryById(id, pageable);
		log.info("Returning from Project Audit by id.");
		return new ResponseEntity<>(projectHistoris, HttpStatus.OK);
	}
	
	/**
	 * soft delete the project by it's id
	 * @param id
	 * @return
	 */
	@GetMapping("/delete")
	public ResponseEntity<Boolean> deleteById(@RequestParam Long id) {
		log.info("Delete Project by ID :: " + id);
		boolean isDeleted = false;
		isDeleted = projectService.deleteById(id);
		log.info("Delete Project by ID Completed.");
		return new ResponseEntity<>(isDeleted, HttpStatus.OK);
	}
	
	/**
	 * To get list(id, name only) to display in the Dropdown
	 * @return
	 */
	@GetMapping("/get/all/lov")
	public ResponseEntity<Map<Long, String>> getAllProjectsForLov() {
		return new ResponseEntity<>(this.projectService.getAllProjects(), HttpStatus.OK);
	}
	
	@GetMapping("/is-valid-name")
	public ResponseEntity<Boolean> validateName(@RequestParam String name) {
		return new ResponseEntity<>(this.projectService.getValidateName(name), HttpStatus.OK);
	}
	
	@GetMapping("/get-by-subsidiary-enddate")
	public ResponseEntity<List<Project>> getProjectBySubsidiaryEndDate(@RequestParam Long subsidiaryId, @RequestParam String endDate) {
		return new ResponseEntity<>(projectService.getProjectBySubsidiaryEndDate(subsidiaryId, endDate), HttpStatus.OK);
	}
	
	/**
	 * get Project based on subsidiary id
	 * 
	 * @param id
	 * @return Project
	 */
	@GetMapping("/get-by-subsidiary-id")
	public ResponseEntity<List<Project>> getProjectBySubsidiaryID(@RequestParam Long subsidiaryId) {
		return new ResponseEntity<>(projectService.getProjectBySubsidiaryId(subsidiaryId), HttpStatus.OK);
	}
}
