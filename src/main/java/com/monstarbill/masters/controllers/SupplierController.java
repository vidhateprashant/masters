package com.monstarbill.masters.controllers;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.monstarbill.masters.commons.CustomException;
import com.monstarbill.masters.commons.CustomMessageException;
import com.monstarbill.masters.commons.ExcelHelper;
import com.monstarbill.masters.models.GraphData;
import com.monstarbill.masters.models.Supplier;
import com.monstarbill.masters.models.SupplierAddress;
import com.monstarbill.masters.models.SupplierContact;
import com.monstarbill.masters.models.SupplierHistory;
import com.monstarbill.masters.models.SupplierSubsidiary;
import com.monstarbill.masters.payload.request.PaginationRequest;
import com.monstarbill.masters.payload.response.PaginationResponse;
import com.monstarbill.masters.service.SupplierService;

import lombok.extern.slf4j.Slf4j;

/**
 * All WS's of the Supplier and it's child components
 * @author prashant
 *
 */
//@CrossOrigin(origins= "*", allowedHeaders = "*", maxAge = 4800, allowCredentials = "false" )
@RestController
@RequestMapping("/supplier")
@Slf4j
public class SupplierController {

	@Autowired
	private SupplierService supplierService;
	
	@PostMapping("/save")
	public ResponseEntity<Supplier> saveSupplier(@Valid @RequestBody Supplier supplier) {
		log.info("Saving the Supplier :: " + supplier.toString());
		supplier = supplierService.save(supplier);
		log.info("Supplier saved successfully");
		return ResponseEntity.ok(supplier);
	}
	
	@GetMapping("/get")
	public ResponseEntity<Supplier> findById(@RequestParam Long id) {
		log.info("Get Supplier for ID :: " + id);
		Supplier supplier = supplierService.getSupplierById(id);
		if (supplier == null) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		log.info("Returning from find by id supplier");
		return new ResponseEntity<>(supplier, HttpStatus.OK);
	}
	
	@GetMapping("/self-approve")
	public ResponseEntity<Boolean> selfApprove(@RequestParam Long supplierId) {
		log.info("Self approve for Supplier ID :: " + supplierId);
		Boolean supplierSelfApproved = supplierService.selfApprove(supplierId);
		log.info("Self approve for Supplier id Finished");
		return new ResponseEntity<>(supplierSelfApproved, HttpStatus.OK);
	}
	
	/**
	 * get all suppliers for the table with pagination
	 * @param 
	 * @return
	 */
	@PostMapping("/get/all")
	public ResponseEntity<PaginationResponse> findAll(@RequestBody PaginationRequest paginationRequest) {
		log.info("Get All suppliers started");
		PaginationResponse paginationResponse = new PaginationResponse();
		paginationResponse = supplierService.findAll(paginationRequest);
		log.info("Get All suppliers Finished");
		return new ResponseEntity<>(paginationResponse, HttpStatus.OK);
	}
	
	/**
	 * get all approved suppliers
	 * @param 
	 * @return list of vendors(id and name only)
	 */
	@GetMapping("/get-suppliers")
	public ResponseEntity<List<Supplier>> findAllSuppliers() {
		log.info("Get Supplier List");
		List<Supplier> supplier = new ArrayList<Supplier>();
		
		supplier = supplierService.findAllSuppliers();
		if (supplier == null) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		log.info("Returning from supplier list");
		return new ResponseEntity<>(supplier, HttpStatus.OK);
	}
	
	/**
	 * Save the subsidiary against supplier
	 * @param supplierSubsidiary
	 * @return
	 */
	@Deprecated
	@PostMapping("/subsidiary/save")
	public ResponseEntity<SupplierSubsidiary> saveSupplierSubsidiary(@Valid @RequestBody SupplierSubsidiary supplierSubsidiary) {
		log.info("Saving the Supplier Subsidiary mapping :: " + supplierSubsidiary.toString());
		supplierSubsidiary = supplierService.saveSubsidiary(supplierSubsidiary);
		log.info("Supplier Subsidiary mapping saved successfully");
		return ResponseEntity.ok(supplierSubsidiary);
	}
	
