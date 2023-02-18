package com.monstarbill.masters.service.impl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monstarbill.masters.commons.AppConstants;
import com.monstarbill.masters.commons.CommonUtils;
import com.monstarbill.masters.commons.CustomException;
import com.monstarbill.masters.commons.CustomMessageException;
import com.monstarbill.masters.commons.FilterNames;
import com.monstarbill.masters.dao.EmployeeDao;
import com.monstarbill.masters.enums.Operation;
import com.monstarbill.masters.enums.Status;
import com.monstarbill.masters.feignclient.SetupServiceClient;
import com.monstarbill.masters.models.Employee;
import com.monstarbill.masters.models.EmployeeAccess;
import com.monstarbill.masters.models.EmployeeAccounting;
import com.monstarbill.masters.models.EmployeeAddress;
import com.monstarbill.masters.models.EmployeeContact;
import com.monstarbill.masters.models.EmployeeHistory;
import com.monstarbill.masters.models.EmployeeRole;
import com.monstarbill.masters.payload.request.PaginationRequest;
import com.monstarbill.masters.payload.response.IdNameResponse;
import com.monstarbill.masters.payload.response.PaginationResponse;
import com.monstarbill.masters.payload.response.UserValidationRequest;
import com.monstarbill.masters.repository.EmployeeAccessRepository;
import com.monstarbill.masters.repository.EmployeeAccountingRepository;
import com.monstarbill.masters.repository.EmployeeAddressRepository;
import com.monstarbill.masters.repository.EmployeeContactRepository;
import com.monstarbill.masters.repository.EmployeeHistoryRepository;
import com.monstarbill.masters.repository.EmployeeRepository;
import com.monstarbill.masters.repository.EmployeeRoleRepository;
import com.monstarbill.masters.service.EmployeeService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional
public class EmployeeServiceImpl implements EmployeeService {

	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private EmployeeContactRepository employeeContactRepository;

	@Autowired
	private EmployeeAccountingRepository employeeAccountingRepository;

	@Autowired
	private EmployeeAddressRepository employeeAddressRepository;

	@Autowired
	private EmployeeAccessRepository employeeAccessRepository;

	@Autowired
	private EmployeeHistoryRepository employeeHistoryRepository;

	@Autowired
	private EmployeeRoleRepository employeeRoleRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private EmployeeDao employeeDao;
	
	@Autowired
	private SetupServiceClient setupServiceClient;

