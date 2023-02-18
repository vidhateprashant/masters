package com.monstarbill.masters.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.monstarbill.masters.models.SupplierAccess;

@Repository
public interface SupplierAccessRepository extends JpaRepository<SupplierAccess, String> {

	public Optional<SupplierAccess> findBySupplierIdAndIsDeleted(Long supplierId, boolean isDeleted);

	public Optional<SupplierAccess> findByIdAndIsDeleted(Long id, boolean isDeleted);

	@Query("SELECT plainPassword FROM SupplierAccess WHERE supplierId = :supplierId ")
	public String getPasswordById(Long supplierId);

	public SupplierAccess findBySupplierId(Long supplierId);

	public List<SupplierAccess> findByAccessMail(String email);

}