	/**
	 * get all the subsidiary against the supplier
	 * @param id
	 * @return
	 */
	@Deprecated
	@GetMapping("/subsidiary/get")
	public ResponseEntity<List<SupplierSubsidiary>> findSubsidiaryBySupplierId(@RequestParam Long id) {
//		log.info("Get Subsidiary against the Supplier ID :: " + id);
//		List<SupplierSubsidiary> supplierSubsidiaries = supplierService.findSubsidiaryBySupplierId(id);
//		if (CollectionUtils.isEmpty(supplierSubsidiaries)) {
//			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
//		}
		return new ResponseEntity<>(null, HttpStatus.OK);
	}
	
	/**
	 * Deletes the supplier subsidiary mapping
	 * @param supplierSubsidiary
	 * @return
	 */
	@Deprecated
	@PostMapping("/subsidiary/delete")
	public ResponseEntity<Boolean> deleteSupplierSubsidiary(@RequestBody SupplierSubsidiary supplierSubsidiary) {
		log.info("Deleting the Subsidiary " + supplierSubsidiary.getSubsidiaryId() + " against the Supplier ID :: " + supplierSubsidiary.getSupplierId());
		Boolean isDeleted = supplierService.deleteSupplierSubsidiary(supplierSubsidiary);
		return new ResponseEntity<>(isDeleted, HttpStatus.OK);
	}
	
	/**
	 * save the address of the supplier
	 * @param supplierAddress
	 * @return
	 */
	@Deprecated
	@PostMapping("/address/save")
	public ResponseEntity<SupplierAddress> saveSupplierAddress(@Valid @RequestBody SupplierAddress supplierAddress) {
		log.info("Saving the Supplier Address :: " + supplierAddress.toString());
		// supplierAddress = supplierService.saveAddress(supplierAddress);
		log.info("Supplier Address saved successfully");
		return ResponseEntity.ok(supplierAddress);
	}

	/**
	 * Get all the address against the supplier
	 * @param supplierId
	 * @return
	 */
	@GetMapping("/address/get")
	public ResponseEntity<List<SupplierAddress>> findAddressBySupplierId(@RequestParam Long supplierId) {
		log.info("Get Address against the Supplier ID :: " + supplierId);
		List<SupplierAddress> supplierAddresses = supplierService.findAddressBySupplierId(supplierId);
		if (CollectionUtils.isEmpty(supplierAddresses)) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<>(supplierAddresses, HttpStatus.OK);
	}

	/**
	 * Delete the address against the supplier and id
	 * @param supplierAddress
	 * @return
	 */
	@Deprecated
	@PostMapping("/address/delete")
	public ResponseEntity<Boolean> deleteSupplierAddress(@RequestBody SupplierAddress supplierAddress) {
		log.info("Deleting the Address of the Supplier ID :: " + supplierAddress.getSupplierId());
		Boolean isDeleted = supplierService.deleteSupplierAddress(supplierAddress);
		return new ResponseEntity<>(isDeleted, HttpStatus.OK);
	}
	
