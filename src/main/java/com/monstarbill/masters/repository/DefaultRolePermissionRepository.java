package com.monstarbill.masters.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.monstarbill.masters.models.DefaultRolePermissions;

@Repository
public interface DefaultRolePermissionRepository extends JpaRepository<DefaultRolePermissions, String> {

	public List<DefaultRolePermissions> findAllByIsAdminAccessAndIsDeleted(boolean isAdminAccess, boolean isDeleted);
	
	public List<DefaultRolePermissions> findAllByIsSupplierAccessAndIsDeleted(boolean isSupplierAccess, boolean isDeleted);

	public List<DefaultRolePermissions> findAllByIsApproverAccessAndIsDeleted(boolean isApproverAccess, boolean isDeleted);
}
