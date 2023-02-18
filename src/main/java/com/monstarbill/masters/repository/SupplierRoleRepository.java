package com.monstarbill.masters.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.monstarbill.masters.models.SupplierRole;

@Repository
public interface SupplierRoleRepository extends JpaRepository<SupplierRole, String> {

	public List<SupplierRole> findBySupplierIdAndIsDeleted(Long supplierId, boolean isDeleted);

	public Optional<SupplierRole> findByIdAndIsDeleted(Long id, boolean isDeleted);

}
