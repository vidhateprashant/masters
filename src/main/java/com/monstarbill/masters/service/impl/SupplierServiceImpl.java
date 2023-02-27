package com.monstarbill.masters.service.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.monstarbill.masters.commons.AppConstants;
import com.monstarbill.masters.commons.CommonUtils;
import com.monstarbill.masters.commons.ComponentUtility;
import com.monstarbill.masters.commons.CustomBadRequestException;
import com.monstarbill.masters.commons.CustomException;
import com.monstarbill.masters.commons.CustomMessageException;
import com.monstarbill.masters.commons.ExcelHelper;
import com.monstarbill.masters.commons.FilterNames;
import com.monstarbill.masters.dao.SupplierDao;
import com.monstarbill.masters.enums.AccountTypes;
import com.monstarbill.masters.enums.FormNames;
import com.monstarbill.masters.enums.Operation;
import com.monstarbill.masters.enums.PaymentTerm;
import com.monstarbill.masters.enums.Status;
import com.monstarbill.masters.enums.TransactionStatus;
import com.monstarbill.masters.feignclient.SetupServiceClient;
import com.monstarbill.masters.models.ApprovalPreference;
import com.monstarbill.masters.models.CustomRoles;
import com.monstarbill.masters.models.GraphData;
import com.monstarbill.masters.models.Subsidiary;
import com.monstarbill.masters.models.Supplier;
import com.monstarbill.masters.models.SupplierAccess;
import com.monstarbill.masters.models.SupplierAccounting;
import com.monstarbill.masters.models.SupplierAddress;
import com.monstarbill.masters.models.SupplierContact;
import com.monstarbill.masters.models.SupplierHistory;
import com.monstarbill.masters.models.SupplierRole;
import com.monstarbill.masters.models.SupplierSubsidiary;
import com.monstarbill.masters.payload.request.ApprovalRequest;
import com.monstarbill.masters.payload.request.PaginationRequest;
import com.monstarbill.masters.payload.response.PaginationResponse;
import com.monstarbill.masters.payload.response.UserValidationRequest;
import com.monstarbill.masters.repository.AccountRepository;
import com.monstarbill.masters.repository.CustomRoleRepository;
import com.monstarbill.masters.repository.SupplierAccessRepository;
import com.monstarbill.masters.repository.SupplierAccountingRepository;
import com.monstarbill.masters.repository.SupplierAddressRepository;
import com.monstarbill.masters.repository.SupplierContactRepository;
import com.monstarbill.masters.repository.SupplierHistoryRepository;
import com.monstarbill.masters.repository.SupplierRepository;
import com.monstarbill.masters.repository.SupplierRoleRepository;
import com.monstarbill.masters.repository.SupplierSubsidiaryRepository;
import com.monstarbill.masters.service.ApprovalPreferenceService;
import com.monstarbill.masters.service.SupplierService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional
public class SupplierServiceImpl implements SupplierService {

	@Autowired
	private SupplierRepository supplierRepository;

	@Autowired
	private SupplierContactRepository supplierContactRepository;

	@Autowired
	private SupplierSubsidiaryRepository supplierSubsidiaryRepository;

	@Autowired
	private SupplierAccountingRepository supplierAccountingRepository;

	@Autowired
	private SupplierAddressRepository supplierAddressRepository;

	@Autowired
	private SupplierAccessRepository supplierAccessRepository;

	@Autowired
	private SupplierHistoryRepository supplierHistoryRepository;

	@Autowired
	private SupplierRoleRepository supplierRoleRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private SupplierDao supplierDao;

	@Autowired
	private ApprovalPreferenceService approvalPreferenceService;
	
	@Autowired
	private SetupServiceClient setupServiceClient;

	@Autowired
	private ComponentUtility componentUtility;
	
	@Autowired
	private CustomRoleRepository customRoleRepository;
	
	@Autowired
	private AccountRepository accountRepository;
	
//	@Autowired
//	private RestTemplate restTemplate;

	/**
	 * steps to save Supplier with Child's - 1. Contact 2. Subsidiary 3. Address 4.
	 * Access 5. Accounting
	 * 
	 * Maintain history in every event
	 */
	@Override
	public Supplier save(Supplier supplier) {
		Long supplierId = null;
		Optional<Supplier> oldSupplier = Optional.empty();
		try {
			// ----------------------------------- supplier started ---------------------------------
			// 1. save the supplier
			log.info("Save supplier started...");
			if (supplier.getId() == null) {
				Long subsidiaryId = null;
				List<SupplierSubsidiary> supplierSubsidiaries1 = supplier.getSupplierSubsidiary();
				if (CollectionUtils.isNotEmpty(supplierSubsidiaries1)) {
					subsidiaryId = supplierSubsidiaries1.get(0).getSubsidiaryId();
				}
				String preferenceNumber = this.setupServiceClient.getPreferenceNumber(subsidiaryId, AppConstants.SUPPLIER_MASTER);
				if (StringUtils.isEmpty(preferenceNumber)) {
					throw new CustomException("Supplier number is not generated. Please check the configurations.");
				}
				supplier.setVendorNumber(preferenceNumber);
				supplier.setApprovalStatus(TransactionStatus.OPEN.getTransactionStatus());
				supplier.setCreatedBy(setupServiceClient.getLoggedInUsername());
			} else {
				// Get the existing object using the deep copy
				oldSupplier = this.supplierRepository.findByIdAndIsDeleted(supplier.getId(), false);
				if (oldSupplier.isPresent()) {
					try {
						oldSupplier = Optional.ofNullable((Supplier) oldSupplier.get().clone());
					} catch (CloneNotSupportedException e) {
						log.error("Error while Cloning the object. Please contact administrator.");
						throw new CustomException("Error while Cloning the object. Please contact administrator.");
					}
				}
			}

			supplier.setLastModifiedBy(setupServiceClient.getLoggedInUsername());
			Supplier savedSupplier;
			try {
				savedSupplier = this.supplierRepository.save(supplier);
			} catch (DataIntegrityViolationException e) {
				log.error("Supplier unique constrain violetd." + e.getMostSpecificCause());
				throw new CustomException("Supplier unique constrain violetd :" + e.getMostSpecificCause());
			}

			supplierId = updateSupplierHistory(oldSupplier, savedSupplier);

			log.info("Save supplier Finished...");
			// ----------------------------------- supplier Finished
			// -------------------------------------------------

			// ----------------------------------- 01. supplier contact started
			// -------------------------------------------------
			log.info("Save supplier contact started...");
			// Save the contact
			List<SupplierContact> supplierContacts = supplier.getSupplierContacts();
			if (CollectionUtils.isNotEmpty(supplierContacts)) {
				for (SupplierContact supplierContact : supplierContacts) {
					saveSupplierContact(supplierId, supplierContact);
				}
			}
			savedSupplier.setSupplierContacts(supplierContacts);
			log.info("Save supplier contact Finished...");
			// ----------------------------------- 01. supplier contact Finished
			// -------------------------------------------------

			// ----------------------------------- 02. supplier Subsidiary started
			// -------------------------------------------------
			log.info("Save supplier Subsidiary started...");
			List<SupplierSubsidiary> supplierSubsidiaries = supplier.getSupplierSubsidiary();
			if (CollectionUtils.isNotEmpty(supplierSubsidiaries)) {
				for (SupplierSubsidiary supplierSubsidiary : supplierSubsidiaries) {
					this.saveSupplierSubsidiary(supplierId, supplierSubsidiary);
				}
			}
			savedSupplier.setSupplierSubsidiary(supplierSubsidiaries);
			log.info("Save supplier Subsidiary Finished...");
			// ----------------------------------- 02. supplier Subsidiary Finished
			// -------------------------------------------------

			// ----------------------------------- 03. supplier Address
			// Started-------------------------------------------------
			log.info("Save supplier Address Started...");
			List<SupplierAddress> supplierAddresses = supplier.getSupplierAddresses();
			if (CollectionUtils.isNotEmpty(supplierAddresses)) {
				for (SupplierAddress supplierAddress : supplierAddresses) {
					this.saveAddress(supplierId, supplierAddress);
				}
			}
			savedSupplier.setSupplierAddresses(supplierAddresses);
			log.info("Save supplier Address Finished...");
			// ----------------------------------- 03. supplier Address Finished
			// -------------------------------------------------

			// ----------------------------------- 04. supplier Access
			// Started-------------------------------------------------
			log.info("Save supplier Access Started...");
			SupplierAccess supplierAccess = supplier.getSupplierAccess();
			if (supplierAccess != null) {
				saveSupplierAccess(supplierAccess, supplierId);
			}
			savedSupplier.setSupplierAccess(supplierAccess);
			log.info("Save supplier Access Finished...");
			// ----------------------------------- 04. supplier Access Finished
			// -------------------------------------------------
			// ----------------------------------- 05. supplier Accounting
			// Started-------------------------------------------------
			log.info("Save supplier Accounting Started...");
			SupplierAccounting supplierAccounting = supplier.getSupplierAccounting();
			if (supplierAccounting != null) {
				saveSupplierAccounting(supplierAccounting, supplierId);
			}
			savedSupplier.setSupplierAccounting(supplierAccounting);
			log.info("Save supplier Accounting Finished...");
			// ----------------------------------- 05. supplier Accounting Finished
			// -------------------------------------------------
			System.gc();

			// savedSupplier = this.getSupplierById(savedSupplier.getId());
			return savedSupplier;
		} catch (Exception e) {
			log.error("Error while saving the supplier. Message : " + e.getLocalizedMessage());
			e.printStackTrace();
			throw new CustomException("Error while saving the supplier. Message : " + e.getLocalizedMessage());
		}
	}

	// save the supplier accounting
	private void saveSupplierAccounting(SupplierAccounting supplierAccounting, Long supplierId) {
		Optional<SupplierAccounting> oldSupplierAccounting = Optional.empty();

		if (supplierAccounting.getId() == null) {
			supplierAccounting.setCreatedBy(setupServiceClient.getLoggedInUsername());
		} else {
			// Get the existing object using the deep copy
			oldSupplierAccounting = this.supplierAccountingRepository.findByIdAndIsDeleted(supplierAccounting.getId(),
					false);
			if (oldSupplierAccounting.isPresent()) {
				try {
					oldSupplierAccounting = Optional
							.ofNullable((SupplierAccounting) oldSupplierAccounting.get().clone());
				} catch (CloneNotSupportedException e) {
					log.error("Error while Cloning the object. Please contact administrator.");
					throw new CustomException("Error while Cloning the object. Please contact administrator.");
				}
			}
		}
		supplierAccounting.setSupplierId(supplierId);
		supplierAccounting.setLastModifiedBy(setupServiceClient.getLoggedInUsername());
		SupplierAccounting supplierAccountingSaved = supplierAccountingRepository.save(supplierAccounting);

		this.updateSupplierAccountingHistory(oldSupplierAccounting, supplierAccountingSaved);
	}

