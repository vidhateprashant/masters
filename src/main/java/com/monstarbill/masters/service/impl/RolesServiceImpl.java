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
import com.monstarbill.masters.dao.CustomRolesDao;
import com.monstarbill.masters.enums.Operation;
import com.monstarbill.masters.enums.Status;
import com.monstarbill.masters.models.CustomRoles;
import com.monstarbill.masters.models.RolePermissions;
import com.monstarbill.masters.models.RolesDepartment;
import com.monstarbill.masters.models.RolesHistory;
import com.monstarbill.masters.payload.request.PaginationRequest;
import com.monstarbill.masters.payload.response.PaginationResponse;
import com.monstarbill.masters.repository.CustomRoleRepository;
import com.monstarbill.masters.repository.RoleHistoryRepository;
import com.monstarbill.masters.repository.RolePermissionsRepository;
import com.monstarbill.masters.repository.RolesDepartmentRepository;
import com.monstarbill.masters.service.RolesService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class RolesServiceImpl implements RolesService {
	
	@Autowired
	private CustomRoleRepository customRoleRepository;
	
	@Autowired
	private RolesDepartmentRepository rolesDepartmentRepository;
	
	@Autowired
	private RolePermissionsRepository rolePermissionsRepository;
	
	@Autowired
	private RoleHistoryRepository roleHistoryRepository;
	
	@Autowired
	private CustomRolesDao customRolesDao;
	
	/**
	 * Save -
	 * 1. Role
	 * 2. Role-subsidiary Mapping
	 * 3. Role-Permission Mapping
	 */
	@Override
	public CustomRoles save(CustomRoles role) {
		log.info("Save Role started...");
		
		String username = CommonUtils.getLoggedInUsername();
		
		// 1. Save the Custom role
		Optional<CustomRoles> customRole = Optional.empty();
		Optional<CustomRoles> oldCustomRole = Optional.empty();
		
		role.setLastModifiedBy(username);
		if (role.getId() == null) {
			role.setCreatedBy(username);
		} else {
			// Get the existing object using the deep copy
			oldCustomRole = this.customRoleRepository.findByIdAndIsDeleted(role.getId(), false);
			if (oldCustomRole.isPresent()) {
				try {
					oldCustomRole = Optional.ofNullable((CustomRoles) oldCustomRole.get().clone());
				} catch (CloneNotSupportedException e) {
					log.error("Error while Cloning the object. Please contact administrator.");
					throw new CustomException("Error while Cloning the object. Please contact administrator.");
				}
			}
		}
		customRole = Optional.ofNullable(this.customRoleRepository.save(role));
		
		if (!customRole.isPresent()) {
			log.info("Error while saving the Role.");
			throw new CustomMessageException("Error while saving the Role.");
		}
		Long roleId = customRole.get().getId();
		
		log.info("Role is saved successfully :: " + roleId);
		
		// update the data in bank history table
		this.updateRoleHistory(customRole.get(), oldCustomRole);
		log.info("Role history is updated for role id : " + roleId);

		// 2. save the mapping of roles and subsidiaries
		if (CollectionUtils.isNotEmpty(role.getRestrictedDepartments())) {
			for (RolesDepartment rolesDepartment : role.getRestrictedDepartments()) {
				if (rolesDepartment.getId() == null) {
					// INSERT Operation
					rolesDepartment.setRoleId(roleId);
					rolesDepartment.setLastModifiedBy(username);
					rolesDepartment.setCreatedBy(username);
					
					rolesDepartment = this.rolesDepartmentRepository.save(rolesDepartment);
					
					if (rolesDepartment == null) {
						log.error("Error while saving the Restricted departments of the Role.");
						throw new CustomException("Error while saving the Restricted departments of the Role.");
					}
					
					this.roleHistoryRepository.save(this.prepareCustomRoleHistory(roleId, rolesDepartment.getId(), AppConstants.ROLE_RESTRICTED_DEPARTMENT, Operation.CREATE.toString(), rolesDepartment.getLastModifiedBy(), null, rolesDepartment.getDepartmentName()));
				} else if (rolesDepartment.isDeleted()) {
					// DELETE Operation
					rolesDepartment.setLastModifiedBy(username);
					
					rolesDepartment = this.rolesDepartmentRepository.save(rolesDepartment);
					
					if (rolesDepartment == null) {
						log.error("Error while deleting the Restricted departments of the Role.");
						throw new CustomException("Error while deleting the Restricted departments of the Role.");
					}
					
					this.roleHistoryRepository.save(this.prepareCustomRoleHistory(roleId, rolesDepartment.getId(), AppConstants.ROLE_RESTRICTED_DEPARTMENT, Operation.DELETE.toString(), rolesDepartment.getLastModifiedBy(), rolesDepartment.getDepartmentName(), null));
				}
			}
		}
		customRole.get().setRestrictedDepartments(role.getRestrictedDepartments());
		
		// 3. save the mapping of roles and Permissions
		for (RolePermissions rolePermission : role.getRolePermissions()) {
			
			Optional<RolePermissions> oldRolePermission = Optional.ofNullable(null);
			if (rolePermission.getId() == null) {
				rolePermission.setRoleId(roleId);
				rolePermission.setCreatedBy(username);				
			} else {
				// Get the existing object using the deep copy
				oldRolePermission = this.rolePermissionsRepository.findByIdAndIsDeleted(rolePermission.getId(), false);
				if (oldRolePermission.isPresent()) {
					try {
						oldRolePermission = Optional.ofNullable((RolePermissions) oldRolePermission.get().clone());
					} catch (CloneNotSupportedException e) {
						log.error("Error while Cloning the object. Please contact administrator.");
						throw new CustomException("Error while Cloning the object. Please contact administrator.");
					}
				}
			}
			
			rolePermission.setLastModifiedBy(username);
			try {
				rolePermission = this.rolePermissionsRepository.save(rolePermission);
			} catch (DataIntegrityViolationException e) {
				log.error(" Role unique constrain violetd." + e.getMostSpecificCause());
				throw new CustomException("Role unique constrain violetd :" + e.getMostSpecificCause());
			}
			log.info("Role Permissions are saved :: " + rolePermission.getId());

			// update the data in Account history table
			this.updateRolePermissionHistory(rolePermission, oldRolePermission);
			log.info("Role Permissions History are saved for Role :: " + rolePermission.getId());
		}
		customRole.get().setRolePermissions(role.getRolePermissions());
		
		log.info("Save Role Finished with Role id : " + roleId);
		return customRole.get();
	}

	/**
	 * This method save the data in history table
	 * Add entry as a Insert if Role-Permission is new 
	 * Add entry as a Update if Role-Permission is exists
	 * 
	 * @param rolePermission
	 * @param old-rolePermission
	 */
	private void updateRolePermissionHistory(RolePermissions rolePermission, Optional<RolePermissions> oldRolePermission) {
		if (oldRolePermission.isPresent()) {
			if (rolePermission.isDeleted()) {
				// insert the updated fields in history table
				List<RolesHistory> roleHistories = new ArrayList<RolesHistory>();
				try {
					roleHistories = oldRolePermission.get().compareFields(rolePermission);
					if (CollectionUtils.isNotEmpty(roleHistories)) {
						this.roleHistoryRepository.saveAll(roleHistories);
					}
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					log.error("Error while comparing the new and old objects. Please contact administrator.");
					throw new CustomException("Error while comparing the new and old objects. Please contact administrator.");
				}
			} else {
				this.roleHistoryRepository.save(this.prepareCustomRoleHistory(rolePermission.getRoleId(), rolePermission.getId(), AppConstants.ROLE_ACCESS_POINT, Operation.DELETE.toString(), rolePermission.getLastModifiedBy(), rolePermission.getAccessPoint(), null));
			}
		} else {
			// Insert in history table as Operation - INSERT 
			this.roleHistoryRepository.save(this.prepareCustomRoleHistory(rolePermission.getRoleId(), rolePermission.getId(), AppConstants.ROLE_ACCESS_POINT, Operation.CREATE.toString(), rolePermission.getLastModifiedBy(), null, rolePermission.getAccessPoint()));
		}
		log.info("Custom Role History is updated successfully");
	}
	

	/**
	 * This method save the data in history table
	 * Add entry as a Insert if Role is new 
	 * Add entry as a Update if Role is exists
	 * 
	 * @param customRole
	 * @param oldCustomRole
	 */
	private void updateRoleHistory(CustomRoles customRole, Optional<CustomRoles> oldCustomRole) {
		if (oldCustomRole.isPresent()) {
			// insert the updated fields in history table
			List<RolesHistory> roleHistories = new ArrayList<RolesHistory>();
			try {
				roleHistories = oldCustomRole.get().compareFields(customRole);
				if (CollectionUtils.isNotEmpty(roleHistories)) {
					this.roleHistoryRepository.saveAll(roleHistories);
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				log.error("Error while comparing the new and old objects. Please contact administrator.");
				throw new CustomException("Error while comparing the new and old objects. Please contact administrator.");
			}
			log.info("Custom Role History is updated successfully");
		} else {
			// Insert in history table as Operation - INSERT 
			this.roleHistoryRepository.save(this.prepareCustomRoleHistory(customRole.getId(), null, AppConstants.ROLE, Operation.CREATE.toString(), customRole.getLastModifiedBy(), null, String.valueOf(customRole.getId())));
		}
	}
	
	/**
	 * Prepares the history for the Custom role
	 * @param roleId
	 * @param moduleName
	 * @param operation
	 * @param lastModifiedBy
	 * @param oldValue
	 * @param newValue
	 * @return
	 */
	public RolesHistory prepareCustomRoleHistory(Long roleId, Long childId, String moduleName, String operation, String lastModifiedBy, String oldValue, String newValue) {
		RolesHistory roleHistory = new RolesHistory();
		roleHistory.setRoleId(roleId);
		roleHistory.setChildId(childId);
		roleHistory.setModuleName(moduleName);
		roleHistory.setChangeType(AppConstants.UI);
		roleHistory.setOperation(operation);
		roleHistory.setOldValue(oldValue);
		roleHistory.setNewValue(newValue);
		roleHistory.setLastModifiedBy(lastModifiedBy);
		return roleHistory;
	}

	
	public List<RolePermissions> getRolePermissionsByRoleId(Long roleId) {
		return this.rolePermissionsRepository.findAllByRoleIdAndIsDeleted(roleId, false);
	}
	
	public List<RolesDepartment> getRolesDepartmentByRoleId(Long roleId) {
		return this.rolesDepartmentRepository.findAllByRoleIdAndIsDeleted(roleId, false);
	}
	
	@Override
	public CustomRoles getRoleById(Long id) {
		Optional<CustomRoles> customRole = Optional.empty();
		customRole = this.customRoleRepository.findByIdAndIsDeleted(id, false);
		if (!customRole.isPresent()) {
			log.info("Role is not exist for id - " + id);
			throw new CustomMessageException("Role is not exist for id - " + id);
		}
		customRole.get().setRestrictedDepartments(this.getRolesDepartmentByRoleId(id));
		customRole.get().setRolePermissions(this.getRolePermissionsByRoleId(id));
		return customRole.get();
	}

	@Override
	public PaginationResponse findAll(PaginationRequest paginationRequest) {
		List<CustomRoles> customRoles = new ArrayList<CustomRoles>();

		// preparing where clause
		String whereClause = this.prepareWhereClause(paginationRequest).toString();

		// get list
		customRoles = this.customRolesDao.findAll(whereClause, paginationRequest);

		// getting count
		Long totalRecords = this.customRolesDao.getCount(whereClause);

		return CommonUtils.setPaginationResponse(paginationRequest.getPageNumber(), paginationRequest.getPageSize(),
				customRoles, totalRecords);
	}

	private StringBuilder prepareWhereClause(PaginationRequest paginationRequest) {
		Map<String, ?> filters = paginationRequest.getFilters();
		Long subsidiaryId = null;
		String roleName = null;
		boolean isActive = false;
		String status = null;
		
		if (filters.containsKey(FilterNames.SUBSIDIARY_ID))
			subsidiaryId = ((Number) filters.get(FilterNames.SUBSIDIARY_ID)).longValue();
		if (filters.containsKey(FilterNames.ROLE_NAME))
			roleName = ((String) filters.get(FilterNames.ROLE_NAME));
		if (filters.containsKey(FilterNames.STATUS)) {
			if (StringUtils.isNotEmpty((String) filters.get(FilterNames.STATUS))) {
				status = (String) filters.get(FilterNames.STATUS);
				if (Status.ACTIVE.toString().equalsIgnoreCase(status)) {
					isActive = true;
				} else {
					isActive = false;
				}
			}
		}
		StringBuilder whereClause = new StringBuilder(" AND r.isDeleted is false");
		if (subsidiaryId != null && subsidiaryId != 0) {
			whereClause.append(" AND r.subsidiaryId = ").append(subsidiaryId);
		}
		if (StringUtils.isNotEmpty(roleName)) {
			whereClause.append(" AND lower(r.name) like lower('%").append(roleName).append("%')");
		}
		if (StringUtils.isNotEmpty(status)) {
			whereClause.append(" AND r.isActive is ").append(isActive);
		}
		return whereClause;
	}

	@Override
	public List<RolesHistory> findHistoryById(Long id, Pageable pageable) {
		return this.roleHistoryRepository.findByRoleId(id, pageable);
	}

	@Override
	public List<CustomRoles> getRoleByIds(List<Long> roleIds) {
		List<CustomRoles> customRoles = new ArrayList<CustomRoles>();
		
		for (Long roleId : roleIds) {
			Optional<CustomRoles> customRole = this.customRoleRepository.findByIdAndIsDeleted(roleId, false);
			if (!customRole.isPresent()) {
				continue;
			}
			customRole.get().setRestrictedDepartments(this.getRolesDepartmentByRoleId(roleId));
			customRole.get().setRolePermissions(this.getRolePermissionsByRoleId(roleId));
			
			customRoles.add(customRole.get());
		}
		
		return customRoles;
	}

	@Override
	public List<CustomRoles> findRolesBySubsidiaryId(Long subsidiaryId, String accessType) {
		List<CustomRoles> customRoles = new ArrayList<CustomRoles>();
		customRoles = this.customRoleRepository.getAllRolesBySubsidiaryIdAndIsDeleted(subsidiaryId, false, accessType);
		log.info("Get all roles  by subsidary id and type ." + customRoles);
		return customRoles;
	}

	@Override
	public List<CustomRoles> findBySubsidiaryForEmplyoee(Long subsidiaryId) {
		List<CustomRoles> customRoles = new ArrayList<CustomRoles>();
		customRoles = this.customRoleRepository.getAllRolesBySubsidiaryIdAndIsDeletedForEmployee(subsidiaryId, false);
		log.info("Get all roles  by subsidary id and type ." + customRoles);
		return customRoles;
	}

}
