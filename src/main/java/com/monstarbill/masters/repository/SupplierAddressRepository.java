package com.monstarbill.masters.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.monstarbill.masters.models.SupplierAddress;

@Repository
public interface SupplierAddressRepository extends JpaRepository<SupplierAddress, String> {

	public List<SupplierAddress> findBySupplierId(Long supplierId);
	
	public Optional<SupplierAddress> findById(Long id);

	public List<SupplierAddress> findBySupplierIdAndIsDeleted(Long supplierId, boolean isDeleted);

	public Optional<SupplierAddress> findByIdAndIsDeleted(Long id, boolean isDeleted);

	public List<SupplierAddress> findBySupplierIdAndAddressCodeAndIsDeleted(Long supplierId, String addressCode,
			boolean isDeleted);

}