	// update the history of supplier access
	private void updateSupplierAccountingHistory(Optional<SupplierAccounting> oldSupplierAccounting,
			SupplierAccounting supplierAccounting) {
		if (supplierAccounting != null) {
			if (oldSupplierAccounting.isPresent()) {
				// insert the updated fields in history table
				List<SupplierHistory> supplierHistories = new ArrayList<SupplierHistory>();
				try {
					supplierHistories = oldSupplierAccounting.get().compareFields(supplierAccounting);
					if (CollectionUtils.isNotEmpty(supplierHistories)) {
						this.supplierHistoryRepository.saveAll(supplierHistories);
					}
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					log.error("Error while comparing the new and old objects. Please contact administrator.");
					throw new CustomException(
							"Error while comparing the new and old objects. Please contact administrator.");
				}
				log.info("Supplied History is updated successfully");
			} else {
				// Insert in history table as Operation - INSERT
				this.supplierHistoryRepository.save(this.prepareSupplierHistory(supplierAccounting.getSupplierId(),
						supplierAccounting.getId(), AppConstants.SUPPLIER_ACCOUNTING, Operation.CREATE.toString(),
						supplierAccounting.getLastModifiedBy(), null, String.valueOf(supplierAccounting.getId())));
			}
			log.info("Supplier Accounting Saved successfully.");
		} else {
			log.error("Error while saving the supplier Accounting.");
			throw new CustomException("Error while saving the supplier Accounting.");
		}
	}

	// save the supplier Access
	private void saveSupplierAccess(SupplierAccess supplierAccess, Long supplierId) {
		Optional<SupplierAccess> oldSupplierAccess = Optional.empty();
		boolean isNewRecord = true;

		if (supplierAccess.getId() == null) {
			supplierAccess.setCreatedBy(setupServiceClient.getLoggedInUsername());
		} else {
			isNewRecord = false;
			// Get the existing object using the deep copy
			oldSupplierAccess = this.supplierAccessRepository.findByIdAndIsDeleted(supplierAccess.getId(), false);
			if (oldSupplierAccess.isPresent()) {
				try {
					oldSupplierAccess = Optional.ofNullable((SupplierAccess) oldSupplierAccess.get().clone());
				} catch (CloneNotSupportedException e) {
					log.error("Error while Cloning the object. Please contact administrator.");
					throw new CustomException("Error while Cloning the object. Please contact administrator.");
				}
			}
		}

		if (supplierAccess.isAccess()) {
			if (StringUtils.isEmpty(supplierAccess.getAccessMail())) {
				log.error("Supplier Access Mail should not be empty.");
				throw new CustomMessageException("Supplier Access Mail should not be empty.");
			}

			String password = supplierAccess.getPassword();
			if (StringUtils.isEmpty(password)) {
				password = this.supplierAccessRepository.getPasswordById(supplierId);
				if (StringUtils.isEmpty(password)) {
					log.error("Password cannot be empty.");
					throw new CustomMessageException("Password cannot be empty");
				}
				supplierAccess.setPlainPassword(password);
			} else {
				supplierAccess.setPlainPassword(password);
			}

			supplierAccess.setPassword(passwordEncoder.encode(password));

			if (CollectionUtils.isNotEmpty(supplierAccess.getSupplierRoles())) {
				Set<Long> roles = supplierAccess.getSupplierRoles().stream().filter(e->e.isDeleted() == false).map(e -> e.getRoleId())
						.collect(Collectors.toSet());
				UserValidationRequest user = new UserValidationRequest(isNewRecord, supplierAccess.getAccessMail(), password, new ArrayList<Long>(roles));
				this.setupServiceClient.saveUserCredentials(user);
			}
			log.info("user with roles is saved.");
		} else {
			if (!StringUtils.isEmpty(supplierAccess.getAccessMail())) {
				this.setupServiceClient.deleteUserCredentials(supplierAccess.getAccessMail());
			}
			supplierAccess.setAccessMail(null);
			supplierAccess.setPassword(null);
			supplierAccess.setPlainPassword(null);
			if (CollectionUtils.isNotEmpty(supplierAccess.getSupplierRoles())) {
				supplierAccess.getSupplierRoles().forEach((role) -> {
					role.setDeleted(true);
				});				
			}
		}
		supplierAccess.setSupplierId(supplierId);
		supplierAccess.setLastModifiedBy(setupServiceClient.getLoggedInUsername());
		SupplierAccess supplierAccessSaved = supplierAccessRepository.save(supplierAccess);

		this.updateSupplierAccessHistory(oldSupplierAccess, supplierAccessSaved);

		// save the roles
		this.saveSupplierRole(supplierAccess, supplierId);
	}

	// save the supplier roles
	private void saveSupplierRole(SupplierAccess supplierAccess, Long supplierId) {
		List<SupplierRole> supplierRoles = supplierAccess.getSupplierRoles();
		if (CollectionUtils.isNotEmpty(supplierRoles)) {
			Optional<SupplierRole> oldSupplierRole = Optional.empty();

			for (SupplierRole supplierRole : supplierRoles) {
				oldSupplierRole = Optional.empty();

				if (supplierRole.getId() == null) {
					supplierRole.setCreatedBy(setupServiceClient.getLoggedInUsername());
				} else {
					// Get the existing object using the deep copy
					oldSupplierRole = this.supplierRoleRepository.findByIdAndIsDeleted(supplierRole.getId(), false);
					if (oldSupplierRole.isPresent()) {
						try {
							oldSupplierRole = Optional.ofNullable((SupplierRole) oldSupplierRole.get().clone());
						} catch (CloneNotSupportedException e) {
							log.error("Error while Cloning the object. Please contact administrator.");
							throw new CustomException("Error while Cloning the object. Please contact administrator.");
						}
					}
				}
				supplierRole.setSupplierId(supplierId);
				supplierRole.setLastModifiedBy(setupServiceClient.getLoggedInUsername());
				SupplierRole supplierRoleSaved = this.supplierRoleRepository.save(supplierRole);

				updateSupplierRoleHistory(oldSupplierRole, supplierRoleSaved);
			}
		}
	}

	// update the history of supplier access
	private void updateSupplierRoleHistory(Optional<SupplierRole> oldSupplierRole, SupplierRole supplierRole) {
		if (supplierRole != null) {
			if (oldSupplierRole.isPresent()) {
				if (supplierRole.isDeleted()) {
					// insert the history as DELETE
					this.supplierHistoryRepository.save(this.prepareSupplierHistory(supplierRole.getSupplierId(),
							supplierRole.getId(), AppConstants.SUPPLIER_ROLE, Operation.DELETE.toString(),
							supplierRole.getLastModifiedBy(), String.valueOf(supplierRole.getId()), null));
				} else {
					// insert the updated fields in history table
					List<SupplierHistory> supplierHistories = new ArrayList<SupplierHistory>();
					try {
						supplierHistories = oldSupplierRole.get().compareFields(supplierRole);
						if (CollectionUtils.isNotEmpty(supplierHistories)) {
							this.supplierHistoryRepository.saveAll(supplierHistories);
						}
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						log.error("Error while comparing the new and old objects. Please contact administrator.");
						throw new CustomException(
								"Error while comparing the new and old objects. Please contact administrator.");
					}
					log.info("Supplied History is updated successfully");
				}
			} else {
				// Insert in history table as Operation - INSERT
				this.supplierHistoryRepository.save(this.prepareSupplierHistory(supplierRole.getSupplierId(),
						supplierRole.getId(), AppConstants.SUPPLIER_ROLE, Operation.CREATE.toString(),
						supplierRole.getLastModifiedBy(), null, String.valueOf(supplierRole.getId())));
			}
			log.info("Supplier Roles Saved successfully.");
		} else {
			log.error("Error while saving the supplier Roles.");
			throw new CustomException("Error while saving the supplier Roles.");
		}
	}

	// update the history of supplier access
	private void updateSupplierAccessHistory(Optional<SupplierAccess> oldSupplierAccess,
			SupplierAccess supplierAccess) {
		if (supplierAccess != null) {
			if (oldSupplierAccess.isPresent()) {
				// insert the updated fields in history table
				List<SupplierHistory> supplierHistories = new ArrayList<SupplierHistory>();
				try {
					supplierHistories = oldSupplierAccess.get().compareFields(supplierAccess);
					if (CollectionUtils.isNotEmpty(supplierHistories)) {
						this.supplierHistoryRepository.saveAll(supplierHistories);
					}
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					log.error("Error while comparing the new and old objects. Please contact administrator.");
					throw new CustomException(
							"Error while comparing the new and old objects. Please contact administrator.");
				}
				log.info("Supplied History is updated successfully");
			} else {
				// Insert in history table as Operation - INSERT
				this.supplierHistoryRepository.save(this.prepareSupplierHistory(supplierAccess.getSupplierId(),
						supplierAccess.getId(), AppConstants.SUPPLIER_ACCESS, Operation.CREATE.toString(),
						supplierAccess.getLastModifiedBy(), null, String.valueOf(supplierAccess.getId())));
			}
			log.info("Supplier Access Saved successfully.");
		} else {
			log.error("Error while saving the supplier Access.");
			throw new CustomException("Error while saving the supplier Access.");
		}
	}

	// Save the supplier subsidiary
	private void saveSupplierSubsidiary(Long supplierId, SupplierSubsidiary supplierSubsidiary) {
		Optional<SupplierSubsidiary> oldSupplierSubsidiary = Optional.empty();

		if (supplierSubsidiary.getId() == null) {
			supplierSubsidiary.setCreatedBy(setupServiceClient.getLoggedInUsername());
		} else {
			// Get the existing object using the deep copy
			oldSupplierSubsidiary = this.supplierSubsidiaryRepository.findByIdAndIsDeleted(supplierSubsidiary.getId(),
					false);
			if (oldSupplierSubsidiary.isPresent()) {
				try {
					oldSupplierSubsidiary = Optional
							.ofNullable((SupplierSubsidiary) oldSupplierSubsidiary.get().clone());
				} catch (CloneNotSupportedException e) {
					log.error("Error while Cloning the object. Please contact administrator.");
					throw new CustomException("Error while Cloning the object. Please contact administrator.");
				}
			}
		}
		supplierSubsidiary.setSupplierId(supplierId);
		supplierSubsidiary.setLastModifiedBy(setupServiceClient.getLoggedInUsername());
		SupplierSubsidiary savedSupplierSubsidiary = this.supplierSubsidiaryRepository.save(supplierSubsidiary);

		updateSupplierSubsidiaryHistory(oldSupplierSubsidiary, savedSupplierSubsidiary);
	}