	/**
	 * steps to save employee 
	 * 1. employee 
	 * 2. Contact 
	 * 3. Subsidiary - through the Add/remove button [separate API] 
	 * 4. Accounting 
	 * 5. Access 
	 * 6. Address - through save button
	 */
	@Override
	public Employee save(Employee employee) {
		Long employeeId = null;
		String employeeNumber = null;
		
		Optional<Employee> oldEmployee = Optional.empty();
		String username = CommonUtils.getLoggedInUsername();

		try {
			// 1. save the employee
			if (employee.getId() == null) {
				employeeNumber = this.setupServiceClient.getPreferenceNumber(employee.getSubsidiaryId(), "Employee Master");
				if (StringUtils.isEmpty(employeeNumber)) {
					throw new CustomException("Employee number is not generated. Please check the configurations.");
				}
				employee.setEmployeeNumber(employeeNumber);
				employee.setCreatedBy(username);
			} else {
				// Get the existing object using the deep copy
				oldEmployee = this.employeeRepository.findByIdAndIsDeleted(employee.getId(), false);
				if (oldEmployee.isPresent()) {
					try {
						oldEmployee = Optional.ofNullable((Employee) oldEmployee.get().clone());
					} catch (CloneNotSupportedException e) {
						log.error("Error while Cloning the object. Please contact administrator.");
						throw new CustomException("Error while Cloning the object. Please contact administrator.");
					}
				}
			}
			employee.setLastModifiedBy(username);
			String firstName = employee.getFirstName();
			String middleName = employee.getMiddleName();
			String lastName = employee.getLastName();
			
			String fullName = "";
			if (StringUtils.isNotEmpty(firstName)) fullName += firstName;
			if (StringUtils.isNotEmpty(middleName)) fullName += " " + middleName;
			if (StringUtils.isNotEmpty(lastName)) fullName += " " + lastName;
			employee.setFullName(fullName);
			
			Employee savedEmployee;
			try {
				savedEmployee = this.employeeRepository.save(employee);
			} catch (DataIntegrityViolationException e) {
				log.error("Employee unique constrain violetd." + e.getMostSpecificCause());
				throw new CustomException("Employee unique constrain violetd :" + e.getMostSpecificCause());
			}
			employeeId = savedEmployee.getId();
			employeeNumber = savedEmployee.getEmployeeNumber();
			log.info("Employee is saved.");

			this.updateEmployeeHistory(oldEmployee, savedEmployee);
			log.info("Employee History is saved.");
			// ----------------------------------- employee Finished -------------------------------------------------

			// ----------------------------------- 01. employee contact started -------------------------------------------------
			log.info("Save employee contact started...");
			// Save the contact
			EmployeeContact employeeContact = employee.getEmployeeContact();
			if (employeeContact != null) {
				this.saveEmployeeContact(employeeId, employeeNumber, employeeContact);
				savedEmployee.setEmployeeContact(employeeContact);
			}
			log.info("Save employee contact Finished...");
			// ----------------------------------- 01. employee contact Finished -------------------------------------------------

			// ----------------------------------- 02. employee Address Started-------------------------------------------------
			log.info("Save employee Address Started...");
			List<EmployeeAddress> employeeAddresses = employee.getEmployeeAddresses();
			if (CollectionUtils.isNotEmpty(employeeAddresses)) {
				for (EmployeeAddress employeeAddress : employeeAddresses) {
					this.saveAddress(employeeId, employeeNumber, employeeAddress);
				}
				savedEmployee.setEmployeeAddresses(employeeAddresses);
			}
			log.info("Save employee Address Finished...");
			// ----------------------------------- 02. employee Address Finished -------------------------------------------------

			// ----------------------------------- 03. employee Accounting Started-------------------------------------------------
			log.info("Save employee Accounting Started...");
			EmployeeAccounting employeeAccounting = employee.getEmployeeAccounting();
			if (employeeAccounting != null) {
				this.saveEmployeeAccounting(employeeAccounting, employeeId, employeeNumber);
				savedEmployee.setEmployeeAccounting(employeeAccounting);
			}
			log.info("Save employee Accounting Finished...");
			// ----------------------------------- 03. employee Accounting Finished -------------------------------------------------
			
			// ----------------------------------- 04. employee Access Started-------------------------------------------------
			log.info("Save employee Access Started...");
			EmployeeAccess employeeAccess = employee.getEmployeeAccess();
			if (employeeAccess != null) {
				this.saveEmployeeAccess(employeeAccess, employeeId, employeeNumber);
				savedEmployee.setEmployeeAccess(employeeAccess);
			}
			log.info("Save employee Access Finished...");
			// ----------------------------------- 04. employee Access Finished -------------------------------------------------

			System.gc();
			// savedEmployee = this.getEmployeeById(savedEmployee.getId());
			return savedEmployee;
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException(e.getLocalizedMessage());
		}
	}

	// save the employee accounting
	private void saveEmployeeAccounting(EmployeeAccounting employeeAccounting, Long employeeId, String employeeNumber) {
		Optional<EmployeeAccounting> oldEmployeeAccounting = Optional.empty();

		if (employeeAccounting.getId() == null) {
			employeeAccounting.setCreatedBy(CommonUtils.getLoggedInUsername());
		} else {
			// Get the existing object using the deep copy
			oldEmployeeAccounting = this.employeeAccountingRepository.findByIdAndIsDeleted(employeeAccounting.getId(),
					false);
			if (oldEmployeeAccounting.isPresent()) {
				try {
					oldEmployeeAccounting = Optional
							.ofNullable((EmployeeAccounting) oldEmployeeAccounting.get().clone());
				} catch (CloneNotSupportedException e) {
					log.error("Error while Cloning the object. Please contact administrator.");
					throw new CustomException("Error while Cloning the object. Please contact administrator.");
				}
			}
		}
		employeeAccounting.setEmployeeId(employeeId);
		employeeAccounting.setEmployeeNumber(employeeNumber);
		employeeAccounting.setLastModifiedBy(CommonUtils.getLoggedInUsername());
		EmployeeAccounting employeeAccountingSaved = employeeAccountingRepository.save(employeeAccounting);
		log.info("Employee Accounting is saved.");
		this.updateEmployeeAccountingHistory(oldEmployeeAccounting, employeeAccountingSaved);
		log.info("Employee Accounting History is saved.");
	}

