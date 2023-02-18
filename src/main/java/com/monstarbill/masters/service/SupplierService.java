package com.monstarbill.masters.service;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.monstarbill.masters.models.GraphData;
import com.monstarbill.masters.models.Supplier;
import com.monstarbill.masters.models.SupplierAddress;
import com.monstarbill.masters.models.SupplierContact;
import com.monstarbill.masters.models.SupplierHistory;
import com.monstarbill.masters.models.SupplierSubsidiary;
import com.monstarbill.masters.payload.request.PaginationRequest;
import com.monstarbill.masters.payload.response.PaginationResponse;

public interface SupplierService {

	public Supplier save(Supplier supplier);

	public Supplier getSupplierById(Long id);

	public SupplierSubsidiary saveSubsidiary(SupplierSubsidiary supplierSubsidiary);

	public Long findSubsidiaryBySupplierId(Long id);

	public Boolean deleteSupplierSubsidiary(SupplierSubsidiary supplierSubsidiary);

	public List<SupplierAddress> findAddressBySupplierId(Long supplierId);

	public Boolean deleteSupplierAddress(SupplierAddress supplierAddress);

	public List<SupplierHistory> findAuditById(Long id, Pageable pageable);

	public List<Supplier> findAllSuppliers();

	public List<Supplier> getSuppliersBySubsidiaryId(Long subsidiaryId);

	public PaginationResponse findAll(PaginationRequest paginationRequest);

	public List<Supplier> getSupplierApproval(String user);

	public List<SupplierSubsidiary> findCurrencyBySupplierId(Long supplierId);

	public Boolean getValidateName(String name);

	public Boolean sendForApproval(Long supplierId);

	public Boolean approveAllSuppliers(List<Long> supplierIds);

	public byte[] upload(MultipartFile file);

	public byte[] downloadTemplate();

	public Boolean rejectAllSuppliers(List<Supplier> suppliers);

	public Boolean updateNextApprover(Long approverId, Long supplierId);

	public Boolean selfApprove(Long supplierId);

	public List<GraphData> findBysubsidiaryAndStatus(Long subsidiaryId);
	
	public List<GraphData> getdashboard(Long subsidiaryId);

	public String sendMailBySupplierId(Long supplierId);

	public Long getSupplierIdByAccessMail(String email);

	public Supplier findByName(String supplierName);

	public List<SupplierAddress> findAddressBySupplierIdAndAddressCode(Long supplierId, String addressCode);

	public SupplierContact findContactBySupplierIdAndIsPrimaryContact(Long vendorId, boolean isPrimaryContact);

	public List<Supplier> getSuppliersByIds(List<Long> supplierIds);
}