	// save the supplier contact
	private void saveSupplierContact(Long supplierId, SupplierContact supplierContact) {
		Optional<SupplierContact> oldSupplierContact = Optional.empty();

		if (supplierContact.getId() == null) {
			supplierContact.setCreatedBy(setupServiceClient.getLoggedInUsername());
		} else {
			// Get the existing object using the deep copy
			oldSupplierContact = this.supplierContactRepository.findByIdAndIsDeleted(supplierContact.getId(), false);
			if (oldSupplierContact.isPresent()) {
				try {
					oldSupplierContact = Optional.ofNullable((SupplierContact) oldSupplierContact.get().clone());
				} catch (CloneNotSupportedException e) {
					log.error("Error while Cloning the object. Please contact administrator.");
					throw new CustomException("Error while Cloning the object. Please contact administrator.");
				}
			}
		}

		supplierContact.setSupplierId(supplierId);
		supplierContact.setLastModifiedBy(setupServiceClient.getLoggedInUsername());
		SupplierContact supplierContactSaved = supplierContactRepository.save(supplierContact);

		updateSupplierContactHistory(oldSupplierContact, supplierContactSaved);
	}

	// update the history of the supplier Subsidiary
	private void updateSupplierSubsidiaryHistory(Optional<SupplierSubsidiary> oldSupplierSubsidiary,
			SupplierSubsidiary supplierSubsidiary) {
		if (supplierSubsidiary != null) {
			if (oldSupplierSubsidiary.isPresent()) {
				// insert the updated fields in history table
				List<SupplierHistory> supplierHistories = new ArrayList<SupplierHistory>();
				try {
					supplierHistories = oldSupplierSubsidiary.get().compareFields(supplierSubsidiary);
					if (CollectionUtils.isNotEmpty(supplierHistories)) {
						this.supplierHistoryRepository.saveAll(supplierHistories);
					}
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					log.error("Error while comparing the new and old objects. Please contact administrator.");
					throw new CustomException(
							"Error while comparing the new and old objects. Please contact administrator.");
				}
				log.info("Supplied History is updated successfully");
			} else {
				// Insert in history table as Operation - INSERT
				this.supplierHistoryRepository.save(this.prepareSupplierHistory(supplierSubsidiary.getSupplierId(),
						supplierSubsidiary.getId(), AppConstants.SUPPLIER_SUBSIDIARY, Operation.CREATE.toString(),
						supplierSubsidiary.getLastModifiedBy(), null, String.valueOf(supplierSubsidiary.getId())));
			}
			log.info("Supplier subsidiary Saved successfully.");
		} else {
			log.error("Error while saving the supplier Subsidiary.");
			throw new CustomException("Error while saving the supplier Subsidiary.");
		}
	}

	// update history of the supplier Contact
	private void updateSupplierContactHistory(Optional<SupplierContact> oldSupplierContact,
			SupplierContact supplierContact) {
		if (supplierContact != null) {
			if (oldSupplierContact.isPresent()) {
				if (supplierContact.isDeleted()) {
					// Insert in history table as Operation - DELETE
					this.supplierHistoryRepository.save(this.prepareSupplierHistory(supplierContact.getSupplierId(),
							supplierContact.getId(), AppConstants.SUPPLIER_CONTACT, Operation.DELETE.toString(),
							supplierContact.getLastModifiedBy(), String.valueOf(supplierContact.getId()), null));
				} else {
					// insert the updated fields in history table
					List<SupplierHistory> supplierHistories = new ArrayList<SupplierHistory>();
					try {
						supplierHistories = oldSupplierContact.get().compareFields(supplierContact);
						if (CollectionUtils.isNotEmpty(supplierHistories)) {
							this.supplierHistoryRepository.saveAll(supplierHistories);
						}
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						log.error("Error while comparing the new and old objects. Please contact administrator.");
						throw new CustomException(
								"Error while comparing the new and old objects. Please contact administrator.");
					}
				}
				log.info("Supplied History is updated successfully");
			} else {
				// Insert in history table as Operation - INSERT
				this.supplierHistoryRepository.save(this.prepareSupplierHistory(supplierContact.getSupplierId(),
						supplierContact.getId(), AppConstants.SUPPLIER_CONTACT, Operation.CREATE.toString(),
						supplierContact.getLastModifiedBy(), null, String.valueOf(supplierContact.getId())));
			}

			log.info("Supplier Contact Saved successfully.");
		} else {
			log.error("Error while saving the supplier Contact.");
			throw new CustomException("Error while saving the supplier Contact..");
		}
	}

	// update history of supplier
	private Long updateSupplierHistory(Optional<Supplier> oldSupplier, Supplier supplier) {
		Long supplierId = null;
		if (supplier != null) {
			supplierId = supplier.getId();

			if (oldSupplier.isPresent()) {
				// insert the updated fields in history table
				List<SupplierHistory> supplierHistories = new ArrayList<SupplierHistory>();
				try {
					supplierHistories = oldSupplier.get().compareFields(supplier);
					if (CollectionUtils.isNotEmpty(supplierHistories)) {
						this.supplierHistoryRepository.saveAll(supplierHistories);
					}
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					log.error("Error while comparing the new and old objects. Please contact administrator.");
					throw new CustomException(
							"Error while comparing the new and old objects. Please contact administrator.");
				}
				log.info("Supplied History is updated successfully");
			} else {
				// Insert in history table as Operation - INSERT
				this.supplierHistoryRepository.save(this.prepareSupplierHistory(supplierId, null, AppConstants.SUPPLIER,
						Operation.CREATE.toString(), supplier.getLastModifiedBy(), null, null));
			}
			log.info("Supplier History Saved successfully.");
		} else {
			log.error("Error while saving the supplier.");
			throw new CustomException("Error while saving the supplier.");
		}
		return supplierId;
	}

	@Override
	public Supplier getSupplierById(Long id) {
		Optional<Supplier> supplier = Optional.ofNullable(new Supplier());
		supplier = supplierRepository.findByIdAndIsDeleted(id, false);

		if (supplier.isPresent()) {
			Long supplierId = supplier.get().getId();
			Long subsidiaryId = null;

			// get the contacts
			List<SupplierContact> supplierContacts = new ArrayList<SupplierContact>();
			supplierContacts = supplierContactRepository.findBySupplierIdAndIsDeleted(supplierId, false);
			if (CollectionUtils.isNotEmpty(supplierContacts)) {
				supplier.get().setSupplierContacts(supplierContacts);
			}

			// 2. get subsidiary
			List<SupplierSubsidiary> supplierSubsidiaries = supplierSubsidiaryRepository
					.findBySupplierIdAndDeletedWithName(supplierId, false);
			if (CollectionUtils.isNotEmpty(supplierSubsidiaries)) {
				supplier.get().setSupplierSubsidiary(supplierSubsidiaries);
				subsidiaryId = supplierSubsidiaries.get(0).getSubsidiaryId();
			}

			// 3. Get Address
			List<SupplierAddress> supplierAddress = supplierAddressRepository.findBySupplierIdAndIsDeleted(supplierId,
					false);
			if (CollectionUtils.isNotEmpty(supplierAddress)) {
				supplier.get().setSupplierAddresses(supplierAddress);
			}

			// 4. Get Access & it's Roles
			Optional<SupplierAccess> supplierAccess = Optional.empty();
			supplierAccess = supplierAccessRepository.findBySupplierIdAndIsDeleted(supplierId, false);
			List<SupplierRole> supplierRoles = new ArrayList<SupplierRole>();
			if (supplierAccess.isPresent()) {
				supplierRoles = this.supplierRoleRepository.findBySupplierIdAndIsDeleted(supplierId, false);
				supplierAccess.get().setSupplierRoles(supplierRoles);
//				supplierAccess.get().setPassword(null);
//				supplierAccess.get().setPlainPassword(null);
				supplier.get().setSupplierAccess(supplierAccess.get());
			}

			// 5. Get Accounting
			Optional<SupplierAccounting> supplierAccounting = this.supplierAccountingRepository
					.findBySupplierIdAndIsDeleted(supplierId, false);
			if (supplierAccounting.isPresent()) {
				supplier.get().setSupplierAccounting(supplierAccounting.get());
			}

			boolean isRoutingActive = this.findIsApprovalRoutingActive(subsidiaryId);
			if (isRoutingActive) {
				String status = supplier.get().getApprovalStatus();
				if (!TransactionStatus.OPEN.getTransactionStatus().equalsIgnoreCase(status) && !TransactionStatus.REJECTED.getTransactionStatus().equalsIgnoreCase(status)) {
					isRoutingActive = false;
				}
			}
			supplier.get().setApprovalRoutingActive(isRoutingActive);
		} else {
			throw new CustomMessageException("Supplier Not Found against given supplier id : " + id);
		}

		return supplier.get();
	}

	/**
	 * Find the approval routing is active or not based on the subsidiary Id
	 * 
	 * @param subsidiaryId
	 * @return
	 */
	private boolean findIsApprovalRoutingActive(Long subsidiaryId) {
		return this.componentUtility.findIsApprovalRoutingActive(subsidiaryId, FormNames.SUPPLIER.getFormName());
	}

	@Override
	public SupplierSubsidiary saveSubsidiary(SupplierSubsidiary supplierSubsidiary) {
		boolean isNew = false;
		if (supplierSubsidiary.getId() == null) {
			isNew = true;
			supplierSubsidiary.setCreatedBy(setupServiceClient.getLoggedInUsername());
		}
		supplierSubsidiary.setLastModifiedBy(setupServiceClient.getLoggedInUsername());
		supplierSubsidiary = this.supplierSubsidiaryRepository.save(supplierSubsidiary);

		if (supplierSubsidiary == null) {
			throw new CustomMessageException("Error while adding the subsidiary in supplier.");
		}

		// As per requirement, there is not Update operation in supplier-subsidiary
		if (isNew) {
			this.supplierHistoryRepository.save(this.prepareSupplierHistory(supplierSubsidiary.getSupplierId(),
					supplierSubsidiary.getId(), AppConstants.SUPPLIER_SUBSIDIARY, Operation.CREATE.toString(),
					supplierSubsidiary.getLastModifiedBy(), String.valueOf(supplierSubsidiary.getId()), null));
		}

		return supplierSubsidiary;
	}

	@Override
	public Long findSubsidiaryBySupplierId(Long id) {
		List<SupplierSubsidiary> supplierSubsidiaries = new ArrayList<SupplierSubsidiary>();
		supplierSubsidiaries = this.supplierSubsidiaryRepository.findBySupplierIdAndDeletedWithName(id, false);
		if (CollectionUtils.isEmpty(supplierSubsidiaries)) {
			log.error("Supplier is not associated with any subsidiary.");
			throw new CustomMessageException("Supplier is not associated with any subsidiary.");
		}
		return supplierSubsidiaries.get(0).getSubsidiaryId();
	}