	// update the history of employee access
	private void updateEmployeeAccountingHistory(Optional<EmployeeAccounting> oldEmployeeAccounting,
			EmployeeAccounting employeeAccounting) {
		if (employeeAccounting != null) {
			if (oldEmployeeAccounting.isPresent()) {
				// insert the updated fields in history table
				List<EmployeeHistory> employeeHistories = new ArrayList<EmployeeHistory>();
				try {
					employeeHistories = oldEmployeeAccounting.get().compareFields(employeeAccounting);
					if (CollectionUtils.isNotEmpty(employeeHistories)) {
						this.employeeHistoryRepository.saveAll(employeeHistories);
					}
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					log.error("Error while comparing the new and old objects. Please contact administrator.");
					throw new CustomException(
							"Error while comparing the new and old objects. Please contact administrator.");
				}
				log.info("Employee Accounting History is updated successfully");
			} else {
				// Insert in history table as Operation - INSERT
				this.employeeHistoryRepository.save(this.prepareEmployeeHistory(employeeAccounting.getEmployeeNumber(),
						employeeAccounting.getId(), AppConstants.EMPLOYEE_ACCOUNTING, Operation.CREATE.toString(),
						employeeAccounting.getLastModifiedBy(), null, String.valueOf(employeeAccounting.getId())));
				log.info("Employee Accounting History is inserted successfully");
			}
		} else {
			log.error("Error while saving the employee Accounting.");
			throw new CustomException("Error while saving the employee Accounting.");
		}
	}

	// save the employee Access
	private void saveEmployeeAccess(EmployeeAccess employeeAccess, Long employeeId, String employeeNumber) {
		Optional<EmployeeAccess> oldEmployeeAccess = Optional.empty();
		boolean isNewRecord = true;
		
		if (employeeAccess.getId() == null) {
			employeeAccess.setCreatedBy(CommonUtils.getLoggedInUsername());
		} else {
			isNewRecord = false;
			// Get the existing object using the deep copy
			oldEmployeeAccess = this.employeeAccessRepository.findByIdAndIsDeleted(employeeAccess.getId(), false);
			if (oldEmployeeAccess.isPresent()) {
				try {
					oldEmployeeAccess = Optional.ofNullable((EmployeeAccess) oldEmployeeAccess.get().clone());
				} catch (CloneNotSupportedException e) {
					log.error("Error while Cloning the object. Please contact administrator.");
					throw new CustomException("Error while Cloning the object. Please contact administrator.");
				}
			}
		}
		
		employeeAccess.setEmployeeId(employeeId);
		employeeAccess.setEmployeeNumber(employeeNumber);
		employeeAccess.setLastModifiedBy(CommonUtils.getLoggedInUsername());
		
		if (employeeAccess.isAccess()) {
			if (StringUtils.isEmpty(employeeAccess.getAccessMail())) {
				log.error("Employee Access Mail should not be empty.");
				throw new CustomMessageException("Employee Access Mail should not be empty.");
			}
			
			String password = employeeAccess.getPassword();
			if (StringUtils.isEmpty(password)) {
				password = employeeAccessRepository.getPasswordById(employeeId);
				if (StringUtils.isEmpty(password)) {
					log.error("Password cannot be empty.");
					throw new CustomMessageException("Password cannot be empty");
				}
				employeeAccess.setPlainPassword(password);
			} else {
				employeeAccess.setPlainPassword(password);
			}
			
			employeeAccess.setPassword(passwordEncoder.encode(password));
			
			if (CollectionUtils.isNotEmpty(employeeAccess.getEmployeeRoles())) {
				Set<Long> roles = employeeAccess.getEmployeeRoles().stream().filter(e->e.isDeleted() == false).map(e -> e.getRoleId()).collect(Collectors.toSet());
				UserValidationRequest user = new UserValidationRequest(isNewRecord, employeeAccess.getAccessMail(), password, new ArrayList<Long>(roles));
				this.setupServiceClient.saveUserCredentials(user);
			}
			log.info("user with roles is saved.");
		} else {
			//delete from user credentials
			if (!StringUtils.isEmpty(employeeAccess.getAccessMail())) {
				this.setupServiceClient.deleteUserCredentials(employeeAccess.getAccessMail());				
			}
			
			employeeAccess.setAccessMail(null);
			employeeAccess.setPassword(null);
			employeeAccess.setPlainPassword(null);
			if (CollectionUtils.isNotEmpty(employeeAccess.getEmployeeRoles())) {
				employeeAccess.getEmployeeRoles().forEach((role)->{
					role.setDeleted(true);
				});				
			}
		}
		EmployeeAccess employeeAccessSaved = employeeAccessRepository.save(employeeAccess);
		log.info("Employee Access is saved");
		this.updateEmployeeAccessHistory(oldEmployeeAccess, employeeAccessSaved);
		log.info("Employee Access History is saved");

		// save the roles
		this.saveEmployeeRole(employeeAccess, employeeId, employeeNumber);
		log.info("Employee Role is saved.");
	}

// save the employee roles
	private void saveEmployeeRole(EmployeeAccess employeeAccess, Long employeeId, String employeeNumber) {
		List<EmployeeRole> employeeRoles = employeeAccess.getEmployeeRoles();
		if (CollectionUtils.isNotEmpty(employeeRoles)) {
			Optional<EmployeeRole> oldEmployeeRole = Optional.empty();

			for (EmployeeRole employeeRole : employeeRoles) {
				oldEmployeeRole = Optional.empty();

				if (employeeRole.getId() == null) {
					employeeRole.setCreatedBy(CommonUtils.getLoggedInUsername());
				} else {
					// Get the existing object using the deep copy
					oldEmployeeRole = this.employeeRoleRepository.findByIdAndIsDeleted(employeeRole.getId(), false);
					if (oldEmployeeRole.isPresent()) {
						try {
							oldEmployeeRole = Optional.ofNullable((EmployeeRole) oldEmployeeRole.get().clone());
						} catch (CloneNotSupportedException e) {
							log.error("Error while Cloning the object. Please contact administrator.");
							throw new CustomException("Error while Cloning the object. Please contact administrator.");
						}
					}
				}
				employeeRole.setEmployeeId(employeeId);
				employeeRole.setEmployeeNumber(employeeNumber);
				employeeRole.setLastModifiedBy(CommonUtils.getLoggedInUsername());
				EmployeeRole employeeRoleSaved = this.employeeRoleRepository.save(employeeRole);
				log.info("Employee Role is saved.");
				this.updateEmployeeRoleHistory(oldEmployeeRole, employeeRoleSaved);
				log.info("Employee Role History is saved.");
			}
		}
	}

