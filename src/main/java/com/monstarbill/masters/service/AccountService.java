package com.monstarbill.masters.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.monstarbill.masters.models.Account;
import com.monstarbill.masters.models.AccountHistory;
import com.monstarbill.masters.payload.request.PaginationRequest;
import com.monstarbill.masters.payload.response.PaginationResponse;

public interface AccountService {
	
	public Account save(Account account);

	public Account getAccountById(Long id);
	
	public boolean deleteById(Long id);
	
	public List<AccountHistory> findHistoryById(Long id, Pageable pageable);

	public PaginationResponse findAll(PaginationRequest paginationRequest);

	public List<Account> getAccountsByType(String type);

	public List<Account> getParentAccounts();

	public List<Account> findBySubsidiaryIdAndType(Long subsidiaryId, List<String> type);

	public List<Account> getParentAccountsBySubsidiaryId(List<Long> subsidiaryId);

	public List<Account> findBySubsidiaryIdAndTdsTaxCode(Long subsidiaryId, List<String> tdsTaxCode);

	public List<Account> findBySubsidiaryIdAndTypeAndCurrency(Long subsidiaryId, List<String> type, String currency);

	public Account findAccountById(Long id);
	
}