	/**
	 * Performing the soft delete only
	 */
	@Override
	public Boolean deleteSupplierSubsidiary(SupplierSubsidiary supplierSubsidiary) {
		Long supplierId = supplierSubsidiary.getSupplierId();
		Long subsidiaryId = supplierSubsidiary.getSubsidiaryId();

		Optional<SupplierSubsidiary> subsidiary = Optional.ofNullable(new SupplierSubsidiary());
		subsidiary = this.supplierSubsidiaryRepository.findBySupplierIdAndSubsidiaryId(supplierId, subsidiaryId);

		if (!subsidiary.isPresent()) {
			log.info("Subsidiary : " + subsidiaryId + " is NOT found against supplier : " + supplierId);
			throw new CustomMessageException(
					"Subsidiary : " + subsidiaryId + " is NOT found against supplier : " + supplierId + " to delete.");
		}

		subsidiary.get().setDeleted(true);
		subsidiary = Optional.ofNullable(this.supplierSubsidiaryRepository.save(subsidiary.get()));

		if (!subsidiary.isPresent()) {
			log.info("Error to delete the Subsidiary : " + subsidiaryId + " of supplier : " + supplierId);
			throw new CustomMessageException(
					"Error to delete the Subsidiary : " + subsidiaryId + " of supplier : " + supplierId);
		}

		// insert in history
		this.supplierHistoryRepository.save(this.prepareSupplierHistory(subsidiary.get().getSupplierId(),
				subsidiary.get().getId(), AppConstants.SUPPLIER_SUBSIDIARY, Operation.DELETE.toString(),
				subsidiary.get().getLastModifiedBy(), String.valueOf(subsidiary.get().getId()), null));

		log.info("Subsidiary deleted successfully. ");
		return true;
	}

	public SupplierAddress saveAddress(Long supplierId, SupplierAddress supplierAddress) {
		Optional<SupplierAddress> oldSupplierAddress = Optional.empty();

		if (supplierAddress.getId() == null) {
			supplierAddress.setCreatedBy(setupServiceClient.getLoggedInUsername());
		} else {
			// Get existing address using deep copy
			oldSupplierAddress = this.supplierAddressRepository.findByIdAndIsDeleted(supplierAddress.getId(), false);
			if (oldSupplierAddress.isPresent()) {
				try {
					oldSupplierAddress = Optional.ofNullable((SupplierAddress) oldSupplierAddress.get().clone());
				} catch (CloneNotSupportedException e) {
					log.error("Error while Cloning the object. Please contact administrator.");
					throw new CustomException("Error while Cloning the object. Please contact administrator.");
				}
			}
		}
		supplierAddress.setSupplierId(supplierId);
		supplierAddress.setLastModifiedBy(setupServiceClient.getLoggedInUsername());

		if ("Registered".equalsIgnoreCase(supplierAddress.getRegistrationType())
				&& StringUtils.isEmpty(supplierAddress.getTaxRegistrationNumber())) {
			log.error("Tax Registration Number under : " + supplierAddress.getRegistrationType() + " : cannot be null ");
			throw new CustomMessageException("Tax Registration Number under : " + supplierAddress.getRegistrationType() + " : cannot be null ");
		}

		supplierAddress = this.supplierAddressRepository.save(supplierAddress);

		if (supplierAddress == null) {
			log.info("Error while Saving the Address in supplier.");
			throw new CustomMessageException("Error while Saving the Address in supplier.");
		}
		// find the updated values and save to history table
		if (oldSupplierAddress.isPresent()) {
			if (supplierAddress.isDeleted()) {
				this.supplierHistoryRepository.save(this.prepareSupplierHistory(supplierAddress.getSupplierId(),
						supplierAddress.getId(), AppConstants.SUPPLIER_ADDRESS, Operation.DELETE.toString(),
						supplierAddress.getLastModifiedBy(), String.valueOf(supplierAddress.getId()), null));
			} else {
				List<SupplierHistory> supplierHistories = new ArrayList<SupplierHistory>();
				try {
					supplierHistories = oldSupplierAddress.get().compareFields(supplierAddress);
					if (CollectionUtils.isNotEmpty(supplierHistories)) {
						this.supplierHistoryRepository.saveAll(supplierHistories);
					}
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					log.error("Error while comparing the new and old objects. Please contact administrator.");
					throw new CustomException(
							"Error while comparing the new and old objects. Please contact administrator.");
				}
			}
			log.info("Supplied History is updated successfully for ");
		} else {
			this.supplierHistoryRepository.save(this.prepareSupplierHistory(supplierAddress.getSupplierId(),
					supplierAddress.getId(), AppConstants.SUPPLIER_ADDRESS, Operation.CREATE.toString(),
					supplierAddress.getLastModifiedBy(), null, String.valueOf(supplierAddress.getId())));
		}

		return supplierAddress;
	}

	@Override
	public List<SupplierAddress> findAddressBySupplierId(Long supplierId) {
		List<SupplierAddress> supplierAddresses = new ArrayList<SupplierAddress>();
		supplierAddresses = this.supplierAddressRepository.findBySupplierIdAndIsDeleted(supplierId, false);
		return supplierAddresses;
	}

	/**
	 * Performing the soft delete only
	 */
	@Override
	public Boolean deleteSupplierAddress(SupplierAddress supplierAddress) {
		Long supplierId = supplierAddress.getSupplierId();
		Long id = supplierAddress.getId();

		if (id == null) {
			throw new CustomMessageException("Address ID should not be null.");
		}

		Optional<SupplierAddress> address = Optional.empty();
		address = this.supplierAddressRepository.findById(id);

		if (!address.isPresent()) {
			log.info("Address : " + id + " is NOT found against supplier : " + supplierId);
			throw new CustomMessageException(
					"Address : " + id + " is NOT found against supplier : " + supplierId + " to delete.");
		}
		if (supplierId != address.get().getSupplierId()) {
			log.info("Supplier ID : " + supplierId + " is NOT Matched against given address : " + id);
			throw new CustomMessageException(
					"Supplier ID : " + supplierId + " is NOT Matched against given address : " + id);
		}
		address.get().setDeleted(true);

		address = Optional.ofNullable(this.supplierAddressRepository.save(address.get()));

		if (!address.isPresent()) {
			log.info("Error while deleting the address : " + id + " against given Supplier : " + supplierId);
			throw new CustomMessageException(
					"Error while deleting the address : " + id + " against given Supplier : " + supplierId);
		}

		// insert the history
		this.supplierHistoryRepository.save(this.prepareSupplierHistory(address.get().getSupplierId(),
				address.get().getId(), AppConstants.SUPPLIER_ADDRESS, Operation.DELETE.toString(),
				address.get().getLastModifiedBy(), String.valueOf(address.get().getId()), null));

		log.info("Address : " + id + " is deleted against supplier : " + supplierId);
		return true;
	}

	@Override
	public List<SupplierHistory> findAuditById(Long id, Pageable pageable) {
		List<SupplierHistory> histories = supplierHistoryRepository.findBySupplierIdOrderById(id, pageable);
		String createdBy = histories.get(0).getLastModifiedBy();
		histories.forEach(e->{
			e.setCreatedBy(createdBy);
		});
		return histories;
	}

	/**
	 * Prashant : 01-Jul-2022 Prepares the history objects for all forms and their
	 * child. Use this if you need single object only
	 * 
	 * @param moduleName
	 * @param operation
	 * @param lastModifiedBy
	 * @param oldValue
	 * @param newValue
	 * @return
	 */
	public SupplierHistory prepareSupplierHistory(Long supplierId, Long childId, String moduleName, String operation,
			String lastModifiedBy, String oldValue, String newValue) {
		SupplierHistory supplierHistory = new SupplierHistory();
		supplierHistory.setSupplierId(supplierId);
		supplierHistory.setChildId(childId);
		supplierHistory.setModuleName(moduleName);
		supplierHistory.setChangeType(AppConstants.UI);
		supplierHistory.setOperation(operation);
		supplierHistory.setOldValue(oldValue);
		supplierHistory.setNewValue(newValue);
		supplierHistory.setLastModifiedBy(lastModifiedBy);
		return supplierHistory;
	}

	@Override
	public List<Supplier> findAllSuppliers() {
		return this.supplierRepository.findAllSuppliers(false);
	}

	@Override
	public List<Supplier> getSuppliersBySubsidiaryId(Long subsidiaryId) {
		List<Supplier> suppliers = new ArrayList<Supplier>();
		suppliers = this.supplierRepository.findBySubsidiaryId(subsidiaryId);
		return suppliers;
	}

	@Override
	public PaginationResponse findAll(PaginationRequest paginationRequest) {
		List<Supplier> supplier = new ArrayList<Supplier>();

		// preparing where clause
		String whereClause = this.prepareWhereClause(paginationRequest).toString();

		// get list
		supplier = this.supplierDao.findAll(whereClause, paginationRequest);

		// getting count
		Long totalRecords = this.supplierDao.getCount(whereClause);

		return CommonUtils.setPaginationResponse(paginationRequest.getPageNumber(), paginationRequest.getPageSize(),
				supplier, totalRecords);
	}

	private StringBuilder prepareWhereClause(PaginationRequest paginationRequest) {
		Map<String, ?> filters = paginationRequest.getFilters();

		String subsidiaryName = null;
		String name = null;
		String type = null;
		String number = null;
		String pan = null;
		boolean isActive = false;
		String status = null;

		if (filters.containsKey(FilterNames.SUBSIDIARY_NAME))
			subsidiaryName = (String) filters.get(FilterNames.SUBSIDIARY_NAME);
		if (filters.containsKey(FilterNames.NAME))
			name = (String) filters.get(FilterNames.NAME);
		if (filters.containsKey(FilterNames.TYPE))
			type = (String) filters.get(FilterNames.TYPE);
		if (filters.containsKey(FilterNames.NUMBER))
			number = (String) filters.get(FilterNames.NUMBER);
		if (filters.containsKey(FilterNames.PAN))
			pan = (String) filters.get(FilterNames.PAN);
		if (filters.containsKey(FilterNames.STATUS)) {
			status = (String) filters.get(FilterNames.STATUS);
			if (Status.ACTIVE.toString().equalsIgnoreCase(status)) {
				isActive = true;
			} else {
				isActive = false;
			}
		}

		StringBuilder whereClause = new StringBuilder(" AND s.isDeleted is false ");
		if (StringUtils.isNotEmpty(subsidiaryName)) {
			whereClause.append(" AND lower(sub.name) like lower ('%").append(subsidiaryName).append("%')");
		}
		if (StringUtils.isNotEmpty(name)) {
			whereClause.append(" AND lower(s.name) like lower('%").append(name).append("%')");
		}
		if (StringUtils.isNotEmpty(type)) {
			whereClause.append(" AND lower(s.vendorType) like lower('%").append(type).append("%')");
		}
		if (StringUtils.isNotEmpty(number)) {
			whereClause.append(" AND lower(s.vendorNumber) like lower('%").append(number).append("%')");
		}
		if (StringUtils.isNotEmpty(pan)) {
			whereClause.append(" AND lower(s.pan) like lower('%").append(pan).append("%')");
		}
		if (StringUtils.isNotEmpty(status)) {
			whereClause.append(" AND s.isActive is ").append(isActive);
		}
		return whereClause;
	}

