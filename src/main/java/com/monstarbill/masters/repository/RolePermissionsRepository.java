package com.monstarbill.masters.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.monstarbill.masters.models.RolePermissions;

@Repository
public interface RolePermissionsRepository extends JpaRepository<RolePermissions, String> {

	public Optional<RolePermissions> findByIdAndIsDeleted(Long id, boolean isDeleted);

	public List<RolePermissions> findAllByRoleIdAndIsDeleted(Long roleId, boolean isDeleted);

}
