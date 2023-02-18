package com.monstarbill.masters.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.monstarbill.masters.models.ApprovalPreference;

@Repository
public interface ApprovalPreferenceRepository extends JpaRepository<ApprovalPreference, String> {

	public Optional<ApprovalPreference> findByIdAndIsDeleted(Long id, boolean isDeleted);

	public Optional<ApprovalPreference> findBySubsidiaryIdAndSubType(Long subsidiaryId, String formName);
	
	/**
	 * below function finds the specific approver throught it's workflow i.e from all the condition it finds he specific sequence
	 * @param subsidiaryId
	 * @param subType
	 * @param amount
	 * @param locationId
	 * @param department
	 * @return
	 */
	@Query("SELECT new com.monstarbill.masters.models.ApprovalPreference(ap.id, apc.id, aps.id, aps.approverId, apc.roleId, apc.level) FROM ApprovalPreference ap "
			+ " LEFT JOIN ApprovalPreferenceCondition apc ON ap.id = apc.approvalPreferenceId "
			+ " LEFT JOIN ApprovalPreferenceSequence aps ON apc.id = aps.conditionId "
			+ " WHERE 1 = 1 AND ap.isActive = true AND ap.subsidiaryId = :subsidiaryId AND ap.subType = :subType "
			+ " AND aps.amountFrom <= :amount AND aps.amountTo >= :amount AND aps.locationId = :locationId AND aps.department = :department ")
	public Optional<ApprovalPreference> findApproverAndRole(@Param("subsidiaryId") Long subsidiaryId, @Param("subType") String subType, 
			@Param("amount") Double amount, @Param("locationId") Long locationId, @Param("department") String department);
	
	/**
	 * Below query only finds the max level & it's sequence id
	 * @param subsidiaryId
	 * @param subType
	 * @param amount
	 * @param locationId
	 * @param department
	 * @return
	 */
//	@Query("SELECT new com.monster.bill.models.ApprovalPreference(ap.id, aps.id, aps.sequenceId, apc.level) FROM ApprovalPreference ap "
//			+ " LEFT JOIN ApprovalPreferenceCondition apc ON ap.id = apc.approvalPreferenceId "
//			+ " LEFT JOIN ApprovalPreferenceSequence aps ON apc.id = aps.conditionId "
//			+ " WHERE 1 = 1 AND ap.subsidiaryId = :subsidiaryId AND ap.subType = :subType "
//			+ " AND aps.amountFrom <= :amount AND aps.amountTo >= :amount AND aps.locationId = :locationId AND aps.department = :department ")
//	public Optional<ApprovalPreference> findApproverMaxLevel(@Param("subsidiaryId") Long subsidiaryId, @Param("subType") String subType, 
//			@Param("amount") Double amount, @Param("locationId") Long locationId, @Param("department") String department);

	@Query("SELECT new com.monstarbill.masters.models.ApprovalPreference(ap.id, apc.id, aps.id, aps.approverId, apc.roleId, apc.level) FROM ApprovalPreference ap "
			+ " LEFT JOIN ApprovalPreferenceCondition apc ON ap.id = apc.approvalPreferenceId "
			+ " LEFT JOIN ApprovalPreferenceSequence aps ON apc.id = aps.conditionId "
			+ " WHERE 1 = 1 AND ap.isActive = true AND ap.id = :id AND apc.level = :level AND aps.sequenceId = :sequenceId ")
	public Optional<ApprovalPreference> findApproverByLevelAndSequence(@Param("id") Long id, @Param("level") String level, @Param("sequenceId") Long sequenceId);

	@Query(" SELECT approvalType FROM ApprovalPreference WHERE id = :id ")
	public String findApprovalPreferenceTypeById(Long id);
}
