package com.monstarbill.masters.controllers;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.monstarbill.masters.commons.CustomException;
import com.monstarbill.masters.models.CustomRoles;
import com.monstarbill.masters.models.RolesHistory;
import com.monstarbill.masters.payload.request.PaginationRequest;
import com.monstarbill.masters.payload.response.PaginationResponse;
import com.monstarbill.masters.service.RolesService;

import lombok.extern.slf4j.Slf4j;

/**
 * All WS's of the Role's and it's child components if any
 * @author Prashant
 * 14-07-2022
 */
//@CrossOrigin(origins= "*", allowedHeaders = "*", maxAge = 4800, allowCredentials = "false" )
@RestController
@RequestMapping("/roles")
@Slf4j
public class RolesController {

	@Autowired
	private RolesService rolesService;
	
	/**
	 * Save/update the Role	
	 * @param role
	 * @return
	 */
	@PostMapping("/save")
	public ResponseEntity<CustomRoles> saveSupplier(@Valid @RequestBody CustomRoles role) {
		log.info("Saving the Roles :: " + role.toString());
		role = rolesService.save(role);
		log.info("Roles saved successfully");
		return ResponseEntity.ok(role);
	}
	
	/**
	 * get tax-rate based on it's id
	 * @param id
	 * @return
	 */
	@GetMapping("/get")
	public ResponseEntity<CustomRoles> findById(@RequestParam Long id) {
		log.info("Get Role for ID :: " + id);
		CustomRoles customRole = rolesService.getRoleById(id);
		if (customRole == null) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		log.info("Returning from find by id Role.");
		return new ResponseEntity<>(customRole, HttpStatus.OK);
	}
	
	/**
	 * get list of Roles
	 * @return
	 */
	@PostMapping("/get/all")
	public ResponseEntity<PaginationResponse> findAll(@RequestBody PaginationRequest paginationRequest) {
		log.info("Get all Roles started.");
		PaginationResponse paginationResponse = new PaginationResponse();
		paginationResponse = rolesService.findAll(paginationRequest);
		log.info("Get all Roles completed.");
		return new ResponseEntity<>(paginationResponse, HttpStatus.OK);
	}
	
	/**
	 * Find history by role Id
	 * Supported for server side pagination
	 * @param id
	 * @param pageSize
	 * @param pageNumber
	 * @return
	 */
	@GetMapping("/get/history")
	public ResponseEntity<List<RolesHistory>> findHistoryById(@RequestParam Long id, @RequestParam(defaultValue = "10") int pageSize, @RequestParam(defaultValue = "0") int pageNumber, @RequestParam(defaultValue = "id") String sortColumn) {
		log.info("Get CustomRoles Audit for Role ID :: " + id);
		Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(sortColumn));
		List<RolesHistory> rolesHistoris = this.rolesService.findHistoryById(id, pageable);
		log.info("Returning from TaxRate Audit by id.");
		return new ResponseEntity<>(rolesHistoris, HttpStatus.OK);
	}
	
	/**
	 * get tax-rate based on it's id
	 * @param id
	 * @return
	 */
	@PostMapping("/get-by-ids")
	public ResponseEntity<List<CustomRoles>> findByRoleName(@RequestBody List<Long> roleIds) {
		log.info("Get Role by name started :: " + roleIds);
		List<CustomRoles> customRoles = rolesService.getRoleByIds(roleIds);
		log.info("Get Role by name finished :: " + roleIds);
		return new ResponseEntity<>(customRoles, HttpStatus.OK);
	}

	/**
	 * get only roles by subsidiaryId
	 * 
	 * @param subsidiaryId
	 * @return roles
	 */
	@GetMapping("/get-roles-by-subsidiary")
	public ResponseEntity<List<CustomRoles>> findBySubsidiary(@RequestParam Long subsidiaryId, @RequestParam List<String> accessType) {
		List<CustomRoles> customRoles = new ArrayList<CustomRoles>();
		try {
			customRoles = rolesService.findRolesBySubsidiaryId(subsidiaryId, accessType);
			log.info("Getting the roles  " + customRoles);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException("Exception while getting the roles by subsidiaryId :: " + e.toString());
		}
		return ResponseEntity.ok(customRoles);
	}
	
	@GetMapping("/get-roles-by-subsidiary-employee")
	public ResponseEntity<List<CustomRoles>> findBySubsidiaryForEmplyoee(@RequestParam Long subsidiaryId) {
		List<CustomRoles> customRoles = new ArrayList<CustomRoles>();
		try {
			customRoles = rolesService.findBySubsidiaryForEmplyoee(subsidiaryId);
			log.info("Getting the roles  " + customRoles);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException("Exception while getting the roles by subsidiaryId :: " + e.toString());
		}
		return ResponseEntity.ok(customRoles);
	}
}
