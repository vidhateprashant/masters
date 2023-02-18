package com.monstarbill.masters.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.monstarbill.masters.models.SupplierSubsidiary;

@Repository
public interface SupplierSubsidiaryRepository extends JpaRepository<SupplierSubsidiary, String> {

	public List<SupplierSubsidiary> findBySupplierId(Long supplierId);

	public Optional<SupplierSubsidiary> findBySupplierIdAndSubsidiaryId(Long supplierId, Long subsidiaryId);
	
	public void delete(SupplierSubsidiary supplierSubsidiary);

	@Query("select new com.monstarbill.masters.models.SupplierSubsidiary(ss.id, ss.supplierId, ss.subsidiaryId, sub.name, ss.currency, ss.supplierCurrency, ss.isPreferredCurrency) from SupplierSubsidiary ss INNER JOIN Subsidiary sub ON ss.subsidiaryId = sub.id WHERE ss.supplierId = :supplierId AND ss.isDeleted = :isDeleted order by ss.id asc ")
	public List<SupplierSubsidiary> findBySupplierIdAndDeletedWithName(@Param("supplierId") Long supplierId, @Param("isDeleted") boolean isDeleted);

	public Optional<SupplierSubsidiary> findByIdAndIsDeleted(Long id, boolean isDeleted);

	public List<SupplierSubsidiary> findBySubsidiaryIdAndSupplierCurrency(Long subsidiaryId, String supplierCurrency);
	
}
