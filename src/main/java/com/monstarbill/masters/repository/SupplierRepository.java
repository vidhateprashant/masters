package com.monstarbill.masters.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.monstarbill.masters.models.Supplier;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, String> {
	
	public Optional<Supplier> findByIdAndIsDeleted(Long id, boolean isDeleted);

	@Query(" select new com.monstarbill.masters.models.Supplier(id, name) from Supplier WHERE isDeleted = :isDeleted ORDER BY name asc ")
	public List<Supplier> findAllSuppliers(@Param("isDeleted") boolean isDeleted);

	@Query(" SELECT new com.monstarbill.masters.models.Supplier(s.id, s.name) FROM Supplier s "
			+ " INNER JOIN SupplierSubsidiary ss ON s.id = ss.supplierId "
			+ " WHERE s.isDeleted = false and ss.isDeleted = false AND ss.subsidiaryId = :subsidiaryId "
			+ " AND s.approvalStatus = 'Approved' AND s.isActive is true  "
			+ " GROUP BY s.id, s.name ")
	public List<Supplier> findBySubsidiaryId(@Param("subsidiaryId") Long subsidiaryId);

	@Query("select new com.monstarbill.masters.models.Supplier(s.id, s.name, ss.currency as currency) from Supplier s inner join SupplierSubsidiary ss ON s.id = ss.supplierId WHERE s.id = :supplierId and s.isDeleted = :isDeleted ")
	public List<Supplier> getAllCurrencyBySupplier(@Param("supplierId") Long supplierId, @Param("isDeleted") boolean isDeleted);
	
	@Query(" SELECT new com.monstarbill.masters.models.Supplier(s.id, s.name, s.approvalStatus, s.vendorNumber, s.vendorType, s.uin, s.rejectComments, sa.access as supplierAccess, ss.subsidiaryId as subsidiaryId, su.name as subsidiaryName, s.approvedBy, "
			+ " s.nextApprover, s.nextApproverRole, e.fullName) FROM Supplier s "
			+ " inner join SupplierSubsidiary ss ON s.id = ss.supplierId inner join Subsidiary su ON su.id = ss.subsidiaryId inner join SupplierAccess sa"
			+ " ON s.id = sa.supplierId left join Employee e ON CAST(e.id as text) = s.approvedBy where s.approvalStatus in :status and s.nextApprover = :userId ")
	public List<Supplier> findAllBySupplierStatus(@Param("status") List<String> status, @Param("userId") String user);

	@Query("select count(id) from Supplier where lower(name) = lower(:name) AND isDeleted = false ")
	public Long getCountByName(@Param("name") String name);
	
	@Query("select new com.monstarbill.masters.models.Supplier(s.id, s.name) from Supplier s WHERE s.id IN :suppliers and s.isDeleted = :isDeleted ")
	public List<Supplier> getSupplierNamesByIds(List<Long> suppliers, @Param("isDeleted") boolean isDeleted);

	public Optional<Supplier> findByNameAndIsDeleted(String vendorName, boolean isDeleted);

	@Query("select count(*) from Supplier su inner join SupplierSubsidiary ss ON ss.supplierId = su.id where ss.subsidiaryId = :subsidiaryId and su.isActive = :isActive ")
	public Long getCountBySubsidiaryIdAndIsActive(Long subsidiaryId, boolean isActive);
	
	@Query("select count(*) from Supplier su inner join SupplierSubsidiary ss ON ss.supplierId = su.id where ss.subsidiaryId = :subsidiaryId and su.approvalStatus = :approvalStatus ")
	public Long getCountBySubsidiaryIdAndIsActive(Long subsidiaryId, String approvalStatus);
}

