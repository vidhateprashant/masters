package com.monstarbill.masters.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.monstarbill.masters.models.CustomRoles;
import com.monstarbill.masters.models.DefaultRolePermissions;
import com.monstarbill.masters.models.RolesHistory;
import com.monstarbill.masters.payload.request.PaginationRequest;
import com.monstarbill.masters.payload.response.PaginationResponse;

public interface RolesService {

	public CustomRoles save(CustomRoles role);

	public CustomRoles getRoleById(Long id);

	public List<RolesHistory> findHistoryById(Long id, Pageable pageable);

	public PaginationResponse findAll(PaginationRequest paginationRequest);

	public List<CustomRoles> getRoleByIds(List<Long> roleIds);

	public List<CustomRoles> findRolesBySubsidiaryId(Long subsidiaryId, List<String> accessType);

	public List<CustomRoles> findBySubsidiaryForEmplyoee(Long subsidiaryId);

	public List<DefaultRolePermissions> findAccessPointBySelectedAccess(String selectedAccess);

}
