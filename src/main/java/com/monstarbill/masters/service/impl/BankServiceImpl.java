package com.monstarbill.masters.service.impl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
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
import com.monstarbill.masters.dao.BankDao;
import com.monstarbill.masters.enums.Operation;
import com.monstarbill.masters.feignclient.SetupServiceClient;
import com.monstarbill.masters.models.Bank;
import com.monstarbill.masters.models.BankHistory;
import com.monstarbill.masters.models.BankPaymentInstrument;
import com.monstarbill.masters.payload.request.PaginationRequest;
import com.monstarbill.masters.payload.response.PaginationResponse;
import com.monstarbill.masters.repository.BankHistoryRepository;
import com.monstarbill.masters.repository.BankPaymentInstrumentsRepository;
import com.monstarbill.masters.repository.BankRepository;
import com.monstarbill.masters.service.BankService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional
public class BankServiceImpl implements BankService {

	@Autowired
	private BankRepository bankRepository;
	
	@Autowired
	private BankHistoryRepository bankHistoryRepository;
	
	@Autowired
	private BankPaymentInstrumentsRepository bankPaymentInstrumentRepository;

	@Autowired
	private BankDao bankDao;
	
	@Autowired
	private SetupServiceClient setupServiceClient;
	
	@Override
	public Bank save(Bank bank) {
		Optional<Bank> oldBank = Optional.ofNullable(null);

		if (bank.getId() == null) {
			bank.setCreatedBy(setupServiceClient.getLoggedInUsername());
		} else {
			// Get the existing object using the deep copy
			oldBank = this.bankRepository.findByIdAndIsDeleted(bank.getId(), false);
			if (oldBank.isPresent()) {
				try {
					oldBank = Optional.ofNullable((Bank) oldBank.get().clone());
				} catch (CloneNotSupportedException e) {
					log.error("Error while Cloning the object. Please contact administrator.");
					throw new CustomException("Error while Cloning the object. Please contact administrator.");
				}
			}
		}

		bank.setLastModifiedBy(setupServiceClient.getLoggedInUsername());
		if (bank.isActive() == true) {
			bank.setActiveDate(null);
		}
		Bank bankSaved;
		try {
			bankSaved = this.bankRepository.save(bank);
		}catch (DataIntegrityViolationException e) {
			log.error("Bank unique constrain violetd." + e.getMostSpecificCause());
			throw new CustomException("Bank unique constrain violetd :" + e.getMostSpecificCause());
		}
		
		if (bankSaved == null) {
			log.info("Error while saving the Bank." + bank.toString());
			throw new CustomMessageException("Error while saving the Bank.");
		}
		
		// update the data in bank history table
		this.updateBankHistory(bankSaved, oldBank);
		
		// 2. Save the Payment Instruments
		if (CollectionUtils.isNotEmpty(bank.getBankPaymentInstruments())) {
			for (BankPaymentInstrument paymentInstruments : bank.getBankPaymentInstruments()) {
				paymentInstruments.setBankId(bankSaved.getId());
				this.save(paymentInstruments);
			}
		}
		
		return bank;
	}