	// update the history of employee access
	private void updateEmployeeRoleHistory(Optional<EmployeeRole> oldEmployeeRole, EmployeeRole employeeRole) {
		if (employeeRole != null) {
			if (oldEmployeeRole.isPresent()) {
				if (employeeRole.isDeleted()) {
					// insert the history as DELETE
					this.employeeHistoryRepository.save(this.prepareEmployeeHistory(employeeRole.getEmployeeNumber(),
							employeeRole.getId(), AppConstants.EMPLOYEE_ROLE, Operation.DELETE.toString(),
							employeeRole.getLastModifiedBy(), String.valueOf(employeeRole.getId()), null));
				} else {
					// insert the updated fields in history table
					List<EmployeeHistory> employeeHistories = new ArrayList<EmployeeHistory>();
					try {
						employeeHistories = oldEmployeeRole.get().compareFields(employeeRole);
						if (CollectionUtils.isNotEmpty(employeeHistories)) {
							this.employeeHistoryRepository.saveAll(employeeHistories);
						}
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						log.error("Error while comparing the new and old objects. Please contact administrator.");
						throw new CustomException(
								"Error while comparing the new and old objects. Please contact administrator.");
					}
					log.info("Employee Access History is updated successfully");
				}
			} else {
				// Insert in history table as Operation - INSERT
				this.employeeHistoryRepository.save(this.prepareEmployeeHistory(employeeRole.getEmployeeNumber(),
						employeeRole.getId(), AppConstants.EMPLOYEE_ROLE, Operation.CREATE.toString(),
						employeeRole.getLastModifiedBy(), null, employeeRole.getRoleName()));
				log.info("Employee Access History is inserted successfully");
			}
		} else {
			log.error("Error while saving the employee Roles.");
			throw new CustomException("Error while saving the employee Roles.");
		}
	}

// update the history of employee access
	private void updateEmployeeAccessHistory(Optional<EmployeeAccess> oldEmployeeAccess,
			EmployeeAccess employeeAccess) {
		if (employeeAccess != null) {
			if (oldEmployeeAccess.isPresent()) {
				// insert the updated fields in history table
				List<EmployeeHistory> employeeHistories = new ArrayList<EmployeeHistory>();
				try {
					employeeHistories = oldEmployeeAccess.get().compareFields(employeeAccess);
					if (CollectionUtils.isNotEmpty(employeeHistories)) {
						this.employeeHistoryRepository.saveAll(employeeHistories);
					}
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					log.error("Error while comparing the new and old objects. Please contact administrator.");
					throw new CustomException(
							"Error while comparing the new and old objects. Please contact administrator.");
				}
				log.info("Employee Access History is updated successfully");
			} else {
				// Insert in history table as Operation - INSERT
				this.employeeHistoryRepository.save(this.prepareEmployeeHistory(employeeAccess.getEmployeeNumber(),
						employeeAccess.getId(), AppConstants.EMPLOYEE_ACCESS, Operation.CREATE.toString(),
						employeeAccess.getLastModifiedBy(), null, String.valueOf(employeeAccess.getId())));
				log.info("Employee Access History is inserted successfully");
			}
		} else {
			log.error("Error while saving the employee Access.");
			throw new CustomException("Error while saving the employee Access.");
		}
	}

