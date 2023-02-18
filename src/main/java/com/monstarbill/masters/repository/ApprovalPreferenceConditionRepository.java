package com.monstarbill.masters.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.monstarbill.masters.models.ApprovalPreferenceCondition;

@Repository
public interface ApprovalPreferenceConditionRepository extends JpaRepository<ApprovalPreferenceCondition, String> {

	public Optional<ApprovalPreferenceCondition> findByIdAndIsDeleted(Long id, boolean isDeleted);

	public List<ApprovalPreferenceCondition> findByApprovalPreferenceIdAndIsDeleted(Long id, boolean isDeleted);

}