	/**
	 * This method save the data in history table
	 * Add entry as a Insert if Bank is new 
	 * Add entry as a Update if bank is exists
	 * @param bank
	 * @param oldBank
	 */
	private void updateBankHistory(Bank bank, Optional<Bank> oldBank) {
		if (oldBank.isPresent()) {
			// insert the updated fields in history table
			List<BankHistory> bankHistories = new ArrayList<BankHistory>();
			try {
				bankHistories = oldBank.get().compareFields(bank);
				if (CollectionUtils.isNotEmpty(bankHistories)) {
					this.bankHistoryRepository.saveAll(bankHistories);
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				log.error("Error while comparing the new and old objects. Please contact administrator.");
				throw new CustomException("Error while comparing the new and old objects. Please contact administrator.");
			}
			log.info("Bank History is updated successfully");
		} else {
			// Insert in history table as Operation - INSERT 
			this.bankHistoryRepository.save(this.prepareBankHistory(bank.getId(), null, AppConstants.BANK, Operation.CREATE.toString(), bank.getLastModifiedBy(), null, String.valueOf(bank.getId())));
		}
	}

	/**
	 * Prepares the history for the Bank
	 * @param bankId
	 * @param moduleName
	 * @param operation
	 * @param lastModifiedBy
	 * @param oldValue
	 * @param newValue
	 * @return
	 */
	public BankHistory prepareBankHistory(Long bankId, Long childId, String moduleName, String operation, String lastModifiedBy, String oldValue, String newValue) {
		BankHistory bankHistory = new BankHistory();
		bankHistory.setBankId(bankId);
		bankHistory.setChildId(childId);
		bankHistory.setModuleName(moduleName);
		bankHistory.setChangeType(AppConstants.UI);
		bankHistory.setOperation(operation);
		bankHistory.setOldValue(oldValue);
		bankHistory.setNewValue(newValue);
		bankHistory.setLastModifiedBy(lastModifiedBy);
		return bankHistory;
	}

	
	@Override
	public Bank getBankById(Long id) {
		Optional<Bank> bank = Optional.ofNullable(null);
		bank = this.bankRepository.findByIdAndIsDeleted(id, false);
		if (!bank.isPresent()) {
			log.info("Bank is not exist for id - " + id);
			throw new CustomMessageException("Bank is not exist for id - " + id);
		}
		bank.get().setBankPaymentInstruments(this.bankPaymentInstrumentRepository.findByBankIdAndIsDeleted(id, false));
		return bank.get();
	}

	@Override
	public PaginationResponse findAll(PaginationRequest paginationRequest) {
		List<Bank> banks = new ArrayList<Bank>();
		
		// preparing where clause
		String whereClause = this.prepareWhereClause(paginationRequest).toString();
		
		// get list
		banks = this.bankDao.findAll(whereClause, paginationRequest);
		
		// getting count
		Long totalRecords = this.bankDao.getCount(whereClause);
		
		return CommonUtils.setPaginationResponse(paginationRequest.getPageNumber(), paginationRequest.getPageSize(), banks, totalRecords);
	}

	@SuppressWarnings("unchecked")
	private StringBuilder prepareWhereClause(PaginationRequest paginationRequest) {
		Map<String, ?> filters = paginationRequest.getFilters();

		List<String> subsidiaries = new ArrayList<String>();
		List<String> names = new ArrayList<String>();
		boolean isActive = true;
		String status = null;
		
		if (filters.containsKey(FilterNames.NAME)) {
			if (filters.get(FilterNames.NAME) instanceof Collection) {
		        names = new ArrayList<String>((Collection<String>) filters.get(FilterNames.NAME));
		    }
		}
		if (filters.containsKey(FilterNames.SUBSIDIARY_NAME)) {
			if (filters.get(FilterNames.SUBSIDIARY_NAME) instanceof Collection) {
				subsidiaries = new ArrayList<String>((Collection<String>) filters.get(FilterNames.SUBSIDIARY_NAME));
			}
		}
		if (filters.containsKey(FilterNames.STATUS)) {
			status = (String) filters.get(FilterNames.STATUS);
			isActive = Boolean.parseBoolean(status);
		}
		
		StringBuilder whereClause = new StringBuilder(" AND b.isDeleted is false ");
		
		if (CollectionUtils.isNotEmpty(subsidiaries)) {
			whereClause.append(" AND s.name in (").append(CommonUtils.convertListToCommaSepratedStringWithQuotes(subsidiaries)).append(")");
		}
		if (CollectionUtils.isNotEmpty(names)) {
			whereClause.append(" AND b.name in (").append(CommonUtils.convertListToCommaSepratedStringWithQuotes(names)).append(")");
		}
		if (StringUtils.isNotEmpty(status)) {
			whereClause.append(" AND b.isActive is ").append(isActive).append(" ");
		}

		return whereClause;
	}

	@Override
	public boolean deleteById(Long id) {
		Bank bank = new Bank();
		bank = this.getBankById(id);
		bank.setDeleted(true);
		
		bank = this.bankRepository.save(bank);
		
		if (bank == null) {
			log.error("Error while deleting the Bank : " + id);
			throw new CustomMessageException("Error while deleting the Bank : " + id);
		}
	
		List<BankPaymentInstrument> bankPaymentInstruments = this.bankPaymentInstrumentRepository.findByBankIdAndIsDeleted(id, false);
		for (BankPaymentInstrument bankPaymentInstrument : bankPaymentInstruments) {
			bankPaymentInstrument.setDeleted(true);
			this.bankPaymentInstrumentRepository.save(bankPaymentInstrument);
			
			// update the operation in the history
			this.bankHistoryRepository.save(this.prepareBankHistory(bankPaymentInstrument.getBankId(), bankPaymentInstrument.getId(), AppConstants.BANK_PAYMENT_INSTRUMENTS, Operation.DELETE.toString(), bankPaymentInstrument.getLastModifiedBy(), String.valueOf(bankPaymentInstrument.getId()), null));
		}

		// update the operation in the history
		this.bankHistoryRepository.save(this.prepareBankHistory(bank.getId(), null, AppConstants.BANK, Operation.DELETE.toString(), bank.getLastModifiedBy(), String.valueOf(bank.getId()), null));
		
		return true;
	}

	@Override
	public List<BankHistory> findHistoryById(Long id, Pageable pageable) {
		return this.bankHistoryRepository.findByBankId(id, pageable);
	}

	@Override
	public BankPaymentInstrument save(BankPaymentInstrument bankPaymentInstrument) {
		bankPaymentInstrument.setCreatedBy(setupServiceClient.getLoggedInUsername());
		bankPaymentInstrument.setLastModifiedBy(setupServiceClient.getLoggedInUsername());
		bankPaymentInstrument = this.bankPaymentInstrumentRepository.save(bankPaymentInstrument);
		
		if (bankPaymentInstrument == null) {
			log.info("Error while saving the Bank.");
			throw new CustomMessageException("Error while saving the Bank.");
		}
		
		// update the data in bank history table
		this.bankHistoryRepository.save(this.prepareBankHistory(bankPaymentInstrument.getBankId(), bankPaymentInstrument.getId(), AppConstants.BANK_PAYMENT_INSTRUMENTS, Operation.CREATE.toString(), bankPaymentInstrument.getLastModifiedBy(), null, String.valueOf(bankPaymentInstrument.getId())));
		
		return bankPaymentInstrument;
	}

	@Override
	public boolean deleteBankPaymentInstrumentById(Long id) {
		Optional<BankPaymentInstrument> bankPaymentInstrument = Optional.ofNullable(null);
		bankPaymentInstrument = this.bankPaymentInstrumentRepository.findByIdAndIsDeleted(id, false);
		
		if (!bankPaymentInstrument.isPresent()) {
			log.error("Bank Payment Instrumnet is not exist for ID - " + id);
			throw new CustomMessageException("Bank Payment Instrumnet is not exist for ID - " + id);
		}
		
		bankPaymentInstrument.get().setDeleted(true);
		BankPaymentInstrument bankPaymentInstruments = this.bankPaymentInstrumentRepository.save(bankPaymentInstrument.get());
		
		if (bankPaymentInstruments == null) {
			log.error("Error while deleting the Bank Payment Instruments : " + id);
			throw new CustomMessageException("Error while deleting the Bank Payment Instruments : " + id);
		}
	
		// update the operation in the history
		this.bankHistoryRepository.save(this.prepareBankHistory(bankPaymentInstruments.getBankId(), bankPaymentInstruments.getId(), AppConstants.BANK_PAYMENT_INSTRUMENTS, Operation.DELETE.toString(), bankPaymentInstruments.getLastModifiedBy(), String.valueOf(bankPaymentInstruments.getId()), null));
		
		return true;
	}

	@Override
	public Boolean getValidateName(String name) {
		// if name is empty then name is not valid
		if (StringUtils.isEmpty(name)) return false;
		
		Long countOfRecordsWithSameName = this.bankRepository.getCountByName(name.trim());
		// if we we found the count greater than 0 then it is not valid. If it is zero then it is valid string
		if (countOfRecordsWithSameName > 0) return false; else return true;
	}

	@Override
	public List<Bank> getBankBySubsidiaryId(Long subsidiaryId) {
		List<Bank> bank = new ArrayList<Bank>();
		bank = this.bankRepository.getAllBySubsidiaryIdAndIsDeleted(subsidiaryId, false);
		return bank;
	}


}