	// save the employee contact
	private void saveEmployeeContact(Long employeeId, String employeeNumber, EmployeeContact employeeContact) {
		Optional<EmployeeContact> oldEmployeeContact = Optional.empty();

		if (employeeContact.getId() == null) {
			employeeContact.setCreatedBy(CommonUtils.getLoggedInUsername());
		} else {
			// Get the existing object using the deep copy
			oldEmployeeContact = this.employeeContactRepository.findByIdAndIsDeleted(employeeContact.getId(), false);
			if (oldEmployeeContact.isPresent()) {
				try {
					oldEmployeeContact = Optional.ofNullable((EmployeeContact) oldEmployeeContact.get().clone());
				} catch (CloneNotSupportedException e) {
					log.error("Error while Cloning the object. Please contact administrator.");
					throw new CustomException("Error while Cloning the object. Please contact administrator.");
				}
			}
		}

		employeeContact.setEmployeeId(employeeId);
		employeeContact.setEmployeeNumber(employeeNumber);
		employeeContact.setLastModifiedBy(CommonUtils.getLoggedInUsername());
		EmployeeContact employeeContactSaved = employeeContactRepository.save(employeeContact);
		log.info("Emplyoee contact saved.");
		this.updateEmployeeContactHistory(oldEmployeeContact, employeeContactSaved);
		log.info("Emplyoee contact History saved.");
	}

	// 	update history of the employee Contact
	private void updateEmployeeContactHistory(Optional<EmployeeContact> oldEmployeeContact,
			EmployeeContact employeeContact) {
		if (employeeContact != null) {
			if (oldEmployeeContact.isPresent()) {
				if (employeeContact.isDeleted()) {
					// Insert in history table as Operation - DELETE
					this.employeeHistoryRepository.save(this.prepareEmployeeHistory(employeeContact.getEmployeeNumber(),
							employeeContact.getId(), AppConstants.EMPLOYEE_CONTACT, Operation.DELETE.toString(),
							employeeContact.getLastModifiedBy(), String.valueOf(employeeContact.getId()), null));
				} else {
					// insert the updated fields in history table
					List<EmployeeHistory> employeeHistories = new ArrayList<EmployeeHistory>();
					try {
						employeeHistories = oldEmployeeContact.get().compareFields(employeeContact);
						if (CollectionUtils.isNotEmpty(employeeHistories)) {
							this.employeeHistoryRepository.saveAll(employeeHistories);
						}
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						log.error("Error while comparing the new and old objects. Please contact administrator.");
						throw new CustomException(
								"Error while comparing the new and old objects. Please contact administrator.");
					}
				}
				log.info("Employee Contact History is updated successfully");
			} else {
				// Insert in history table as Operation - INSERT
				this.employeeHistoryRepository.save(this.prepareEmployeeHistory(employeeContact.getEmployeeNumber(),
						employeeContact.getId(), AppConstants.EMPLOYEE_CONTACT, Operation.CREATE.toString(),
						employeeContact.getLastModifiedBy(), null, String.valueOf(employeeContact.getId())));
				
				log.info("Employee Contact History is inserted successfully");
			}
		} else {
			log.error("Error while saving the employee Contact.");
			throw new CustomException("Error while saving the employee Contact..");
		}
	}

	// update history of employee
	private void updateEmployeeHistory(Optional<Employee> oldEmployee, Employee employee) {
		if (employee != null) {
			if (oldEmployee.isPresent()) {
				// insert the updated fields in history table
				List<EmployeeHistory> employeeHistories = new ArrayList<EmployeeHistory>();
				try {
					employeeHistories = oldEmployee.get().compareFields(employee);
					if (CollectionUtils.isNotEmpty(employeeHistories)) {
						this.employeeHistoryRepository.saveAll(employeeHistories);
					}
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					log.error("Error while comparing the new and old objects. Please contact administrator.");
					throw new CustomException(
							"Error while comparing the new and old objects. Please contact administrator.");
				}
				log.info("Employee History is updated successfully");
			} else {
				// Insert in history table as Operation - INSERT
				this.employeeHistoryRepository.save(this.prepareEmployeeHistory(employee.getEmployeeNumber(), null,
						AppConstants.EMPLOYEE, Operation.CREATE.toString(), employee.getLastModifiedBy(), null, null));
				log.info("Employee History Inserted successfully.");
			}
		} else {
			log.error("Error while saving the employee.");
			throw new CustomException("Error while saving the employee.");
		}
	}

