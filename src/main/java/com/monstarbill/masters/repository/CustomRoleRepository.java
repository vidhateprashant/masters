package com.monstarbill.masters.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.monstarbill.masters.models.CustomRoles;

@Repository
public interface CustomRoleRepository extends JpaRepository<CustomRoles, String> {

	public Optional<CustomRoles> findByIdAndIsDeleted(Long id, boolean isDeleted);

	public Optional<CustomRoles> findByNameAndIsDeleted(String roleName, boolean isDeleted);

	@Query("select new com.monstarbill.masters.models.CustomRoles(cr.id, cr.name, cr.selectedAccess, cr.isActive) from CustomRoles cr where cr.selectedAccess = :selectedAccess AND cr.subsidiaryId = :subsidiaryId and cr.isDeleted = :isDeleted ")
	public List<CustomRoles> getAllRolesBySubsidiaryIdAndIsDeleted(@Param("subsidiaryId") Long subsidiaryId, @Param("isDeleted") boolean isDeleted, String selectedAccess);
	
	@Query("select new com.monstarbill.masters.models.CustomRoles(cr.id, cr.name, cr.selectedAccess, cr.isActive) from CustomRoles cr where cr.subsidiaryId = :subsidiaryId and cr.isDeleted = :isDeleted ")
	public List<CustomRoles> getAllRolesBySubsidiaryIdAndIsDeletedForEmployee(@Param("subsidiaryId") Long subsidiaryId, @Param("isDeleted") boolean isDeleted);

}
