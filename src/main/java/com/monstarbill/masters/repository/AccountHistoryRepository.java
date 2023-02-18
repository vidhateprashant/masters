package com.monstarbill.masters.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.monstarbill.masters.models.AccountHistory;

public interface AccountHistoryRepository extends JpaRepository<AccountHistory, String>{
	
	public List<AccountHistory> findByAccountIdOrderById(Long id, Pageable pageable);

}
