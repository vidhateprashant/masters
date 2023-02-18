package com.monstarbill.masters.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.monstarbill.masters.models.Account;

public interface AccountRepository extends JpaRepository<Account, String>{
	
	public Optional<Account> findByIdAndIsDeleted(Long id, boolean isDeleted);

	public List<Account> findByType(String type);

	@Query("SELECT new com.monstarbill.masters.models.Account(id, code, description) FROM Account WHERE isAccountSummary is true AND isInactive is false ORDER BY code ")
	public List<Account> findParentAccounts();

	@Query("SELECT new com.monstarbill.masters.models.Account(a.id, a.code, a.description, a.type, a.currency, a.isInactive, a.isAccountSummary) FROM Account a inner join AccountSubsidiary asu ON a.id = asu.accountId "
			+"where asu.subsidiaryId = :subsidiaryId and a.type in :type and a.isDeleted = :isDeleted AND a.isInactive is false ")
	public List<Account> getAllAccountCodeBySubsidiaryIdAndType(@Param("subsidiaryId") Long subsidiaryId,@Param("type") List<String> type, @Param("isDeleted") boolean isDeleted);

	@Query(" SELECT id FROM Account where lower(code) = lower(:accountName) AND isDeleted = :isDeleted ")
	public Long findIdByNameAndDeleted(String accountName, boolean isDeleted);
	
	@Query(" SELECT id FROM Account where lower(code) = lower(:accountName) AND type = :type AND isDeleted = :isDeleted ")
	public Long findIdByNameAndTypeAndDeleted(String accountName, String type, boolean isDeleted);

	@Query("SELECT new com.monstarbill.masters.models.Account(a.id, a.code, a.description) FROM Account a inner join AccountSubsidiary asu ON a.id = asu.accountId WHERE asu.subsidiaryId in :subsidiaryId AND a.isDeleted = :isDeleted and a.isAccountSummary is true ")
	public List<Account> findParentAccountsBySubsidiaryId(@Param("subsidiaryId") List<Long> subsidiaryId, @Param("isDeleted") boolean isDeleted);
	
	@Query("SELECT new com.monstarbill.masters.models.Account(a.id, a.code, a.description, a.type) FROM Account a inner join AccountSubsidiary asu ON a.id = asu.accountId "
			+"where asu.subsidiaryId = :subsidiaryId and a.tdsTaxCode in :tdsTaxCode and a.isDeleted = :isDeleted")
	public List<Account> getAllAccountCodeBySubsidiaryIdAndTdsTaxCode(@Param("subsidiaryId") Long subsidiaryId,@Param("tdsTaxCode") List<String> tdsTaxCode, @Param("isDeleted") boolean isDeleted);

	@Query("SELECT new com.monstarbill.masters.models.Account(a.id, a.code, a.description, a.type, a.currency, a.isInactive, a.isAccountSummary) FROM Account a inner join AccountSubsidiary asu ON a.id = asu.accountId "
			+"where asu.subsidiaryId = :subsidiaryId and a.type in :type and a.currency = :currency and a.isDeleted = :isDeleted AND a.isInactive is false ")
	public List<Account> getAllAccountCodeBySubsidiaryIdAndTypeAndCurrencyAndIsDeleted(Long subsidiaryId, List<String> type, String currency, boolean isDeleted);

}
