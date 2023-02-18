package com.monstarbill.masters.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.monstarbill.masters.models.SupplierContact;

@Repository
public interface SupplierContactRepository extends JpaRepository<SupplierContact, String> {

	public List<SupplierContact> findBySupplierIdAndIsDeleted(Long supplierId, boolean isDeleted);
	
	public Optional<SupplierContact> findByIdAndIsDeleted(Long id, boolean isDeleted);

	public Optional<SupplierContact> findBySupplierIdAndIsPrimaryContactAndIsDeleted(Long vendorId, boolean isPrimaryContact,
			boolean isDeleted);

}
