package com.monstarbill.masters.service.impl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monstarbill.masters.commons.AppConstants;
import com.monstarbill.masters.commons.CommonUtils;
import com.monstarbill.masters.commons.CustomException;
import com.monstarbill.masters.commons.CustomMessageException;
import com.monstarbill.masters.commons.FilterNames;
import com.monstarbill.masters.dao.AccountDao;
import com.monstarbill.masters.enums.AccountTypes;
import com.monstarbill.masters.enums.Operation;
import com.monstarbill.masters.enums.Status;
import com.monstarbill.masters.feignclient.SetupServiceClient;
import com.monstarbill.masters.models.Account;
import com.monstarbill.masters.models.AccountDepartment;
import com.monstarbill.masters.models.AccountHistory;
import com.monstarbill.masters.models.AccountLocation;
import com.monstarbill.masters.models.AccountSubsidiary;
import com.monstarbill.masters.payload.request.PaginationRequest;
import com.monstarbill.masters.payload.response.PaginationResponse;
import com.monstarbill.masters.repository.AccountDepartmentRepository;
import com.monstarbill.masters.repository.AccountHistoryRepository;
import com.monstarbill.masters.repository.AccountLocationRepository;
import com.monstarbill.masters.repository.AccountRepository;
import com.monstarbill.masters.repository.AccountSubsidiaryRepository;
import com.monstarbill.masters.service.AccountService;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class AccountServiceImpl implements AccountService{
	
	@Autowired
	private AccountRepository accountRepository;
	
	@Autowired
	private AccountDao accountDao;
	
	@Autowired
	private AccountHistoryRepository accountHistoryRepository;
	
	@Autowired
	private AccountSubsidiaryRepository accountSubsidiaryRepository;
	
	@Autowired
	private AccountDepartmentRepository accountDepartmentRepository;
	
	@Autowired
	private AccountLocationRepository accountLocationRepository;
	
	@Autowired
	private SetupServiceClient setupServiceClient;
	
	@Override
	public Account save(Account account) {
		String username = setupServiceClient.getLoggedInUsername();
		
		Optional<Account> oldAccount = Optional.empty();

		if (account.getId() == null) {
			account.setCreatedBy(username);
		} else {
			// Get the existing object using the deep copy
			oldAccount = this.accountRepository.findByIdAndIsDeleted(account.getId(), false);
			if (oldAccount.isPresent()) {
				try {
					oldAccount = Optional.ofNullable((Account) oldAccount.get().clone());
				} catch (CloneNotSupportedException e) {
					log.error("Error while Cloning the object. Please contact administrator.");
					throw new CustomException("Error while Cloning the object. Please contact administrator.");
				}
			}
		}
		
		if (AccountTypes.BANK.getAccountType().equals(account.getType())
				&& StringUtils.isEmpty(account.getCurrency())) {
			log.error("Currency is required.");
			throw new CustomMessageException("Currency is required.");
		}

		account.setLastModifiedBy(username);
		if(account.isInactive()==false) {
			account.setInactiveDate(null);
		}
		Account accountSaved;
		try {
			accountSaved = this.accountRepository.save(account);
		} catch (DataIntegrityViolationException e) {
			log.error("Account unique constrain violetd." + e.getMostSpecificCause());
			throw new CustomException("Account unique constrain violetd :" + e.getMostSpecificCause());
		}
		
		if (accountSaved == null) {
			log.info("Error while saving the Account.");
			throw new CustomMessageException("Error while saving the Account.");
		}
		Long accountId = accountSaved.getId();
		
		log.info("Account is saved successfully :: " + accountId);
		
		// update the data in account history table
		this.updateAccountHistory(accountSaved, oldAccount);
		log.info("Account History is saved successfully.");
		
		/**
		 * STEP : 02
		 * Save the subsidiary and manage the history
		 */
		List<AccountSubsidiary> accountSubsidiaries = account.getAccountSubsidiaries();
		if (CollectionUtils.isNotEmpty(accountSubsidiaries)) {
			this.saveAccountSubsidiary(username, accountId, accountSubsidiaries);
			accountSaved.setAccountSubsidiaries(accountSubsidiaries);
		}
		log.info("All account subsidiaries are saved successfully.");
		/**
		 * STEP : 03
		 * Save the Departments and manage the history
		 */
		List<AccountDepartment> accountDepartments = account.getAccountDepartments();
		if (CollectionUtils.isNotEmpty(accountDepartments)) {
			this.saveAccountRestrictedDepartments(username, accountId, accountDepartments);
			accountSaved.setAccountDepartments(accountDepartments);
		}
		log.info("All account departments are saved successfully.");
		
		/**
		 * STEP : 04
		 * Save the Location and manage the history
		 */
		List<AccountLocation> accountLocations = account.getAccountLocations();
		if (CollectionUtils.isNotEmpty(accountLocations)) {
			this.saveAccountLocation(username, accountId, accountLocations);
			accountSaved.setAccountLocations(accountLocations);
		}
		log.info("All account Location are saved successfully.");
		
		return accountSaved;
	}

	private void saveAccountLocation(String username, Long accountId, List<AccountLocation> accountLocations) {
		for (AccountLocation accountLocation : accountLocations) {
			if (accountLocation.getId() == null) {
				// if account Location is new
				accountLocation.setAccountId(accountId);
				accountLocation.setCreatedBy(username);
				accountLocation.setLastModifiedBy(username);
				// save in table
				accountLocation = this.accountLocationRepository.save(accountLocation);
				if (accountLocation == null) {
					log.info("Error while saving the Account Location.");
					throw new CustomMessageException("Error while saving the Account Location.");
				}
				// save in history
				this.accountHistoryRepository.save(this.prepareAccountHistory(accountId, accountLocation.getId(), AppConstants.ACCOUNT_LOCATION, Operation.CREATE.toString(), username, null, accountLocation.getLocationName()));
				
				log.info("Account Location is inserted.");
			} else if (accountLocation.isDeleted()) { 
				// if account Location is Delete
				accountLocation.setLastModifiedBy(username);
				// save in table
				accountLocation = this.accountLocationRepository.save(accountLocation);
				if (accountLocation == null) {
					log.info("Error while saving the Account Location.");
					throw new CustomMessageException("Error while saving the Account Location.");
				}
				// save in history
				this.accountHistoryRepository.save(this.prepareAccountHistory(accountId, accountLocation.getId(), AppConstants.ACCOUNT_LOCATION, Operation.DELETE.toString(), username, accountLocation.getLocationName(), null));
				
				log.info("Account Location is Deleted.");
			}
		}
	}

	private void saveAccountRestrictedDepartments(String username, Long accountId, List<AccountDepartment> accountDepartments) {
		for (AccountDepartment accountDepartment : accountDepartments) {
			if (accountDepartment.getId() == null) {
				// if account Department is new
				accountDepartment.setAccountId(accountId);
				accountDepartment.setCreatedBy(username);
				accountDepartment.setLastModifiedBy(username);
				// save in table
				accountDepartment = this.accountDepartmentRepository.save(accountDepartment);
				if (accountDepartment == null) {
					log.info("Error while saving the Account Department.");
					throw new CustomMessageException("Error while saving the Account Department.");
				}
				// save in history
				this.accountHistoryRepository.save(this.prepareAccountHistory(accountId, accountDepartment.getId(), AppConstants.ACCOUNT_DEPARTMENT, Operation.CREATE.toString(), username, null, accountDepartment.getDepartmentName()));
				
				log.info("Account Department is inserted.");
			} else if (accountDepartment.isDeleted()) { 
				// if account Department is Delete
				accountDepartment.setLastModifiedBy(username);
				// save in table
				accountDepartment = this.accountDepartmentRepository.save(accountDepartment);
				if (accountDepartment == null) {
					log.info("Error while saving the Account Department.");
					throw new CustomMessageException("Error while saving the Account Department.");
				}
				// save in history
				this.accountHistoryRepository.save(this.prepareAccountHistory(accountId, accountDepartment.getId(), AppConstants.ACCOUNT_DEPARTMENT, Operation.DELETE.toString(), username, accountDepartment.getDepartmentName(), null));
				
				log.info("Account Department is Deleted.");
			}
		}
	}

	private void saveAccountSubsidiary(String username, Long accountId, List<AccountSubsidiary> accountSubsidiaries) {
		for (AccountSubsidiary accountSubsidiary : accountSubsidiaries) {
			if (accountSubsidiary.getId() == null) {
				// if account subsidiary is new
				accountSubsidiary.setAccountId(accountId);
				accountSubsidiary.setCreatedBy(username);
				accountSubsidiary.setLastModifiedBy(username);
				// save in table
				accountSubsidiary = this.accountSubsidiaryRepository.save(accountSubsidiary);
				if (accountSubsidiary == null) {
					log.info("Error while saving the Account Subsidiary.");
					throw new CustomMessageException("Error while saving the Account Subsidiary.");
				}
				// save in history
				this.accountHistoryRepository.save(this.prepareAccountHistory(accountId, accountSubsidiary.getId(), AppConstants.ACCOUNT_SUBSIDIARY, Operation.CREATE.toString(), username, null, String.valueOf(accountSubsidiary.getId())));
				
				log.info("Account subsidiary is inserted.");
			} else if (accountSubsidiary.isDeleted()) { 
				// if account subsidiary is Delete
				accountSubsidiary.setLastModifiedBy(username);
				// save in table
				accountSubsidiary = this.accountSubsidiaryRepository.save(accountSubsidiary);
				if (accountSubsidiary == null) {
					log.info("Error while saving the Account Subsidiary.");
					throw new CustomMessageException("Error while saving the Account Subsidiary.");
				}
				// save in history
				this.accountHistoryRepository.save(this.prepareAccountHistory(accountId, accountSubsidiary.getId(), AppConstants.ACCOUNT_SUBSIDIARY, Operation.DELETE.toString(), username, String.valueOf(accountSubsidiary.getId()), null));
				
				log.info("Account subsidiary is Deleted.");
			}
		}
	}
	
	/**
	 * This method save the data in history table
	 * Add entry as a Insert if Account is new 
	 * Add entry as a Update if Account is exists
	 * @param Account
	 * @param oldAccount
	 */
	private void updateAccountHistory(Account account, Optional<Account> oldAccount) {
		if (oldAccount.isPresent()) {
			// insert the updated fields in history table
			List<AccountHistory> accountHistories = new ArrayList<AccountHistory>();
			try {
				accountHistories = oldAccount.get().compareFields(account);
				if (CollectionUtils.isNotEmpty(accountHistories)) {
					this.accountHistoryRepository.saveAll(accountHistories);
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				log.error("Error while comparing the new and old objects. Please contact administrator.");
				throw new CustomException("Error while comparing the new and old objects. Please contact administrator.");
			}
		} else {
			// Insert in history table as Operation - INSERT 
			this.accountHistoryRepository.save(this.prepareAccountHistory(account.getId(), null, AppConstants.ACCOUNT, Operation.CREATE.toString(), account.getLastModifiedBy(), null, String.valueOf(account.getId())));
		}
		log.info("Account History is updated successfully");
	}

	/**
	 * Prepares the history for the Account
	 * @param accountId
	 * @param moduleName
	 * @param operation
	 * @param lastModifiedBy
	 * @param oldValue
	 * @param newValue
	 * @return
	 */
	public AccountHistory prepareAccountHistory(Long accountId, Long childId, String moduleName, String operation, String lastModifiedBy, String oldValue, String newValue) {
		AccountHistory accountHistory = new AccountHistory();
		accountHistory.setAccountId(accountId);
		accountHistory.setChildId(childId);
		accountHistory.setModuleName(moduleName);
		accountHistory.setChangeType(AppConstants.UI);
		accountHistory.setOperation(operation);
		accountHistory.setOldValue(oldValue);
		accountHistory.setNewValue(newValue);
		accountHistory.setLastModifiedBy(lastModifiedBy);
		return accountHistory;
	}
	
	@Override
	public Account getAccountById(Long id) {
		Optional<Account> account = Optional.empty();
		account = this.accountRepository.findByIdAndIsDeleted(id, false);
		if (!account.isPresent()) {
			log.info("Account is not found given id : " + id);
			throw new CustomMessageException("Account is not found given id : " + id);
		}
		account.get().setAccountSubsidiaries(this.accountSubsidiaryRepository.findAllByAccountIdAndIsDeletedOrderByIdAsc(id, false));
		account.get().setAccountDepartments(this.accountDepartmentRepository.findAllByAccountIdAndIsDeletedOrderByIdAsc(id, false));
		account.get().setAccountLocations(this.accountLocationRepository.findAllByAccountIdAndIsDeletedOrderByIdAsc(id, false));
		return account.get();
	}
	
	@Override
	public PaginationResponse findAll(PaginationRequest paginationRequest) {
		List<Account> accounts = new ArrayList<Account>();
		
		// preparing where clause
		String whereClause = this.prepareWhereClause(paginationRequest).toString();
		
		// get list
		accounts = this.accountDao.findAll(whereClause, paginationRequest);
		
		// getting count
		Long totalRecords = this.accountDao.getCount(whereClause);
		
		return CommonUtils.setPaginationResponse(paginationRequest.getPageNumber(), paginationRequest.getPageSize(), accounts, totalRecords);
	
	}

	private StringBuilder prepareWhereClause(PaginationRequest paginationRequest) {
		Map<String, ?> filters = paginationRequest.getFilters();
		
		String code = null;
		String type = null;
		String status = null;
		List<?> subsidiaryNames = new ArrayList<String>();
		
		if (filters.containsKey(FilterNames.CODE))
			code = (String) filters.get(FilterNames.CODE);
		if (filters.containsKey(FilterNames.TYPE))
			type = (String) filters.get(FilterNames.TYPE);
		if (filters.containsKey(FilterNames.STATUS)) 
			status = (String) filters.get(FilterNames.STATUS);
		if (filters.containsKey(FilterNames.SUBSIDIARY_NAME))
			subsidiaryNames = CommonUtils.convertObjectToList(filters.get(FilterNames.SUBSIDIARY_NAME));
		
		StringBuilder whereClause = new StringBuilder("  ");
		if (StringUtils.isNotEmpty(code)) {
			whereClause.append(" AND lower(a.code) like lower ('%").append(code).append("%')");
		}
		if (StringUtils.isNotEmpty(type)) {
			whereClause.append(" AND lower(a.type) like lower('%").append(type).append("%')");
		}
		if (StringUtils.isNotEmpty(status)) {
			if (Status.ACTIVE.toString().equalsIgnoreCase(status)) {
				whereClause.append(" AND a.is_inactive is false ");
			} else if (Status.INACTIVE.toString().equalsIgnoreCase(status)) {
				whereClause.append(" AND a.is_inactive is true ");
			}
		}
		if (CollectionUtils.isNotEmpty(subsidiaryNames)) {
			String subsidiaryName = CommonUtils.convertListToCommaSepratedStringWithQuotes(subsidiaryNames);
			whereClause.append(" AND s.subsidiary_name in (").append(subsidiaryName).append(")");
		}
		return whereClause;
	}
	
	@Override
	public boolean deleteById(Long id) {
		Account account = new Account();
		account = this.getAccountById(id);
		account.setDeleted(true);
		account = this.accountRepository.save(account);
		
		if (account == null) {
			log.error("Error while deleting the Account : " + id);
			throw new CustomMessageException("Error while deleting the Account : " + id);
		}
		// update the operation in the history
		this.accountHistoryRepository.save(this.prepareAccountHistory(account.getId(), null, AppConstants.ACCOUNT, Operation.DELETE.toString(), account.getLastModifiedBy(), String.valueOf(account.getId()), null));

	
		return true;
	}
	
	@Override
	public List<AccountHistory> findHistoryById(Long id, Pageable pageable) {
		List<AccountHistory> histories = this.accountHistoryRepository.findByAccountIdOrderById(id, pageable);
		String createdBy = histories.get(0).getLastModifiedBy();
		histories.forEach(e->{
			e.setCreatedBy(createdBy);
		});
		return histories;
	}

	@Override
	public List<Account> getAccountsByType(String type) {
		return this.accountRepository.findByType(type);
	}

	@Override
	public List<Account> getParentAccounts() {
		return this.accountRepository.findParentAccounts();
	}

	@Override
	public List<Account> findBySubsidiaryIdAndType(Long subsidiaryId, List<String> type) {
		List<Account> accounts = new ArrayList<Account>();
		accounts = this.accountRepository.getAllAccountCodeBySubsidiaryIdAndType(subsidiaryId, type, false);
		log.info("Get all account code  by subsidary id and type ." + accounts);
		return accounts;
	}

	@Override
	public List<Account> getParentAccountsBySubsidiaryId(List<Long> subsidiaryId) {
		return this.accountRepository.findParentAccountsBySubsidiaryId(subsidiaryId, false);
	}

	@Override
	public List<Account> findBySubsidiaryIdAndTdsTaxCode(Long subsidiaryId, List<String> tdsTaxCode) {
		List<Account> accounts = new ArrayList<Account>();
		accounts = this.accountRepository.getAllAccountCodeBySubsidiaryIdAndTdsTaxCode(subsidiaryId, tdsTaxCode, false);
		log.info("Get all account code  by subsidary id and tax code ." + accounts);
		return accounts;
	}

	@Override
	public List<Account> findBySubsidiaryIdAndTypeAndCurrency(Long subsidiaryId, List<String> type, String currency) {
		List<Account> accounts = new ArrayList<Account>();
		accounts = this.accountRepository.getAllAccountCodeBySubsidiaryIdAndTypeAndCurrencyAndIsDeleted(subsidiaryId, type, currency, false);
		log.info("Get all account code  by subsidary id and type and currency." + accounts);
		return accounts;
	}

	@Override
	public Account findAccountById(Long id) {
		Optional<Account> account = Optional.empty();
		account = this.accountRepository.findByIdAndIsDeleted(id, false);
		if (!account.isPresent()) {
			log.info("Account is not found given id : " + id);
			throw new CustomMessageException("Account is not found given id : " + id);
		}
		return account.get();
	}
}