	@Override
	public Employee getEmployeeById(Long id) {
		Optional<Employee> employee = Optional.empty();
		
		employee = employeeRepository.findByIdAndIsDeleted(id, false);
		if (employee.isPresent()) {
			Long employeeId = employee.get().getId();
			log.info("Employee is found");
			
			// get the contacts
			Optional<EmployeeContact> employeeContact = this.employeeContactRepository
					.findByEmployeeIdAndIsDeleted(employeeId, false);
			if (employeeContact.isPresent()) {
				employee.get().setEmployeeContact(employeeContact.get());
			}
			log.info("Employee Contact is found");

			// 2. Get Address
			List<EmployeeAddress> employeeAddresses = this.employeeAddressRepository.findByEmployeeIdAndIsDeleted(employeeId,
					false);
			if (CollectionUtils.isNotEmpty(employeeAddresses)) {
				employee.get().setEmployeeAddresses(employeeAddresses);
			}
			log.info("Employee Addresses is found");
			
			// 3. Get Access & it's Roles
			Optional<EmployeeAccess> employeeAccess = Optional.empty();
			employeeAccess = this.employeeAccessRepository.findByEmployeeIdAndIsDeleted(employeeId, false);
			if (employeeAccess.isPresent()) {
				// Password not sending on the UI side - as per requirement by Snehasis - 29-10-2022
//				employeeAccess.get().setPassword(null);
//				employeeAccess.get().setPlainPassword(null);
				List<EmployeeRole> employeeRoles = this.employeeRoleRepository.findByEmployeeIdAndIsDeleted(employeeId,
						false);
				if (CollectionUtils.isNotEmpty(employeeRoles)) {
					employeeAccess.get().setEmployeeRoles(employeeRoles);
				}
				employee.get().setEmployeeAccess(employeeAccess.get());
			}
			log.info("Employee Addresses is found");

			// 4. Get Accounting
			Optional<EmployeeAccounting> employeeAccounting = this.employeeAccountingRepository
					.findByEmployeeIdAndIsDeleted(employeeId, false);
			if (employeeAccounting.isPresent()) {
				employee.get().setEmployeeAccounting(employeeAccounting.get());
			}
			log.info("Employee Accounting is found");
		} else {
			log.error("Employee Not Found against given employee id : " + id);
			throw new CustomMessageException("Employee Not Found against given employee id : " + id);
		}

		log.info("Employee is prepared and sending response.");
		return employee.get();
	}

	public EmployeeAddress saveAddress(Long employeeId, String employeeNumber, EmployeeAddress employeeAddress) {
		Optional<EmployeeAddress> oldEmployeeAddress = Optional.empty();

		if (employeeAddress.getId() == null) {
			employeeAddress.setCreatedBy(CommonUtils.getLoggedInUsername());
		} else {
			// Get existing address using deep copy
			oldEmployeeAddress = this.employeeAddressRepository.findByIdAndIsDeleted(employeeAddress.getId(), false);
			if (oldEmployeeAddress.isPresent()) {
				try {
					oldEmployeeAddress = Optional.ofNullable((EmployeeAddress) oldEmployeeAddress.get().clone());
				} catch (CloneNotSupportedException e) {
					log.error("Error while Cloning the object. Please contact administrator.");
					throw new CustomException("Error while Cloning the object. Please contact administrator.");
				}
			}
		}
		employeeAddress.setEmployeeId(employeeId);
		employeeAddress.setEmployeeNumber(employeeNumber);
		employeeAddress.setLastModifiedBy(CommonUtils.getLoggedInUsername());
		employeeAddress = this.employeeAddressRepository.save(employeeAddress);

		if (employeeAddress == null) {
			log.info("Error while Saving the Address in employee.");
			throw new CustomMessageException("Error while Saving the Address in employee.");
		}
		
		// find the updated values and save to history table
		if (oldEmployeeAddress.isPresent()) {
			if (employeeAddress.isDeleted()) {
				this.employeeHistoryRepository.save(this.prepareEmployeeHistory(employeeAddress.getEmployeeNumber(),
						employeeAddress.getId(), AppConstants.EMPLOYEE_ADDRESS, Operation.DELETE.toString(),
						employeeAddress.getLastModifiedBy(), String.valueOf(employeeAddress.getId()), null));
			} else {
				List<EmployeeHistory> employeeHistories = new ArrayList<EmployeeHistory>();
				try {
					employeeHistories = oldEmployeeAddress.get().compareFields(employeeAddress);
					if (CollectionUtils.isNotEmpty(employeeHistories)) {
						this.employeeHistoryRepository.saveAll(employeeHistories);
					}
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					log.error("Error while comparing the new and old objects. Please contact administrator.");
					throw new CustomException(
							"Error while comparing the new and old objects. Please contact administrator.");
				}
			}
			log.info("Employee Address History is updated successfully.");
		} else {
			this.employeeHistoryRepository.save(this.prepareEmployeeHistory(employeeAddress.getEmployeeNumber(),
					employeeAddress.getId(), AppConstants.EMPLOYEE_ADDRESS, Operation.CREATE.toString(),
					employeeAddress.getLastModifiedBy(), null, String.valueOf(employeeAddress.getId())));
			
			log.info("Employee Address History is inserted successfully.");
		}
		
		return employeeAddress;
	}