	@Override
	public List<Supplier> getSupplierApproval(String user) {
		List<String> status = new ArrayList<String>();
		status.add(TransactionStatus.PENDING_APPROVAL.getTransactionStatus());
		status.add(TransactionStatus.PARTIALLY_APPROVED.getTransactionStatus());
		List<Supplier> suppliers = new ArrayList<Supplier>();
		suppliers = this.supplierRepository.findAllBySupplierStatus(status, user);
		log.info("supplier are for approval process " + suppliers);
		return suppliers;
	}

	@Override
	public List<SupplierSubsidiary> findCurrencyBySupplierId(Long supplierId) {
		List<Supplier> suppliers = new ArrayList<Supplier>();
		suppliers = this.supplierRepository.getAllCurrencyBySupplier(supplierId, false);
		
		List<SupplierSubsidiary> supplierSubsidiaries = this.supplierSubsidiaryRepository.findBySupplierIdAndDeletedWithName(supplierId, false);
		log.info("Get all currency  by subsidary id and type ." + suppliers);
		return supplierSubsidiaries;
	}

	@Override
	public Boolean getValidateName(String name) {
		// if name is empty then name is not valid
		if (StringUtils.isEmpty(name))
			return false;

		Long countOfRecordsWithSameName = this.supplierRepository.getCountByName(name.trim());
		// if we we found the count greater than 0 then it is not valid. If it is zero
		// then it is valid string
		if (countOfRecordsWithSameName > 0)
			return false;
		else
			return true;
	}

	@Override
	public Boolean sendForApproval(Long id) {
		Boolean isSentForApproval = false;

		try {
			/**
			 * Due to single transaction we are getting updated value when we find from repo
			 * after the update hence finding old one first
			 */
			// Get the existing object using the deep copy
			Optional<Supplier> oldSupplier = this.findOldDeepCopiedSupplier(id);

			Optional<Supplier> supplier = Optional.empty();
			supplier = this.findById(id);

			/**
			 * Check routing is active or not
			 */
			boolean isRoutingActive = this.findIsApprovalRoutingActive(supplier.get().getSubsidiaryId());
			if (!isRoutingActive) {
				log.error("Routing is not active for the supplier : " + id + ". Please update your configuration. ");
				throw new CustomMessageException(
						"Routing is not active for the supplier : " + id + ". Please update your configuration. ");
			}

			String natureOfSupply = supplier.get().getNatureOfSupply();
			log.info("Nature Of supply for Supplier is :: " + natureOfSupply);

			Long subsidiaryId = this.findSubsidiaryBySupplierId(id);
			log.info("Subsidiary for the supplier is :: " + subsidiaryId);

			ApprovalRequest approvalRequest = new ApprovalRequest();
			approvalRequest.setSubsidiaryId(subsidiaryId);
			approvalRequest.setFormName(FormNames.SUPPLIER.getFormName());
			approvalRequest.setNatureOfSupply(natureOfSupply);
			log.info("Approval object is prepared : " + approvalRequest.toString());

			/**
			 * method will give max level & it's sequence if match otherwise throw error
			 * message as no approver process exist if level or sequence id is null then
			 * also throws error message.
			 */
			ApprovalPreference approvalPreference = this.approvalPreferenceService
					.findApproverMaxLevel(approvalRequest);
			Long sequenceId = approvalPreference.getSequenceId();
			String level = approvalPreference.getLevel();
			Long approverPreferenceId = approvalPreference.getId();
			log.info("Max level & sequence is found :: " + approvalPreference.toString());

			supplier.get().setApproverSequenceId(sequenceId);
			supplier.get().setApproverMaxLevel(level);
			supplier.get().setApproverPreferenceId(approverPreferenceId);

			String levelToFindRole = "L1";
			if (AppConstants.APPROVAL_TYPE_INDIVIDUAL.equals(approvalPreference.getApprovalType())) {
				levelToFindRole = level;
			}
			approvalRequest = this.approvalPreferenceService.findApproverByLevelAndSequence(approverPreferenceId,
					levelToFindRole, sequenceId);

			this.updateApproverDetailsInSupplier(supplier, approvalRequest);
			supplier.get().setApprovalStatus(TransactionStatus.PENDING_APPROVAL.getTransactionStatus());
			log.info("Approver is found and details is updated for RTV :: " + supplier.get());

			this.saveSupplierForApproval(supplier.get(), oldSupplier);
			log.info("RTV is saved successfully with Approver details.");

			componentUtility.sendEmailByApproverId(supplier.get().getNextApprover(), FormNames.SUPPLIER.getFormName());

			isSentForApproval = true;
		} catch (Exception e) {
			log.error("Error while sending Supplier for approval for id - " + id);
			e.printStackTrace();
			throw new CustomMessageException("Error while sending Supplier for approval for id - " + id + ", Message : "
					+ e.getLocalizedMessage());
		}

		return isSentForApproval;
	}

	/**
	 * Save RTV after the approval details change
	 * 
	 * @param supplier
	 */
	private void saveSupplierForApproval(Supplier supplier, Optional<Supplier> oldSupplier) {
		supplier.setLastModifiedBy(setupServiceClient.getLoggedInUsername());
		supplier = this.supplierRepository.save(supplier);

		if (supplier == null) {
			log.info("Error while saving the Supplier after the Approval.");
			throw new CustomMessageException("Error while saving the Supplier after the Approval.");
		}
		log.info("Supplier saved successfully :: " + supplier.getSubsidiaryId());

		// update the data in PR history table
		this.updateSupplierHistory(oldSupplier, supplier);
		log.info("Rtv history is updated. after approval change.");
	}

	/**
	 * Set/Prepares the approver details in the RTV object
	 * 
	 * @param purchaseRequisition
	 * @param approvalRequest
	 */
	private void updateApproverDetailsInSupplier(Optional<Supplier> supplier, ApprovalRequest approvalRequest) {
		supplier.get().setApprovedBy(supplier.get().getNextApprover());
		supplier.get().setNextApprover(approvalRequest.getNextApprover());
		supplier.get().setNextApproverRole(approvalRequest.getNextApproverRole());
		supplier.get().setNextApproverLevel(approvalRequest.getNextApproverLevel());
	}

	private Optional<Supplier> findById(Long id) {
		Optional<Supplier> supplier = Optional.empty();
		supplier = this.supplierRepository.findByIdAndIsDeleted(id, false);

		if (!supplier.isPresent()) {
			log.error("Supplier Not Found against given Supplier id : " + id);
			throw new CustomMessageException("Supplier Not Found against given Supplier id : " + id);
		}

		// find subsidiary
		List<SupplierSubsidiary> supplierSubsidiary = supplierSubsidiaryRepository
				.findBySupplierIdAndDeletedWithName(supplier.get().getId(), false);
		if (CollectionUtils.isNotEmpty(supplierSubsidiary)) {
			supplier.get().setSubsidiaryId(supplierSubsidiary.get(0).getSubsidiaryId());
		}

		return supplier;
	}

	private Optional<Supplier> findOldDeepCopiedSupplier(Long id) {
		Optional<Supplier> supplier = this.supplierRepository.findByIdAndIsDeleted(id, false);
		if (supplier.isPresent()) {
			try {
				supplier = Optional.ofNullable((Supplier) supplier.get().clone());
				log.info("Existing Rtv is copied.");
			} catch (CloneNotSupportedException e) {
				log.error("Error while Cloning the object. Please contact administrator.");
				throw new CustomException("Error while Cloning the object. Please contact administrator.");
			}
		}
		return supplier;
	}

	@Override
	public Boolean approveAllSuppliers(List<Long> supplierIds) {
		Boolean isAllPoApproved = false;
		try {

			for (Long supplierId : supplierIds) {
				log.info("Approval Process is started for supplier-id :: " + supplierId);

				/**
				 * Due to single transaction we are getting updated value when we find from repo
				 * after the update hence finding old one first
				 */
				// Get the existing object using the deep copy
				Optional<Supplier> oldSupplier = this.findOldDeepCopiedSupplier(supplierId);

				Optional<Supplier> supplier = Optional.empty();
				supplier = this.findById(supplierId);

				/**
				 * Check routing is active or not
				 */
				boolean isRoutingActive = this.findIsApprovalRoutingActive(supplier.get().getSubsidiaryId());
				if (!isRoutingActive) {
					log.error("Routing is not active for the supplier : " + supplierId
							+ ". Please update your configuration. ");
					throw new CustomMessageException("Routing is not active for the supplier : " + supplierId
							+ ". Please update your configuration. ");
				}

				if (TransactionStatus.APPROVED.getTransactionStatus()
						.equalsIgnoreCase(supplier.get().getApprovalStatus())) {
					continue;
				}

				// meta data
				Long approvalPreferenceId = supplier.get().getApproverPreferenceId();
				Long sequenceId = supplier.get().getApproverSequenceId();
				String maxLevel = supplier.get().getApproverMaxLevel();

				String approvalPreferenceType = this.approvalPreferenceService
						.findPreferenceTypeById(approvalPreferenceId);

				ApprovalRequest approvalRequest = new ApprovalRequest();

				if (AppConstants.APPROVAL_TYPE_CHAIN.equalsIgnoreCase(approvalPreferenceType)
						&& !maxLevel.equals(supplier.get().getNextApproverLevel())) {
					Long currentLevelNumber = Long
							.parseLong(supplier.get().getNextApproverLevel().replaceFirst("L", "")) + 1;
					String currentLevel = "L" + currentLevelNumber;
					approvalRequest = this.approvalPreferenceService
							.findApproverByLevelAndSequence(approvalPreferenceId, currentLevel, sequenceId);
					supplier.get().setApprovalStatus(TransactionStatus.PARTIALLY_APPROVED.getTransactionStatus());
				} else {
					supplier.get().setApprovalStatus(TransactionStatus.APPROVED.getTransactionStatus());
				}
				log.info("Approval Request is found for Supplier :: " + approvalRequest.toString());

				this.updateApproverDetailsInSupplier(supplier, approvalRequest);
				log.info("Approver is found and details is updated :: " + supplier.get());

				this.saveSupplierForApproval(supplier.get(), oldSupplier);
				log.info("Supplier is saved successfully with Approver details.");

				componentUtility.sendEmailByApproverId(supplier.get().getNextApprover(),
						FormNames.SUPPLIER.getFormName());

				log.info("Approval Process is Finished for Supplier :: " + supplier.get().getId());
			}

			isAllPoApproved = true;
		} catch (Exception e) {
			log.error("Error while approving the supplier.");
			e.printStackTrace();
			throw new CustomMessageException(
					"Error while approving the supplier. Message : " + e.getLocalizedMessage());
		}
		return isAllPoApproved;
	}

