package com.monstarbill.masters.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.monstarbill.masters.models.BankHistory;

/**
 * Repository for the Bank and it's childs history
 * @author Prashant
 * 02-Jul-2022
 */
@Repository
public interface BankHistoryRepository extends JpaRepository<BankHistory, String> {

	public List<BankHistory> findByBankId(Long id, Pageable pageable);

}