	@Override
	public List<EmployeeAddress> findAddressByEmployeeId(Long employeeId) {
		List<EmployeeAddress> employeeAddresses = new ArrayList<EmployeeAddress>();
		employeeAddresses = this.employeeAddressRepository.findByEmployeeIdAndIsDeleted(employeeId, false);
		return employeeAddresses;
	}

	/**
	 * Performing the soft delete only
	 */
	@Override
	public Boolean deleteEmployeeAddress(EmployeeAddress employeeAddress) {
		Long employeeId = employeeAddress.getEmployeeId();
		Long id = employeeAddress.getId();

		if (id == null) {
			throw new CustomMessageException("Address ID should not be null.");
		}

		Optional<EmployeeAddress> address = Optional.empty();
		address = this.employeeAddressRepository.findById(id);

		if (!address.isPresent()) {
			log.info("Address : " + id + " is NOT found against employee : " + employeeId);
			throw new CustomMessageException(
					"Address : " + id + " is NOT found against employee : " + employeeId + " to delete.");
		}
		if (employeeId != address.get().getEmployeeId()) {
			log.info("Employee ID : " + employeeId + " is NOT Matched against given address : " + id);
			throw new CustomMessageException(
					"Employee ID : " + employeeId + " is NOT Matched against given address : " + id);
		}
		address.get().setDeleted(true);

		address = Optional.ofNullable(this.employeeAddressRepository.save(address.get()));

		if (!address.isPresent()) {
			log.info("Error while deleting the address : " + id + " against given Employee : " + employeeId);
			throw new CustomMessageException(
					"Error while deleting the address : " + id + " against given Employee : " + employeeId);
		}

		// insert the history
		this.employeeHistoryRepository.save(this.prepareEmployeeHistory(address.get().getEmployeeNumber(),
				address.get().getId(), AppConstants.EMPLOYEE_ADDRESS, Operation.DELETE.toString(),
				address.get().getLastModifiedBy(), String.valueOf(address.get().getId()), null));
		log.info("Address : " + id + " is deleted against employee : " + employeeId);
		
		return true;
	}

	@Override
	public List<EmployeeHistory> findAuditById(String employeeNumber, Pageable pageable) {
		List<EmployeeHistory> histories = this.employeeHistoryRepository.findByEmployeeNumberOrderById(employeeNumber, pageable);
		String createdBy = histories.get(0).getLastModifiedBy();
		histories.forEach(e->{
			e.setCreatedBy(createdBy);
		});
		return histories;
	}

	/**
	 * Prepares the history objects for all forms and their child. Use this if you
	 * need single object only
	 * 
	 * @param moduleName
	 * @param operation
	 * @param lastModifiedBy
	 * @param oldValue
	 * @param newValue
	 * @return
	 */
	public EmployeeHistory prepareEmployeeHistory(String employeeNumber, Long childId, String moduleName,
			String operation, String lastModifiedBy, String oldValue, String newValue) {
		EmployeeHistory employeeHistory = new EmployeeHistory();
		employeeHistory.setEmployeeNumber(employeeNumber);
		employeeHistory.setChildId(childId);
		employeeHistory.setModuleName(moduleName);
		employeeHistory.setChangeType(AppConstants.UI);
		employeeHistory.setOperation(operation);
		employeeHistory.setOldValue(oldValue);
		employeeHistory.setNewValue(newValue);
		employeeHistory.setLastModifiedBy(lastModifiedBy);
		return employeeHistory;
	}