	@Override
	public byte[] upload(MultipartFile file) {
		try {
			return this.importSuppliersFromExcel(file);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException("Something went wrong. Please Contact Administrator. Error : " + e.getLocalizedMessage());
		}
	}

	@SuppressWarnings("deprecation")
	public byte[] importSuppliersFromExcel(MultipartFile inputFile) {
		try {
			InputStream inputStream = inputFile.getInputStream();
			@SuppressWarnings("resource")
			Workbook workbook = new XSSFWorkbook(inputStream);
			Sheet sheet = workbook.getSheet("Sheet1");
			Iterator<Row> rows = sheet.iterator();

			int statusColumnNumber = 0;
			int rowNumber = 0;

			boolean isError = false;
			StringBuilder errorMessage = new StringBuilder();

			Map<String, Supplier> supplierMapping = new TreeMap<String, Supplier>();

			while (rows.hasNext()) {
				isError = false;
				errorMessage = new StringBuilder();
				int errorCount = 1;
				Row inputCurrentRow = rows.next();
				if (rowNumber == 0) {
					statusColumnNumber = inputCurrentRow.getLastCellNum();
					Cell cell = inputCurrentRow.createCell(statusColumnNumber);
					cell.setCellValue("Imported Status");
					rowNumber++;
					continue;
				}

				boolean isRowEmpty = ExcelHelper.checkIfRowIsEmpty(inputCurrentRow);

				// if row is empty it means all records completed.
				if (isRowEmpty)
					break;

				Supplier supplier = new Supplier();
				String externalId = null;
				// External ID - REQUIRED
				try {
					if (inputCurrentRow.getCell(0) != null) {
						externalId = new DataFormatter().formatCellValue(inputCurrentRow.getCell(0));
//						externalId = inputCurrentRow.getCell(0).getStringCellValue();
					} else {
//						errorMessage.append(errorCount + ") External ID should not be empty. ");
						log.error("External ID should not be empty.");
//						isError = true;
//						errorCount++;
						continue;
//						throw new CustomException("External ID shoudl not empty.");
					}
				} catch (Exception e) {
					log.error("Exception External ID " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of External ID is invalid.");
					isError = true;
					errorCount++;
					throw new CustomException("External ID should not be empty.");
				}
				// ----------------- supplier header fields STARTED -----------------------
				if (supplierMapping.containsKey(externalId)) {
					supplier = supplierMapping.get(externalId);
				}
				supplier.setExternalId(externalId);
				// Vendor Name - REQUIRED
				try {
					if (inputCurrentRow.getCell(1) != null) {
						String vendorName = inputCurrentRow.getCell(1).getStringCellValue();
						supplier.setName(vendorName);
					} else {
						errorMessage.append(errorCount + ") Vendor Name is required. ");
						log.error("Vendor Name is required.");
						isError = true;
						errorCount++;
					}
				} catch (Exception e) {
					log.error("Exception subsidiary " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Vendor Name is invalid.");
					isError = true;
					errorCount++;
				}

				// Legal Name
				try {
					if (inputCurrentRow.getCell(2) != null) {
						supplier.setLegalName(inputCurrentRow.getCell(2).getStringCellValue());
					}
				} catch (Exception e) {
					log.error("Exception Legal Name " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Legal Name is invalid.");
					isError = true;
					errorCount++;
				}

				// Vendor Type - REQUIRED
				try {
					if (inputCurrentRow.getCell(3) != null) {
						supplier.setVendorType(inputCurrentRow.getCell(3).getStringCellValue());
					} else {
						errorMessage.append(errorCount + ") Vendor Type is required.");
						log.error("Vendor Type is required.");
						isError = true;
						errorCount++;
					}
				} catch (Exception e) {
					log.error("Exception Vendor Type " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Vendor Type is invalid.");
					isError = true;
					errorCount++;
				}

				// Nature of Supply
				try {
					if (inputCurrentRow.getCell(4) != null) {
						supplier.setNatureOfSupply(inputCurrentRow.getCell(4).getStringCellValue());
					}
				} catch (Exception e) {
					log.error("Exception Nature of Supply " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Nature of Supply is invalid.");
					isError = true;
					errorCount++;
				}

				// UIN
				try {
					if (inputCurrentRow.getCell(5) != null) {
						supplier.setUin(inputCurrentRow.getCell(5).getStringCellValue());
					}
				} catch (Exception e) {
					log.error("Exception Unique Idenfication Number " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Unique Idenfication Number is invalid.");
					isError = true;
					errorCount++;
				}

				// Invoice Receiving Mail
				try {
					if (inputCurrentRow.getCell(6) != null) {
						supplier.setInvoiceMail(inputCurrentRow.getCell(6).getStringCellValue());
					}
				} catch (Exception e) {
					log.error("Exception Invoice Receiving Mail " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Invoice Receiving Mail is invalid.");
					isError = true;
					errorCount++;
				}

				// Payment Term
				try {
					if (inputCurrentRow.getCell(7) != null) {
						String paymentTerm = inputCurrentRow.getCell(7).getStringCellValue();
						if ("30 Days".equalsIgnoreCase(paymentTerm))
							paymentTerm = PaymentTerm.DAYS_30.getPaymentTerm();
						if ("60 Days".equalsIgnoreCase(paymentTerm))
							paymentTerm = PaymentTerm.DAYS_60.getPaymentTerm();
						else
							paymentTerm = null;
						if (StringUtils.isNotEmpty(paymentTerm))
							supplier.setPaymentTerm(paymentTerm);
					}
				} catch (Exception e) {
					log.error("Exception Payment Term " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Payment Term is invalid.");
					isError = true;
					errorCount++;
				}
				// ----------------- supplier header fields completed --------------------------

				// -------------- SUPPLIER CONTACT IS STARTED -------------------------------
				SupplierContact supplierContact = new SupplierContact();
				// Contact person
				try {
					if (inputCurrentRow.getCell(8) != null) {
						supplierContact.setName(inputCurrentRow.getCell(8).getStringCellValue());
					}
				} catch (Exception e) {
					log.error("Exception Contact person " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Contact person is invalid.");
					isError = true;
					errorCount++;
				}
				
				// Contact Number
				try {
					if (inputCurrentRow.getCell(9) != null) {
						Double number = inputCurrentRow.getCell(9).getNumericCellValue();
						supplierContact.setContactNumber(String.valueOf(number.longValue()));
					}
				} catch (Exception e) {
					log.error("Exception Contact Number " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Contact Number is invalid.");
					isError = true;
					errorCount++;
				}
				
				// alternate phone number
				try {
					if (inputCurrentRow.getCell(10) != null) {
						Double number = inputCurrentRow.getCell(10).getNumericCellValue();
						supplierContact.setAltContactNumber(String.valueOf(number.longValue()));
					}
				} catch (Exception e) {
					log.error("Exception Alternate Contact Number " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Alternate Contact Number is invalid.");
					isError = true;
					errorCount++;
				}
				// Fax
				try {
					if (inputCurrentRow.getCell(11) != null) {
						Double number = inputCurrentRow.getCell(11).getNumericCellValue();
						supplierContact.setFax(String.valueOf(number.longValue()));
					}
				} catch (Exception e) {
					log.error("Exception Fax " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Fax is invalid.");
					isError = true;
					errorCount++;
				}
				// Email
				try {
					if (inputCurrentRow.getCell(12) != null) {
						supplierContact.setEmail(inputCurrentRow.getCell(12).getStringCellValue());
					}
				} catch (Exception e) {
					log.error("Exception Email " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Email is invalid.");
					isError = true;
					errorCount++;
				}
				// Website
				try {
					if (inputCurrentRow.getCell(13) != null) {
						supplierContact.setWeb(inputCurrentRow.getCell(13).getStringCellValue());
					}
				} catch (Exception e) {
					log.error("Exception Website " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Website is invalid.");
					isError = true;
					errorCount++;
				}
				
				// primary Contact
				try {
					if (inputCurrentRow.getCell(14) != null) {
						boolean isPrimaryContact = false;
						String primaryContact = inputCurrentRow.getCell(14).getStringCellValue();
						if ("yes".equalsIgnoreCase(primaryContact)) isPrimaryContact = true;
						supplierContact.setPrimaryContact(isPrimaryContact);
					}
				} catch (Exception e) {
					log.error("Exception Primary Contact " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Primary Contact is invalid.");
					isError = true;
					errorCount++;
				}
				
				List<SupplierContact> supplierContacts = new ArrayList<SupplierContact>();
				supplierContacts = supplier.getSupplierContacts();
				if (CollectionUtils.isEmpty(supplierContacts))
					supplierContacts = new ArrayList<SupplierContact>();
				
				supplierContacts.add(supplierContact);
				supplierContacts = supplierContacts.stream().distinct().collect(Collectors.toList());
				
				supplier.setSupplierContacts(supplierContacts);
				// -------------- SUPPLIER ADDRESS IS FINISHED -------------------------------

				// -------------- SUPPLIER SUBSIDIARY IS STARTED -------------------------------
				SupplierSubsidiary supplierSubsidiary = new SupplierSubsidiary();
				
				// subsidiary
				try {
					if (inputCurrentRow.getCell(15) != null) {
						String subsidiaryName = inputCurrentRow.getCell(15).getStringCellValue();
						Long subsidiaryId = this.setupServiceClient.getSubsidiaryIdByName(subsidiaryName);
						if (subsidiaryId == null) {
							errorMessage.append(errorCount + ") Subsidiary : " + subsidiaryName + " is not found Please enter the valid Subsidiary Name. ");
							log.error("Subsidiary : " + subsidiaryName + " is not found. Please enter the valid Subsidiary Name. ");
							isError = true;
							errorCount++;
						}
						supplierSubsidiary.setSubsidiaryId(subsidiaryId);
						Subsidiary subsidiary = this.setupServiceClient.getsubsidiaryById(subsidiaryId);
						// currency
						if (subsidiary != null) {
							supplierSubsidiary.setCurrency(subsidiary .getCurrency());
						}
					} else {
						errorMessage.append(errorCount + ") Subsidiary is required. ");
						log.error("Subsidiary is required. Please enter the valid Subsidiary Name. ");
						isError = true;
						errorCount++;
					}
				} catch (Exception e) {
					log.error("Exception subsidiary " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Subsidiary Name is invalid.");
					isError = true;
					errorCount++;
				}
				
				// supplier currency
				try {
					if (inputCurrentRow.getCell(16) != null) {
						supplierSubsidiary.setSupplierCurrency(inputCurrentRow.getCell(16).getStringCellValue());
					}
				} catch (Exception e) {
					log.error("Exception Supplier currency " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Supplier currency is invalid.");
					isError = true;
					errorCount++;
				}
				
				// preferred currency
				try {
					if (inputCurrentRow.getCell(17) != null) {
						boolean isPreferredCurrency = false;
						String preferredCurrency = inputCurrentRow.getCell(17).getStringCellValue();
						if ("yes".equalsIgnoreCase(preferredCurrency)) isPreferredCurrency = true;
						supplierSubsidiary.setPreferredCurrency(isPreferredCurrency);
					}
				} catch (Exception e) {
					log.error("Exception Preferred Currency " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Preferred Currency is invalid.");
					isError = true;
					errorCount++;
				}
				
				List<SupplierSubsidiary> supplierSubsidiaries = supplier.getSupplierSubsidiary();
				if (CollectionUtils.isEmpty(supplierSubsidiaries))
					supplierSubsidiaries = new ArrayList<SupplierSubsidiary>();
				
				supplierSubsidiaries.add(supplierSubsidiary);
				
				supplierSubsidiaries = supplierSubsidiaries.stream().distinct().collect(Collectors.toList());
				supplier.setSupplierSubsidiary(supplierSubsidiaries);
				// -------------- SUPPLIER SUBSIDIARY IS FINISHED -------------------------------
				// -------------- SUPPLIER ADDRESS IS STARTED -------------------------------
				List<SupplierAddress> supplierAddresses = new ArrayList<SupplierAddress>();
				SupplierAddress supplierAddress = new SupplierAddress();
				// country
				try {
					if (inputCurrentRow.getCell(18) != null) {
						supplierAddress.setCountry(inputCurrentRow.getCell(18).getStringCellValue());
					} else {
						errorMessage.append(errorCount + ") Country is required.");
						log.error("Country is required.");
						isError = true;
						errorCount++;
					}
				} catch (Exception e) {
					log.error("Exception Supplier Country " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Supplier Country is invalid.");
					isError = true;
					errorCount++;
				}
				// state
				try {
					if (inputCurrentRow.getCell(19) != null) {
						supplierAddress.setState(inputCurrentRow.getCell(19).getStringCellValue());
					} else {
						errorMessage.append(errorCount + ") State is required.");
						log.error("State is required.");
						isError = true;
						errorCount++;
					}
				} catch (Exception e) {
					log.error("Exception Supplier State " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Supplier State is invalid.");
					isError = true;
					errorCount++;
				}
				// city
				try {
					if (inputCurrentRow.getCell(20) != null) {
						supplierAddress.setCity(inputCurrentRow.getCell(20).getStringCellValue());
					}
				} catch (Exception e) {
					log.error("Exception Supplier City " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Supplier City is invalid.");
					isError = true;
					errorCount++;
				}
				// Address Line 1
				try {
					if (inputCurrentRow.getCell(21) != null) {
						supplierAddress.setAddress1(inputCurrentRow.getCell(21).getStringCellValue());
					} else {
						errorMessage.append(errorCount + ") Address1 is required.");
						log.error("Address1 is required.");
						isError = true;
						errorCount++;
					}
				} catch (Exception e) {
					log.error("Exception Supplier Address1 " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Supplier Address1 is invalid.");
					isError = true;
					errorCount++;
				}
				// Address 2
				try {
					if (inputCurrentRow.getCell(22) != null) {
						supplierAddress.setAddress2(inputCurrentRow.getCell(22).getStringCellValue());
					}
				} catch (Exception e) {
					log.error("Exception Supplier Address2 " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Supplier Address2 is invalid.");
					isError = true;
					errorCount++;
				}
				// zip
				try {
					if (inputCurrentRow.getCell(23) != null) {
						Double pin = inputCurrentRow.getCell(23).getNumericCellValue();
						if (pin != null) {
							supplierAddress.setPin(String.valueOf(pin.intValue()));							
						}
					}
				} catch (Exception e) {
					log.error("Exception Supplier Zip code " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Supplier Zip code is invalid.");
					isError = true;
					errorCount++;
				}
				// Default Billing
				try {
					if (inputCurrentRow.getCell(24) != null) {
						boolean isDefaultBilling = false;
						String defaultBilling = inputCurrentRow.getCell(24).getStringCellValue();
						if ("yes".equalsIgnoreCase(defaultBilling)) isDefaultBilling = true;
						supplierAddress.setDefaultBilling(isDefaultBilling);
					}
				} catch (Exception e) {
					log.error("Exception Default Billing " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Default Billing is invalid.");
					isError = true;
					errorCount++;
				}
				// Default Shipping
				try {
					if (inputCurrentRow.getCell(25) != null) {
						boolean isDefaultShipping = false;
						String defaultShipping = inputCurrentRow.getCell(25).getStringCellValue();
						if ("yes".equalsIgnoreCase(defaultShipping)) isDefaultShipping = true;
						supplierAddress.setDefaultShipping(isDefaultShipping);
					}
				} catch (Exception e) {
					log.error("Exception Default Billing " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Default Shipping is invalid.");
					isError = true;
					errorCount++;
				}
				// Registration type
				String registrationType = null;
				try {
					if (inputCurrentRow.getCell(26) != null) {
						registrationType = inputCurrentRow.getCell(26).getStringCellValue();
						supplierAddress.setRegistrationType(registrationType);
					}
				} catch (Exception e) {
					log.error("Exception Supplier Registration type " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of registration type is invalid.");
					isError = true;
					errorCount++;
				}
				// TAN
				try {
					if (inputCurrentRow.getCell(27) != null) {
						String trn = inputCurrentRow.getCell(27).getStringCellValue();
						if ("Registered".equalsIgnoreCase(registrationType) && StringUtils.isEmpty(trn)) {
							log.error("Supplier Tax Registration Number is required.");
							throw new CustomMessageException(errorCount + ") Tax Registration Number is required.");
						} else {
							supplierAddress.setTaxRegistrationNumber(trn);
						}
					}
				} catch (Exception e) {
					log.error("Exception Tax Registration Number  " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Tax Registration Number is invalid.");
					isError = true;
					errorCount++;
				}
				
				supplierAddresses = supplier.getSupplierAddresses();
				if (CollectionUtils.isEmpty(supplierAddresses))
					supplierAddresses = new ArrayList<SupplierAddress>();
				
				supplierAddresses.add(supplierAddress);
				supplierAddresses = supplierAddresses.stream().distinct().collect(Collectors.toList());
				
				supplier.setSupplierAddresses(supplierAddresses);
				// -------------- SUPPLIER ADDRESS IS FINISHED -------------------------------
				// -------------- SUPPLIER ACCESS IS STARTED -------------------------------
				SupplierAccess supplierAccess = new SupplierAccess();
				// has access
				boolean isAccess = false;
				try {
					if (inputCurrentRow.getCell(28) != null) {
						String access = inputCurrentRow.getCell(28).getStringCellValue();
						if ("yes".equalsIgnoreCase(access)) isAccess = true;
						supplierAccess.setAccess(isAccess);
					}
				} catch (Exception e) {
					log.error("Exception Has access " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Has access is invalid.");
					isError = true;
					errorCount++;
				}
				// access mail
				String accessMail = null;
				try {
					if (isAccess && inputCurrentRow.getCell(29) != null) {
						accessMail = inputCurrentRow.getCell(29).getStringCellValue();
						supplierAccess.setAccessMail(accessMail);
					} else if (isAccess && inputCurrentRow.getCell(29) == null) {
						log.error("Supplier Access Mail should not be empty.");
						errorMessage.append(errorCount + ") Supplier Access Mail should not be empty.");
						isError = true;
						errorCount++;
					}
				} catch (Exception e) {
					log.error("Exception Supplier Access Mail " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Access Mail is invalid.");
					isError = true;
					errorCount++;
				}
				
				// role
				String role = null;
				try {
					if (isAccess && inputCurrentRow.getCell(30) != null) {
						role = inputCurrentRow.getCell(30).getStringCellValue();
						// TODO : validate the role and create new object and set to supplier access
						Optional<CustomRoles> customRole = customRoleRepository.findByNameAndIsDeleted(role, false);
						
						if (customRole.isPresent()) {
							List<SupplierRole> supplierRoles = new ArrayList<SupplierRole>();
							SupplierRole supplierRole = new SupplierRole();
							supplierRole.setRoleId(customRole.get().getId());
							supplierRole.setRoleName(customRole.get().getName());
							supplierRoles.add(supplierRole);
							supplierAccess.setSupplierRoles(supplierRoles);							
						} /*
							 * else { log.error("Supplier role : " + role + " is not found. ");
							 * errorMessage.append(errorCount + ") Supplier role : " + role +
							 * " is not found. "); isError = true; errorCount++; }
							 */
					}
				} catch (Exception e) {
					log.error("Exception Supplier Access Mail " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Access Mail is invalid.");
					isError = true;
					errorCount++;
				}
				
				// password
				String password = null;
				try {
					if (isAccess && inputCurrentRow.getCell(31) != null) {
						password = inputCurrentRow.getCell(31).getStringCellValue();
						supplierAccess.setPassword(password);
						supplierAccess.setPlainPassword(password);
					} else if (isAccess && inputCurrentRow.getCell(31) == null) {
						log.error("Supplier Password should not be empty.");
						errorMessage.append(errorCount + ") Supplier Password should not be empty.");
						isError = true;
						errorCount++;
					}
				} catch (Exception e) {
					log.error("Exception Supplier Password " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Supplier Password is invalid.");
					isError = true;
					errorCount++;
				}
				
				supplier.setSupplierAccess(supplierAccess);
				// -------------- SUPPLIER ACCESS IS FINISHED -------------------------------
				// -------------- SUPPLIER ACCOUNTING IS STARTED -------------------------------
				SupplierAccounting supplierAccounting = new SupplierAccounting();
				// Liability Account
				String accountName = null;
				try {
					if (inputCurrentRow.getCell(32) != null) {
						Cell cell = inputCurrentRow.getCell(32);
						cell.setCellType(CellType.STRING);
						
						accountName = cell.getStringCellValue();
						Long accountId = accountRepository.findIdByNameAndTypeAndDeleted(accountName, AccountTypes.LIABILITY.getAccountType(), false);
						if (accountId != null) supplierAccounting.setLiabilityAccountId(accountId);
						else {
							log.error("Supplier Liability Account is Not exist.");
							errorMessage.append(errorCount + ") Supplier Liability Account is Not exist.");
							isError = true;
							errorCount++;
						}
					} else {
						log.error("Supplier Liability Account is required.");
						errorMessage.append(errorCount + ") Supplier Liability Account is required.");
						isError = true;
						errorCount++;
					}
				} catch (Exception e) {
					log.error("Exception Supplier Liability Account " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Liability Account is invalid.");
					isError = true;
					errorCount++;
				}
				
				// Pre-payment
				accountName = null;
				try {
					if (inputCurrentRow.getCell(33) != null) {
						Cell cell = inputCurrentRow.getCell(33);
						cell.setCellType(CellType.STRING);
						
						accountName = cell.getStringCellValue();
						Long accountId = accountRepository.findIdByNameAndTypeAndDeleted(accountName, AccountTypes.ASSETS.getAccountType(), false);
						if (accountId != null) supplierAccounting.setPrepaymentAccountId(accountId);
						else {
							log.error("Supplier Prepayment Account is Not exist.");
							errorMessage.append(errorCount + ") Supplier Prepayment Account is Not exist.");
							isError = true;
							errorCount++;
						}
					} else {
						log.error("Supplier Prepayment Account is required.");
						errorMessage.append(errorCount + ") Supplier Prepayment Account is required.");
						isError = true;
						errorCount++;
					}
				} catch (Exception e) {
					log.error("Exception Supplier Prepayment Account " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Prepayment Account is invalid.");
					isError = true;
					errorCount++;
				}
				supplier.setSupplierAccounting(supplierAccounting);
				// ADDED IN MAP
				supplierMapping.put(externalId, supplier);
				// -------------- SUPPLIER ACCOUNTING IS FINISHED -------------------------------
				Cell cell = inputCurrentRow.createCell(statusColumnNumber);
				if (isError) {
					cell.setCellValue(errorMessage.toString());
					supplier.setHasError(true);
					continue;
				} else {
					cell.setCellValue("Imported");
				}
			}
			
			for (Map.Entry<String, Supplier> map : supplierMapping.entrySet()) {
			    log.info(map.getKey() + " ==== >>> " + map.getValue());
			    Supplier supplier = map.getValue();
			    if (supplier != null && !supplier.isHasError()) {
			    	supplier.setActive(true);
					this.save(supplier);
					log.info("user with roles is saved.");
			    }
			}

			FileOutputStream out = null;

			File outputFile = new File("item_export.xlsx");
			try {
				// Writing the workbook
				out = new FileOutputStream(outputFile);
				workbook.write(out);
				log.info("supplier_export.xlsx written successfully on disk.");
			} catch (Exception e) {
				// Display exceptions along with line number
				// using printStackTrace() method
				e.printStackTrace();
				throw new CustomException("Something went wrong. Please Contact Administrator.");
			} finally {
				out.close();
				workbook.close();
			}

			return Files.readAllBytes(outputFile.toPath());
		} catch (IOException e) {
			e.printStackTrace();
			throw new CustomException("Something went wrong. Please Contact Administrator..");
		}
	}

	@Override
	public byte[] downloadTemplate() {
		DefaultResourceLoader loader = new DefaultResourceLoader();
		try {
			File is = loader.getResource("classpath:/templates/supplier_template.xlsx").getFile();
			return Files.readAllBytes(is.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Boolean rejectAllSuppliers(List<Supplier> suppliers) {
		for (Supplier supplier : suppliers) {
			String rejectComments = supplier.getRejectComments();
			
			if (StringUtils.isEmpty(rejectComments)) {
				log.error("Reject Comments is required.");
				throw new CustomException("Reject Comments is required. It is missing for supplier : " + supplier.getId());
			}
			
			Optional<Supplier> oldSupplier = this.findOldDeepCopiedSupplier(supplier.getId());

			Optional<Supplier> existingSupplier = this.supplierRepository.findByIdAndIsDeleted(supplier.getId(), false);
			existingSupplier.get().setApprovalStatus(TransactionStatus.REJECTED.getTransactionStatus());
			existingSupplier.get().setRejectComments(rejectComments);
			existingSupplier.get().setApprovedBy(null);
			existingSupplier.get().setNextApprover(null);
			existingSupplier.get().setNextApproverRole(null);
			existingSupplier.get().setNextApproverLevel(null);
			existingSupplier.get().setApproverSequenceId(null);
			existingSupplier.get().setApproverMaxLevel(null);
			existingSupplier.get().setApproverPreferenceId(null);
			existingSupplier.get().setNoteToApprover(null);
			
			log.info("Approval Fields are restored to empty. For Supplier : " + supplier);
			
			this.saveSupplierForApproval(existingSupplier.get(), oldSupplier);
			log.info("Supplier is saved successfully with restored Approver details.");

			log.info("Approval Process is Finished for Supplier-id :: " + supplier.getId());
		}
		return true;
	}

	@Override
	public Boolean updateNextApprover(Long approverId, Long supplierId) {
		Optional<Supplier> supplier = this.supplierRepository.findByIdAndIsDeleted(supplierId, false);
		
		if (!supplier.isPresent()) {
			log.error("Supplier Not Found against given Supplier id : " + supplierId);
			throw new CustomMessageException("Supplier Not Found against given Supplier id : " + supplierId);
		}
		supplier.get().setNextApprover(String.valueOf(approverId));
		supplier.get().setLastModifiedBy(setupServiceClient.getLoggedInUsername());
		this.supplierRepository.save(supplier.get());
		
		return true;
	}

	@Override
	public Boolean selfApprove(Long supplierId) {
		Optional<Supplier> supplier = this.supplierRepository.findByIdAndIsDeleted(supplierId, false);
		
		if (!supplier.isPresent()) {
			log.error("Supplier Not Found against given Supplier id : " + supplierId);
			throw new CustomMessageException("Supplier Not Found against given Supplier id : " + supplierId);
		}
		supplier.get().setApprovalStatus(TransactionStatus.APPROVED.getTransactionStatus());
		supplier.get().setLastModifiedBy(setupServiceClient.getLoggedInUsername());
		
		if (this.supplierRepository.save(supplier.get()) != null) return true;
		else throw new CustomException("Error in self approve. Please contact System Administrator");
	}

	@Override
	public List<GraphData> findBysubsidiaryAndStatus(Long subsidiaryId) {
		Long activeCount = this.supplierRepository.getCountBySubsidiaryIdAndIsActive(subsidiaryId, true);
		Long inActiveCount = this.supplierRepository.getCountBySubsidiaryIdAndIsActive(subsidiaryId, false);
		List<GraphData> dashbordCount = new ArrayList<GraphData>();
		GraphData activeGraphData = new GraphData();
		activeGraphData.setCategory("Active Supplier");
		activeGraphData.setValue(activeCount);
		dashbordCount.add(activeGraphData);
		GraphData inActiveGraphData = new GraphData();
		inActiveGraphData.setCategory("Inactive Supplier");
		inActiveGraphData.setValue(inActiveCount);
		dashbordCount.add(inActiveGraphData);
		return dashbordCount;
	}
	@Override
	public List<GraphData> getdashboard(Long subsidiaryId) {
		GraphData pendingGraphData = new GraphData();
		pendingGraphData.setCategory(TransactionStatus.PENDING_APPROVAL.getTransactionStatus());
		pendingGraphData.setValue(supplierRepository.getCountBySubsidiaryIdAndIsActive(subsidiaryId,
				TransactionStatus.PENDING_APPROVAL.getTransactionStatus()));
		GraphData approvedGraphData = new GraphData();
		approvedGraphData.setCategory(TransactionStatus.APPROVED.getTransactionStatus());
		approvedGraphData.setValue(supplierRepository.getCountBySubsidiaryIdAndIsActive(subsidiaryId,
				TransactionStatus.APPROVED.getTransactionStatus()));
		List<GraphData> GraphDatas = new ArrayList<GraphData>();
		GraphDatas.add(approvedGraphData);
		GraphDatas.add(pendingGraphData);
		return GraphDatas;
	}

	@Override
	public String sendMailBySupplierId(Long supplierId) {
		SupplierAccess supplierAccess = this.supplierAccessRepository.findBySupplierId(supplierId);
		if (supplierAccess==null) {
			log.error("supplier is not exsist .");
			throw new CustomMessageException("supplier is not exsist . ");
		}
		String mailId = supplierAccess.getAccessMail();
		String autoCreatePassword = UUID.randomUUID().toString().substring(0, 16).replace("-", "");	
		supplierAccess.setPlainPassword(autoCreatePassword);
		supplierAccess.setPassword(passwordEncoder.encode(autoCreatePassword));
		this.supplierAccessRepository.save(supplierAccess);
		String body = "Hi,<br><br>This is the auto genrated password.<br> " + autoCreatePassword + " <br><b>Regards</b>,<br>Team Monstarbill<br>" ;
		try {
			CommonUtils.sendMail(mailId, null, "Password", body);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Error while sending the mail.");
			throw new CustomException("Error while sending the mail.");
		}	
		
		return "Mail sent successfully";
	}

	@Override
	public Long getSupplierIdByAccessMail(String email) {
		List<SupplierAccess> supplierAccesses = this.supplierAccessRepository.findByAccessMail(email);
		if (CollectionUtils.isNotEmpty(supplierAccesses)) return supplierAccesses.get(0).getSupplierId();
		else { throw new CustomBadRequestException("User is not exist in Supplier."); }
	}

	@Override
	public Supplier findByName(String supplierName) {
		Optional<Supplier> supplier = this.supplierRepository.findByNameAndIsDeleted(supplierName, false);
		if (supplier.isPresent()) return supplier.get();
		return null;
	}
	
	@Override
	public List<SupplierAddress> findAddressBySupplierIdAndAddressCode(Long supplierId, String addressCode) {
		List<SupplierAddress> addresses = new ArrayList<SupplierAddress>();
		addresses = this.supplierAddressRepository.findBySupplierIdAndAddressCodeAndIsDeleted(supplierId, addressCode, false);
		return addresses;
	}
	
	@Override
	public SupplierContact findContactBySupplierIdAndIsPrimaryContact(Long vendorId, boolean isPrimaryContact) {
		Optional<SupplierContact> supplierContact = this.supplierContactRepository.findBySupplierIdAndIsPrimaryContactAndIsDeleted(vendorId, isPrimaryContact, false);
		if (supplierContact.isPresent()) return supplierContact.get();
		return null;
	}
	
	@Override
	public List<Supplier> getSuppliersByIds(List<Long> supplierIds) {
		List<Supplier> supplierMapping = new ArrayList<Supplier>();
		if (CollectionUtils.isNotEmpty(supplierIds)) {
			supplierMapping = this.supplierRepository.getSupplierNamesByIds(supplierIds, false);
		}
		return supplierMapping;
	}
}
