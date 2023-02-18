package com.monstarbill.masters.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.monstarbill.masters.models.AccountCostCenter;

@Repository
public interface AccountCostCenterRepository extends JpaRepository<AccountCostCenter, String> {

	public List<AccountCostCenter> findAllByAccountIdAndIsDeleted(Long accountId, boolean isDeleted);

}