	@Override
	public PaginationResponse findAll(PaginationRequest paginationRequest) {
		List<Employee> employee = new ArrayList<Employee>();

		// preparing where clause
		String whereClause = this.prepareWhereClause(paginationRequest).toString();

		// get list
		employee = this.employeeDao.findAll(whereClause, paginationRequest);

		// getting count
		Long totalRecords = this.employeeDao.getCount(whereClause);

		return CommonUtils.setPaginationResponse(paginationRequest.getPageNumber(), paginationRequest.getPageSize(),
				employee, totalRecords);
	}

	private StringBuilder prepareWhereClause(PaginationRequest paginationRequest) {
		Map<String, ?> filters = paginationRequest.getFilters();

		String name = null;
		String number = null;
		String designation = null;
		String status = null;
//		String role = null;
		boolean isActive = false;

		if (filters.containsKey(FilterNames.NAME))
			name = (String) filters.get(FilterNames.NAME);
		if (filters.containsKey(FilterNames.NUMBER))
			number = (String) filters.get(FilterNames.NUMBER);
		if (filters.containsKey(FilterNames.DESIGNATION))
			designation = (String) filters.get(FilterNames.DESIGNATION);
//		if (filters.containsKey(FilterNames.ROLE))
//			role = (String) filters.get(FilterNames.ROLE);
		if (filters.containsKey(FilterNames.STATUS)) {
			status = (String) filters.get(FilterNames.STATUS);
			if (Status.ACTIVE.toString().equalsIgnoreCase(status)) {
				isActive = true;
			} else {
				isActive = false;
			}
		}
		
		StringBuilder whereClause = new StringBuilder(" AND e.isDeleted is false ");
		if (StringUtils.isNotEmpty(name)) {
			whereClause.append(" AND lower(e.fullName) like lower('%").append(name).append("%') ");
		}
		if (StringUtils.isNotEmpty(number)) {
			whereClause.append(" AND lower(e.employeeNumber) like lower('%").append(number).append("%')");
		}
		if (StringUtils.isNotEmpty(designation)) {
			whereClause.append(" AND lower(e.designation) like lower('%").append(designation).append("%')");
		}
//		if (StringUtils.isNotEmpty(role)) {
//			whereClause.append(" AND lower(e.vendorNumber) like lower('%").append(number).append("%')");
//		}
		if (StringUtils.isNotEmpty(status)) {
			whereClause.append(" AND e.isActive is ").append(isActive);
		}
		return whereClause;
	}
	
	@Override
	public Boolean getValidateName(String name) {
		// if name is empty then name is not valid
		if (StringUtils.isEmpty(name)) return false;
		
		Long countOfRecordsWithSameName = this.employeeRepository.getCountByName(name.trim());
		// if we we found the count greater than 0 then it is not valid. If it is zero then it is valid string
		if (countOfRecordsWithSameName > 0) return false; else return true;
	}
	
	/**
	 * To get list(id, name) only to display in the Dropdown
	 */
	@Override
	public Map<Long, String> getAllEmployees(Long subsidiaryId) {
		return this.employeeRepository.findIdAndNameMap(subsidiaryId, false);
	}

	@Override
	public String getEmailByEmployeeId(Long employeeId) {
		return this.employeeContactRepository.findEmailByEmployeeId(employeeId);
	}

	@Override
	public List<Employee> findBySubsidiaryId(Long subsidiaryId) {
		List<Employee> employees = new ArrayList<Employee>();
		employees = this.employeeRepository.getAllBySubsidiaryIdAndIsDeleted(subsidiaryId, false);
		log.info("Get all employee by subsidiary  ." + employees);
		return employees;
	}

	@Override
	public List<IdNameResponse> findByRoleId(Long roleId, Long subsidiaryId) {
		List<IdNameResponse> employees = new ArrayList<IdNameResponse>();
		employees = this.employeeRepository.findByRoleIdAndSubsidiary(roleId, subsidiaryId);
		return employees;
	}
	
	@Override
	public Long getEmployeeIdByAccessMail(String email) {
		List<EmployeeAccess> employeeAccesses = this.employeeAccessRepository.findByAccessMail(email);
		if (CollectionUtils.isNotEmpty(employeeAccesses)) return employeeAccesses.get(0).getEmployeeId();
		return null;
	}
}