	/**
	 * Find history by supplier Id
	 * Supported for server side pagination
	 * @param id
	 * @param pageSize
	 * @param pageNumber
	 * @return
	 */
	@GetMapping("/get/history")
	public ResponseEntity<List<SupplierHistory>> findHistoryById(@RequestParam Long id, @RequestParam(defaultValue = "10") int pageSize, @RequestParam(defaultValue = "0") int pageNumber, @RequestParam(defaultValue = "id") String sortColumn) {
		log.info("Get Supplier Audit for Supplier ID :: " + id);
		Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(sortColumn));
		List<SupplierHistory> supplierHistoreis = supplierService.findAuditById(id, pageable);
		log.info("Returning from Supplier Audit by id.");
		return new ResponseEntity<>(supplierHistoreis, HttpStatus.OK);
	}
	
	/**
	 * Get all the suppliers by subsidiary Id
	 * @param subsidiaryId
	 * @return
	 */
	@GetMapping("/get-by-subsidiary-id")
	public ResponseEntity<List<Supplier>> findSuppliersBySubsidiaryId(@RequestParam Long subsidiaryId) {
		log.info("Get Supplier List By Subsidairy Id. STARTED ... ");
		List<Supplier> suppliers = new ArrayList<Supplier>();
		suppliers = supplierService.getSuppliersBySubsidiaryId(subsidiaryId);
		if (suppliers == null) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		log.info("Get Supplier List By Subsidairy Id. FINISHED ... ");
		return new ResponseEntity<>(suppliers, HttpStatus.OK);
	}
	
	/**
	 * get the all values for supplier approval process
	 * 
	 * @return
	 */
	@GetMapping("/get-supplier-appoval")
	public ResponseEntity<List<Supplier>> getSupplierApproval( @RequestParam String user) {
		List<Supplier> suppliers = new ArrayList<Supplier>();
		try {
			suppliers =	supplierService.getSupplierApproval( user);
			log.info("Getting the Supplier for approval " + suppliers);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException("Exception while getting the approval process for the supplier :: " + e.toString());
		}
		return ResponseEntity.ok(suppliers);
	}
	
	/**
	 * get only currency by supplier
	 * @param supplierId
	 * @return currency
	 */
	@GetMapping("/get-currency-by-supplier")
	public ResponseEntity<List<SupplierSubsidiary>> findBySupplier(@RequestParam Long supplierId ) {
		List<SupplierSubsidiary> suppliers = new ArrayList<SupplierSubsidiary>();
		try {
			suppliers = supplierService.findCurrencyBySupplierId(supplierId);
			log.info("Getting the currency by supplierId " + suppliers);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException(
					"Exception while getting the  currency for make payment :: " + e.toString());
		}
		return ResponseEntity.ok(suppliers);
	}
	
	@GetMapping("/is-valid-name")
	public ResponseEntity<Boolean> validateName(@RequestParam String name) {
		return new ResponseEntity<>(this.supplierService.getValidateName(name), HttpStatus.OK);
	}
	
	/**
	 * Send's the Supplier for approval
	 * @param supplierId
	 * @return
	 */
	@GetMapping("/send-for-approval")
	public ResponseEntity<Boolean> sendForApproval(@RequestParam Long supplierId) {
		log.info("Send for approval started for Supplier ID :: " + supplierId);
		Boolean isSentForApproval = this.supplierService.sendForApproval(supplierId);
		log.info("Send for approval Finished for supploier ID :: " + supplierId);
		return new ResponseEntity<>(isSentForApproval, HttpStatus.OK);
	}
	
	/**
	 * Approve all the selected RTV's from the Approval For RTV
	 * @param supplierIds
	 * @return
	 */
	@PostMapping("/approve-all-supplier")
	public ResponseEntity<Boolean> approveAllSuppliers(@RequestBody List<Long> supplierIds) {
		log.info("Approve all supplier's is started...");
		Boolean isAllApproved = this.supplierService.approveAllSuppliers(supplierIds);
		log.info("Approve all supplier's is Finished...");
		return new ResponseEntity<>(isAllApproved, HttpStatus.OK);
	}
	
	@PostMapping("/reject-all-supplier")
	public ResponseEntity<Boolean> rejectAllSuppliers(@RequestBody List<Supplier> suppliers) {
		log.info("Reject all supplier's is started...");
		Boolean isAllRejected = this.supplierService.rejectAllSuppliers(suppliers);
		log.info("Reject all supplier's is Finished...");
		return new ResponseEntity<>(isAllRejected, HttpStatus.OK);
	}
	
	@GetMapping("/download-template")
	public HttpEntity<ByteArrayResource> downloadTemplate() {
		try {
			byte[] excelContent = this.supplierService.downloadTemplate();

			HttpHeaders header = new HttpHeaders();
			header.setContentType(new MediaType("application", "force-download"));
			header.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=supplier_template.xlsx");

			return new HttpEntity<>(new ByteArrayResource(excelContent), header);
		} catch (Exception e) {
			log.error("Something went wrong while downloading the Template. Please contact Administrator. Message : " + e.getLocalizedMessage());
			throw new CustomMessageException("Something went wrong while downloading the Template. Please contact Administrator. Message : " + e.getLocalizedMessage());
		}
	}
	
	@PostMapping("/upload")
	public HttpEntity<ByteArrayResource> uploadFile(@RequestParam("file") MultipartFile file) {
		if (ExcelHelper.hasExcelFormat(file)) {
			try {
				byte[] excelContent = this.supplierService.upload(file);

				HttpHeaders header = new HttpHeaders();
				header.setContentType(new MediaType("application", "force-download"));
				header.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=supplier_import_status.xlsx");

				return new HttpEntity<>(new ByteArrayResource(excelContent), header);
			} catch (Exception e) {
				String message = "Could not upload the file: " + file.getOriginalFilename() + "!";
				throw new CustomMessageException(message + ", Message : " + e.getLocalizedMessage());
			}
		}
		return null;
	}
	
	/*
	 * For LINE LEVEL next approver change
	 */
	@GetMapping("/update-next-approver")
	public ResponseEntity<Boolean> updateNextApproverByLine(@RequestParam Long approverId, @RequestParam Long supplierId) {
		return new ResponseEntity<>(this.supplierService.updateNextApprover(approverId, supplierId), HttpStatus.OK);
	}
	
	/**
	 * get only count of supplier by status and subsidiary
	 * @param supplierId
	 * @return currency
	 */
	@GetMapping("/get-dashboard-by-status")
	public ResponseEntity<List<GraphData>> findBysubsidiaryAndStatus(@RequestParam Long subsidiaryId ) {
		List<GraphData> suppliers = new ArrayList<GraphData>();
		try {
			suppliers = supplierService.findBysubsidiaryAndStatus(subsidiaryId);
			log.info("Getting the dashboard count " + suppliers);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException(
					"Exception while getting the dashboard count :: " + e.toString());
		}
		return ResponseEntity.ok(suppliers);
	}
	@GetMapping("/get-supplier-dashboard-by-approval-status")
	public ResponseEntity<List<GraphData>> getdashboard(@RequestParam Long SubsidiaryId) {
		List<GraphData> dashboard = supplierService.getdashboard(SubsidiaryId);
		return ResponseEntity.ok(dashboard);
	}
	
	/**
	 * send mail to the supplier give access
	 * @param supplierId
	 * @return string
	 */
	@GetMapping("/send-mail-for-password")
	public ResponseEntity<String> sendMail(@RequestParam Long supplierId) {
		log.info("Send Mail started for supplier id :: " + supplierId);
		String message = supplierService.sendMailBySupplierId(supplierId);
		log.info("Send Mail Finished for supplier id :: " + supplierId);
		return new ResponseEntity<>(message, HttpStatus.OK);
	}
	
	@GetMapping("/get-supplier-id-by-mail")
	public ResponseEntity<Long> getSupplierIdByAccessMail(@RequestParam String email) {
		log.info("Get Supplier ID by email ID :: " + email);
		Long supplierId = this.supplierService.getSupplierIdByAccessMail(email);
		log.info("Get Supplier ID by email ID Finished.");
		return new ResponseEntity<>(supplierId, HttpStatus.OK);
	}
	
	@GetMapping("/find-supplier-by-name")
	public ResponseEntity<Supplier> findByName(@RequestParam String supplierName) {
		log.info("Get Supplier for supplierName :: " + supplierName);
		Supplier supplier = supplierService.findByName(supplierName);
		log.info("Returning from find by supplierName supplier");
		return new ResponseEntity<>(supplier, HttpStatus.OK);
	}
	
	@GetMapping("/find-by-supplier-id-address-code")
	public ResponseEntity<List<SupplierAddress>> findAddressBySupplierIdAndAddressCode(@RequestParam Long supplierId, @RequestParam String addressCode) {
		log.info("findAddressBySupplierIdAndAddressCode started for supplier id :: " + supplierId);
		List<SupplierAddress> addresses = supplierService.findAddressBySupplierIdAndAddressCode(supplierId, addressCode);
		log.info("findAddressBySupplierIdAndAddressCode for supplier id :: " + supplierId);
		return new ResponseEntity<>(addresses, HttpStatus.OK);
	}
	
	@GetMapping("/find-contact-by-supplier-id-and-is-primary-contact")
	public ResponseEntity<SupplierContact> findContactBySupplierIdAndIsPrimaryContact(@RequestParam Long supplierId, @RequestParam Boolean isPrimaryContact) {
		log.info("findContactBySupplierIdAndIsPrimaryContact started for supplier id :: " + supplierId);
		SupplierContact supplierContact = supplierService.findContactBySupplierIdAndIsPrimaryContact(supplierId, isPrimaryContact);
		log.info("findContactBySupplierIdAndIsPrimaryContact for supplier id :: " + supplierId);
		return new ResponseEntity<>(supplierContact, HttpStatus.OK);
	}
	
	@PostMapping("/get-suppliers-by-ids")
	public ResponseEntity<List<Supplier>> getSuppliersByIds(@RequestBody List<Long> supplierIds) {
		log.info("getSuppliersByIds started for supplier id :: " + supplierIds);
		List<Supplier> supplierContact = supplierService.getSuppliersByIds(supplierIds);
		log.info("getSuppliersByIds for supplier id :: " + supplierIds);
		return new ResponseEntity<>(supplierContact, HttpStatus.OK);
	}
}
