package com.monstarbill.masters.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.monstarbill.masters.models.ApprovalRoutingPreference;

@Repository
public interface ApprovalRoutingPreferenceRepository extends JpaRepository<ApprovalRoutingPreference, String> {
	
	public List<ApprovalRoutingPreference> findByPreferenceIdAndIsDeleted(Long preferenceId, boolean isDeleted);
	
	public Optional<ApprovalRoutingPreference> findByIdAndIsDeleted(Long id, boolean isDeleted);

	@Query(" select arp "
			+ " FROM GeneralPreference gp "
			+ " LEFT JOIN ApprovalRoutingPreference arp ON gp.id = arp.preferenceId "
			+ " WHERE gp.subsidiaryId = :subsidiaryId AND arp.formName = :formName ")
	public Optional<ApprovalRoutingPreference> findIsRoutingActiveBySubsidiaryAndFormName(@Param("subsidiaryId") Long subsidiaryId, @Param("formName") String formName);
	
	@Query(" select arp.formName "
			+ " FROM GeneralPreference gp "
			+ " LEFT JOIN ApprovalRoutingPreference arp ON gp.id = arp.preferenceId "
			+ " WHERE gp.subsidiaryId = :subsidiaryId AND arp.formType = :formType AND arp.isRoutingActive = :isRoutingActive ")
	public List<String> findRoutingByStatus(@Param("subsidiaryId") Long subsidiaryId, @Param("formType") String formType, @Param("isRoutingActive") boolean isRoutingActive);
}
