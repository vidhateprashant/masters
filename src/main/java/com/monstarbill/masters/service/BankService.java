package com.monstarbill.masters.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.monstarbill.masters.models.Bank;
import com.monstarbill.masters.models.BankHistory;
import com.monstarbill.masters.models.BankPaymentInstrument;
import com.monstarbill.masters.payload.request.PaginationRequest;
import com.monstarbill.masters.payload.response.PaginationResponse;

public interface BankService {

	public Bank save(Bank bank);

	public Bank getBankById(Long id);

	public PaginationResponse findAll(PaginationRequest paginationRequest);

	public boolean deleteById(Long id);

	public List<BankHistory> findHistoryById(Long id, Pageable pageable);

	public BankPaymentInstrument save(BankPaymentInstrument bankPaymentInstrument);

	public boolean deleteBankPaymentInstrumentById(Long id);

	public Boolean getValidateName(String name);

	public List<Bank> getBankBySubsidiaryId(Long subsidiaryId);

}
