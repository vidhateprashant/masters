package com.monstarbill.masters.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.monstarbill.masters.models.ApprovalPreferenceHistory;

/**
 * Repository for the Approval Preference and it's childs history
 * @author Prashant
 * 07-Sep-2022
 */
@Repository
public interface ApprovalPreferenceHistoryRepository extends JpaRepository<ApprovalPreferenceHistory, String> {

	public List<ApprovalPreferenceHistory> findByApprovalPreferenceId(Long id, Pageable pageable);

}
