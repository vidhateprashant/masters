package com.monstarbill.masters.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.apache.commons.collections4.CollectionUtils;
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

import com.monstarbill.masters.commons.CustomException;
import com.monstarbill.masters.models.Employee;
import com.monstarbill.masters.models.EmployeeAddress;
import com.monstarbill.masters.models.EmployeeHistory;
import com.monstarbill.masters.payload.request.PaginationRequest;
import com.monstarbill.masters.payload.response.IdNameResponse;
import com.monstarbill.masters.payload.response.PaginationResponse;
import com.monstarbill.masters.service.EmployeeService;

import lombok.extern.slf4j.Slf4j;

//@CrossOrigin //(origins= "*", allowedHeaders = "*", maxAge = 4800, allowCredentials = "false" )
@RestController
@RequestMapping("/employee")
@Slf4j
public class EmployeeController {

	@Autowired
	private EmployeeService employeeService;
	
	/**
	 * Save/update employee
	 * @param 
	 * @return
	 */
	@PostMapping("/save")
	public ResponseEntity<Employee> saveEmployee(@Valid @RequestBody Employee employee) {
		log.info("Saving the Employee :: " + employee.toString());
		employee = employeeService.save(employee);
		log.info("Employee saved successfully");
		return ResponseEntity.ok(employee);
	}
	
	/**
	 * get all data of a single employee for view and update
	 * @param 
	 * @return
	 */
	@GetMapping("/get")
	public ResponseEntity<Employee> findById(@RequestParam Long id) {
		log.info("Get Employee for ID :: " + id);
		Employee employee = employeeService.getEmployeeById(id);
		if (employee == null) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		log.info("Returning from find by id employee");
		return new ResponseEntity<>(employee, HttpStatus.OK);
	}
	
	/**
	 * get all employees for the table with pagination
	 * @param 
	 * @return
	 */
	@PostMapping("/get/all")
	public ResponseEntity<PaginationResponse> findAll(@RequestBody PaginationRequest paginationRequest) {
		log.info("Get All employees started");
		PaginationResponse paginationResponse = new PaginationResponse();
		paginationResponse = employeeService.findAll(paginationRequest);
		log.info("Get All employees Finished");
		return new ResponseEntity<>(paginationResponse, HttpStatus.OK);
	}
	
	/**
	 * save the address of the employee
	 * @param employeeAddress
	 * @return
	 */
	@Deprecated
	@PostMapping("/address/save")
	public ResponseEntity<EmployeeAddress> saveEmployeeAddress(@Valid @RequestBody EmployeeAddress employeeAddress) {
		log.info("Saving the Employee Address :: " + employeeAddress.toString());
		// employeeAddress = employeeService.saveAddress(employeeAddress);
		log.info("Employee Address saved successfully");
		return ResponseEntity.ok(employeeAddress);
	}

	/**
	 * Get all the address against the employee
	 * @param employeeId
	 * @return
	 */
	@GetMapping("/address/get")
	public ResponseEntity<List<EmployeeAddress>> findAddressByEmployeeId(@RequestParam Long employeeId) {
		log.info("Get Address against the Employee ID :: " + employeeId);
		List<EmployeeAddress> employeeAddresses = employeeService.findAddressByEmployeeId(employeeId);
		if (CollectionUtils.isEmpty(employeeAddresses)) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<>(employeeAddresses, HttpStatus.OK);
	}

	/**
	 * Delete the address against the employee and id
	 * @param employeeAddress
	 * @return
	 */
	@Deprecated
	@PostMapping("/address/delete")
	public ResponseEntity<Boolean> deleteEmployeeAddress(@RequestBody EmployeeAddress employeeAddress) {
		log.info("Deleting the Address of the Employee ID :: " + employeeAddress.getEmployeeId());
		Boolean isDeleted = employeeService.deleteEmployeeAddress(employeeAddress);
		return new ResponseEntity<>(isDeleted, HttpStatus.OK);
	}
	
	/**
	 * Find history by employee Id
	 * Supported for server side pagination
	 * @param id
	 * @param pageSize
	 * @param pageNumber
	 * @return
	 */
	@GetMapping("/get/history")
	public ResponseEntity<List<EmployeeHistory>> findHistoryById(@RequestParam String employeeNumber, @RequestParam(defaultValue = "10") int pageSize, @RequestParam(defaultValue = "0") int pageNumber, @RequestParam(defaultValue = "id") String sortColumn) {
		log.info("Get Employee Audit for Employee ID :: " + employeeNumber);
		Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(sortColumn));
		List<EmployeeHistory> employeeHistoreis = employeeService.findAuditById(employeeNumber, pageable);
		log.info("Returning from Employee Audit by id.");
		return new ResponseEntity<>(employeeHistoreis, HttpStatus.OK);
	}
	
	@GetMapping("/is-valid-name")
	public ResponseEntity<Boolean> validateName(@RequestParam String name) {
		return new ResponseEntity<>(this.employeeService.getValidateName(name), HttpStatus.OK);
	}
	
	/**
	 * To get list(id, name only) to display in the Dropdown
	 * @return
	 */
	@GetMapping("/get/all/lov")
	public ResponseEntity<Map<Long, String>> getAllEmployeesForLov(@RequestParam Long subsidiaryId) {
		return new ResponseEntity<>(this.employeeService.getAllEmployees(subsidiaryId), HttpStatus.OK);
	}
	
	/**
	 * get employee details by subsidiary
	 * @param subsidiaryId
	 * @return account code
	 */
	@GetMapping("/get-employee-by-subsidiary")
	public ResponseEntity<List<Employee>> findByLocation(@RequestParam Long subsidiaryId ) {
		List<Employee> employees = new ArrayList<Employee>();
		try {
			employees = employeeService.findBySubsidiaryId(subsidiaryId);
			log.info("Getting the employee by subsidiary  " + employees);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException(
					"Exception while getting the  employee :: " + e.toString());
		}
		return ResponseEntity.ok(employees);
	}
	
	@GetMapping("/get-by-role-subsidiary")
	public ResponseEntity<List<IdNameResponse>> findByRoleId(@RequestParam Long roleId, @RequestParam Long subsidiaryId) {
		log.info("Get Employee by Role ID :: " + roleId);
		List<IdNameResponse> employees = employeeService.findByRoleId(roleId, subsidiaryId);
		log.info("Returning from find by Role employee");
		return new ResponseEntity<>(employees, HttpStatus.OK);
	}
	
	@GetMapping("/get-emp-id-by-mail")
	public ResponseEntity<Long> getEmployeeIdByAccessMail(@RequestParam String email) {
		log.info("Get Employee ID by email ID :: " + email);
		Long employeeId = this.employeeService.getEmployeeIdByAccessMail(email);
		log.info("Get Employee ID by email ID Finished.");
		return new ResponseEntity<>(employeeId, HttpStatus.OK);
	}
	
}
