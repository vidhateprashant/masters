package com.monstarbill.masters.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.monstarbill.masters.models.AccountSubsidiary;

@Repository
public interface AccountSubsidiaryRepository extends JpaRepository<AccountSubsidiary, String> {

	//public List<AccountSubsidiary> findAllByAccountIdAndIsDeleted(Long accountId, boolean isDeleted);

	public List<AccountSubsidiary> findBySubsidiaryIdIn(List<Long> subsidiaryId);

	public List<AccountSubsidiary> findAllByAccountIdAndIsDeletedOrderByIdAsc(Long accountId, boolean isDeleted);

}
