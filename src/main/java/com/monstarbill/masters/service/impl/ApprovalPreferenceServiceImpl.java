package com.monstarbill.masters.service.impl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monstarbill.masters.commons.AppConstants;
import com.monstarbill.masters.commons.CommonUtils;
import com.monstarbill.masters.commons.CustomException;
import com.monstarbill.masters.commons.CustomMessageException;
import com.monstarbill.masters.commons.FilterNames;
import com.monstarbill.masters.dao.ApprovalPreferenceDao;
import com.monstarbill.masters.enums.Operation;
import com.monstarbill.masters.feignclient.SetupServiceClient;
import com.monstarbill.masters.models.ApprovalPreference;
import com.monstarbill.masters.models.ApprovalPreferenceCondition;
import com.monstarbill.masters.models.ApprovalPreferenceHistory;
import com.monstarbill.masters.models.ApprovalPreferenceSequence;
import com.monstarbill.masters.payload.request.ApprovalRequest;
import com.monstarbill.masters.payload.request.PaginationRequest;
import com.monstarbill.masters.payload.response.PaginationResponse;
import com.monstarbill.masters.repository.ApprovalPreferenceConditionRepository;
import com.monstarbill.masters.repository.ApprovalPreferenceHistoryRepository;
import com.monstarbill.masters.repository.ApprovalPreferenceRepository;
import com.monstarbill.masters.repository.ApprovalPreferenceSequenceRepository;
import com.monstarbill.masters.service.ApprovalPreferenceService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class ApprovalPreferenceServiceImpl implements ApprovalPreferenceService{
	
	@Autowired
	private ApprovalPreferenceRepository approvalPreferenceRepository;
	
	@Autowired
	private ApprovalPreferenceConditionRepository approvalPreferenceConditionRepository;
	
	@Autowired
	private ApprovalPreferenceSequenceRepository approvalPreferenceSequenceRepository;
	
	@Autowired
	private ApprovalPreferenceHistoryRepository approvalPreferenceHistoryRepository;
	
//	@Autowired
//	private PurchaseRequisitionRepository purchaseRequisitionRepository;
	
	@Autowired
	private ApprovalPreferenceDao approvalPreferenceDao;
	
	@Autowired
	private SetupServiceClient setupServiceClient;
	
	@Override
	public List<ApprovalPreferenceHistory> findHistoryById(Long id, Pageable pageable) {
		return this.approvalPreferenceHistoryRepository.findByApprovalPreferenceId(id, pageable);
	}

	/**
	 * Save Approval Preference
	 * 1. Approval Preference
	 * 2. All Approval Preference Conditions using loop
	 * 3. All Approval Preference sequences for all conditions
	 */
	@Override
	public ApprovalPreference save(ApprovalPreference approvalPreference) {
		String username = setupServiceClient.getLoggedInUsername();
		
		Optional<ApprovalPreference> oldApprovalPreference = Optional.empty();

		if (approvalPreference.getId() == null) {
			approvalPreference.setCreatedBy(username);
		} else {
			// Get the existing object using the deep copy
			oldApprovalPreference = this.approvalPreferenceRepository.findByIdAndIsDeleted(approvalPreference.getId(), false);
			if (oldApprovalPreference.isPresent()) {
				try {
					oldApprovalPreference = Optional.ofNullable((ApprovalPreference) oldApprovalPreference.get().clone());
				} catch (CloneNotSupportedException e) {
					log.error("Error while Cloning the object. Please contact administrator.");
					throw new CustomException("Error while Cloning the object. Please contact administrator.");
				}
			}
		}

		approvalPreference.setLastModifiedBy(username);
		ApprovalPreference approvalPreferenceSaved = this.approvalPreferenceRepository.save(approvalPreference);
		
		if (approvalPreferenceSaved == null) {
			log.error("Error while saving the Approval Preference.");
			throw new CustomMessageException("Error while saving the Approval Preference.");
		}
		log.info("Approval Preference is saved Successfully :: " + approvalPreferenceSaved.getId());
		
		// update the data in Approval Preference history table
		this.updateApprovalPreferenceHistory(approvalPreferenceSaved, oldApprovalPreference);
		log.info("Approval Preference History is saved Successfully.");
		
		Long approvalPreferenceId = approvalPreferenceSaved.getId();
		
		List<ApprovalPreferenceCondition> approvalPreferenceconditions = approvalPreference.getApprovalPreferenceConditions();
		if (CollectionUtils.isNotEmpty(approvalPreferenceconditions)) {
			for (ApprovalPreferenceCondition approvalPreferenceCondition : approvalPreferenceconditions) {
				this.save(approvalPreferenceId, approvalPreferenceCondition);
			}
		}
		
		return approvalPreferenceSaved;
	}

	/**
	 * Save the Approval Preference Condition and it's sequences
	 * @param approvalPreferenceId
	 * @param approvalPreferenceCondition
	 */
	private void save(Long approvalPreferenceId, ApprovalPreferenceCondition approvalPreferenceCondition) {
		String username = setupServiceClient.getLoggedInUsername();
		
		approvalPreferenceCondition.setApprovalPreferenceId(approvalPreferenceId);

		Optional<ApprovalPreferenceCondition> oldApprovalPreferenceCondition = Optional.empty();

		if (approvalPreferenceCondition.getId() == null) {
			approvalPreferenceCondition.setCreatedBy(username);
		} else {
			// Get the existing object using the deep copy
			oldApprovalPreferenceCondition = this.approvalPreferenceConditionRepository.findByIdAndIsDeleted(approvalPreferenceCondition.getId(), false);
			if (oldApprovalPreferenceCondition.isPresent()) {
				try {
					oldApprovalPreferenceCondition = Optional.ofNullable((ApprovalPreferenceCondition) oldApprovalPreferenceCondition.get().clone());
				} catch (CloneNotSupportedException e) {
					log.error("Error while Cloning the object. Please contact administrator.");
					throw new CustomException("Error while Cloning the object. Please contact administrator.");
				}
			}
		}

		approvalPreferenceCondition.setLastModifiedBy(username);
		ApprovalPreferenceCondition approvalPreferenceConditionSaved = this.approvalPreferenceConditionRepository.save(approvalPreferenceCondition);
		
		if (approvalPreferenceConditionSaved == null) {
			log.error("Error while saving the Approval Preference Condition.");
			throw new CustomMessageException("Error while saving the Approval Preference Condition.");
		}
		log.info("Approval Preference Condition is saved Successfully :: " + approvalPreferenceConditionSaved.getId());
		
		// update the data in Approval Preference history table
		this.updateApprovalPreferenceConditionHistory(approvalPreferenceConditionSaved, oldApprovalPreferenceCondition);
		log.info("Approval Preference Condition History is saved Successfully.");
		
		Long approvalPreferenceConditionId = approvalPreferenceConditionSaved.getId();
		
		List<ApprovalPreferenceSequence> approvalPreferenceSequences = approvalPreferenceCondition.getApprovalPreferenceSequences();
		
		if (CollectionUtils.isNotEmpty(approvalPreferenceSequences)) {
			for (ApprovalPreferenceSequence approvalPreferenceSequence : approvalPreferenceSequences) {
				this.save(approvalPreferenceId, approvalPreferenceConditionId, approvalPreferenceSequence, approvalPreferenceConditionSaved.getRoleId());
			}
		}
	}

	/**
	 * Save the Approval Preference Sequence
	 * @param approvalPreferenceId
	 * @param approvalPreferenceConditionId
	 * @param approvalPreferenceSequence
	 */
	private void save(Long approvalPreferenceId, Long approvalPreferenceConditionId, ApprovalPreferenceSequence approvalPreferenceSequence, Long roleId) {
		String username = setupServiceClient.getLoggedInUsername();
		approvalPreferenceSequence.setApprovalPreferenceId(approvalPreferenceId);
		approvalPreferenceSequence.setConditionId(approvalPreferenceConditionId);
		
		Optional<ApprovalPreferenceSequence> oldApprovalPreferenceSequence = Optional.empty();

		if (approvalPreferenceSequence.getId() == null) {
			approvalPreferenceSequence.setCreatedBy(username);
		} else {
			// Get the existing object using the deep copy
			oldApprovalPreferenceSequence = this.approvalPreferenceSequenceRepository.findByIdAndIsDeleted(approvalPreferenceSequence.getId(), false);
			if (oldApprovalPreferenceSequence.isPresent()) {
				try {
					oldApprovalPreferenceSequence = Optional.ofNullable((ApprovalPreferenceSequence) oldApprovalPreferenceSequence.get().clone());
				} catch (CloneNotSupportedException e) {
					log.error("Error while Cloning the object. Please contact administrator.");
					throw new CustomException("Error while Cloning the object. Please contact administrator.");
				}
			}
		}

		approvalPreferenceSequence.setLastModifiedBy(username);
		ApprovalPreferenceSequence approvalPreferenceSequenceSaved = this.approvalPreferenceSequenceRepository.save(approvalPreferenceSequence);
		
		if (approvalPreferenceSequenceSaved == null) {
			log.error("Error while saving the Approval Preference Sequence.");
			throw new CustomMessageException("Error while saving the Approval Preference Sequence.");
		}
		log.info("Approval Preference Sequence is saved Successfully :: " + approvalPreferenceSequenceSaved.getId());
		
		// update the data in Approval Preference history table
		this.updateApprovalPreferenceSequenceHistory(approvalPreferenceSequenceSaved, oldApprovalPreferenceSequence);
		
		/**
		 * If old approval is exist &
		 * if old & new approver is different then we have to change the new approvers in all approvers pages
		 * - PR, PO + other forms
		 */
//		if (oldApprovalPreferenceSequence.isPresent() && !oldApprovalPreferenceSequence.get().getApproverId().equals(approvalPreferenceSequenceSaved.getApproverId())) {
//			Long updatedApproverId = approvalPreferenceSequenceSaved.getApproverId();
//			log.info("Approver is changed for the condition :: " + approvalPreferenceConditionId + ". New Approver is : " + updatedApproverId);
//			
//			List<PurchaseRequisition> purchaseRequisitions = this.purchaseRequisitionRepository.findByNextApprover(String.valueOf(oldApprovalPreferenceSequence.get().getApproverId()));
//			for (PurchaseRequisition purchaseRequisition : purchaseRequisitions) {
//				purchaseRequisition.setNextApprover(String.valueOf(updatedApproverId));
//				purchaseRequisition.setNextApproverRole(String.valueOf(roleId));
//				this.purchaseRequisitionRepository.save(purchaseRequisition);
//			}
//		}
		
		log.info("Approval Preference Sequence History is saved Successfully.");
	}
	
	/**
	 * This method save the data in history table
	 * Add entry as a Insert if Approval Preference Sequence is new 
	 * Add entry as a Update if Approval Preference Sequence is exists
	 * @param approvalPreferenceSequence
	 * @param oldApprovalPreferenceSequence
	 */
	private void updateApprovalPreferenceSequenceHistory(ApprovalPreferenceSequence approvalPreferenceSequence, Optional<ApprovalPreferenceSequence> oldApprovalPreferenceSequence) {
		if (oldApprovalPreferenceSequence.isPresent()) {
			// insert the updated fields in history table
			List<ApprovalPreferenceHistory> approvalPreferenceHistories = new ArrayList<ApprovalPreferenceHistory>();
			try {
				approvalPreferenceHistories = oldApprovalPreferenceSequence.get().compareFields(approvalPreferenceSequence);
				if (CollectionUtils.isNotEmpty(approvalPreferenceHistories)) {
					this.approvalPreferenceHistoryRepository.saveAll(approvalPreferenceHistories);
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				log.error("Error while comparing the new and old objects. Please contact administrator.");
				throw new CustomException("Error while comparing the new and old objects. Please contact administrator.");
			}
		} else {
			// Insert in history table as Operation - INSERT 
			this.approvalPreferenceHistoryRepository.save(this.prepareApprovalPreferenceHistory(approvalPreferenceSequence.getApprovalPreferenceId(), approvalPreferenceSequence.getId(), AppConstants.APPROVAL_PREFERENCE_SEQUENCE, Operation.CREATE.toString(), approvalPreferenceSequence.getLastModifiedBy(), null, String.valueOf(approvalPreferenceSequence.getId())));
		}
		log.info("Approval Preference Sequence History is updated successfully");
	}

	/**
	 * This method save the data in history table
	 * Add entry as a Insert if Approval Preference is new 
	 * Add entry as a Update if Approval Preference is exists
	 * @param approvalPreferenceSaved
	 * @param oldApprovalPreference
	 */
	private void updateApprovalPreferenceHistory(ApprovalPreference approvalPreferenceSaved, Optional<ApprovalPreference> oldApprovalPreference) {
		if (oldApprovalPreference.isPresent()) {
			// insert the updated fields in history table
			List<ApprovalPreferenceHistory> approvalPreferenceHistories = new ArrayList<ApprovalPreferenceHistory>();
			try {
				approvalPreferenceHistories = oldApprovalPreference.get().compareFields(approvalPreferenceSaved);
				if (CollectionUtils.isNotEmpty(approvalPreferenceHistories)) {
					this.approvalPreferenceHistoryRepository.saveAll(approvalPreferenceHistories);
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				log.error("Error while comparing the new and old objects. Please contact administrator.");
				throw new CustomException("Error while comparing the new and old objects. Please contact administrator.");
			}
		} else {
			// Insert in history table as Operation - INSERT 
			this.approvalPreferenceHistoryRepository.save(this.prepareApprovalPreferenceHistory(approvalPreferenceSaved.getId(), null, AppConstants.APPROVAL_PREFERENCE, Operation.CREATE.toString(), approvalPreferenceSaved.getLastModifiedBy(), null, String.valueOf(approvalPreferenceSaved.getId())));
		}
		log.info("Approval Preference Item History is updated successfully");
	}
	
	/**
	 *  * This method save the data in history table
	 * Add entry as a Insert if Approval Preference Condition is new 
	 * Add entry as a Update if Approval Preference Condition is exists
	 * @param approvalPreferenceCondition
	 * @param oldApprovalPreferenceCondition
	 */
	private void updateApprovalPreferenceConditionHistory(ApprovalPreferenceCondition approvalPreferenceCondition, Optional<ApprovalPreferenceCondition> oldApprovalPreferenceCondition) {
		if (oldApprovalPreferenceCondition.isPresent()) {
			// insert the updated fields in history table
			List<ApprovalPreferenceHistory> approvalPreferenceHistories = new ArrayList<ApprovalPreferenceHistory>();
			try {
				approvalPreferenceHistories = oldApprovalPreferenceCondition.get().compareFields(approvalPreferenceCondition);
				if (CollectionUtils.isNotEmpty(approvalPreferenceHistories)) {
					this.approvalPreferenceHistoryRepository.saveAll(approvalPreferenceHistories);
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				log.error("Error while comparing the new and old objects. Please contact administrator.");
				throw new CustomException("Error while comparing the new and old objects. Please contact administrator.");
			}
		} else {
			// Insert in history table as Operation - INSERT 
			this.approvalPreferenceHistoryRepository.save(this.prepareApprovalPreferenceHistory(approvalPreferenceCondition.getApprovalPreferenceId(), approvalPreferenceCondition.getId(), AppConstants.APPROVAL_PREFERENCE_CONDITION, Operation.CREATE.toString(), approvalPreferenceCondition.getLastModifiedBy(), null, String.valueOf(approvalPreferenceCondition.getId())));
		}
		log.info("Approval Preference Condition History is updated successfully");
	}

	/**
	 * Prepares the history for the Approval Preference
	 * @param approvalPreferenceId
	 * @param childId
	 * @param moduleName
	 * @param operation
	 * @param lastModifiedBy
	 * @param oldValue
	 * @param newValue
	 * @return
	 */
	public ApprovalPreferenceHistory prepareApprovalPreferenceHistory(Long approvalPreferenceId, Long childId, String moduleName, String operation, String lastModifiedBy, String oldValue, String newValue) {
		ApprovalPreferenceHistory approvalPreferenceHistory = new ApprovalPreferenceHistory();
		approvalPreferenceHistory.setApprovalPreferenceId(approvalPreferenceId);
		approvalPreferenceHistory.setChildId(childId);
		approvalPreferenceHistory.setModuleName(moduleName);
		approvalPreferenceHistory.setChangeType(AppConstants.UI);
		approvalPreferenceHistory.setOperation(operation);
		approvalPreferenceHistory.setOldValue(oldValue);
		approvalPreferenceHistory.setNewValue(newValue);
		approvalPreferenceHistory.setLastModifiedBy(lastModifiedBy);
		return approvalPreferenceHistory;
	}
	
	@Override
	public ApprovalPreference getApprovalPreferenceById(Long id) {		
		// Step - 1 - Find Approval Preference
		Optional<ApprovalPreference> approvalPreference = Optional.empty();
		approvalPreference = this.approvalPreferenceRepository.findByIdAndIsDeleted(id, false);
		
		if (!approvalPreference.isPresent()) {
			log.error("Approval Preference is not found against given id - " + id);
			throw new CustomMessageException("Approval Preference is not found against given id - " + id);
		}
		
		// Step - 2 - Find Approval Preference Conditions
		List<ApprovalPreferenceCondition> approvalPreferenceConditions = new ArrayList<ApprovalPreferenceCondition>();
		approvalPreferenceConditions = this.approvalPreferenceConditionRepository.findByApprovalPreferenceIdAndIsDeleted(id, false);
		
		// Step - 3 - Find Approval Preference Sequences for each Condition
		if (CollectionUtils.isNotEmpty(approvalPreferenceConditions)) {
			for (ApprovalPreferenceCondition approvalPreferenceCondition : approvalPreferenceConditions) {
				List<ApprovalPreferenceSequence> approvalPreferenceSequences = new ArrayList<ApprovalPreferenceSequence>();
				approvalPreferenceSequences = this.approvalPreferenceSequenceRepository.findByConditionIdAndIsDeleted(approvalPreferenceCondition.getId(), false);
				if (CollectionUtils.isNotEmpty(approvalPreferenceSequences)) {
					approvalPreferenceSequences.sort(Comparator.comparing(ApprovalPreferenceSequence::getSequenceId));
					approvalPreferenceCondition.setApprovalPreferenceSequences(approvalPreferenceSequences);
				}
			}
		}
		approvalPreference.get().setApprovalPreferenceConditions(approvalPreferenceConditions);
		
		return approvalPreference.get();
	}

	/**
	 * This function find the max level up to which conditions/approvers will match/approve
	 * IF present
	 * 		return level & sequence 
	 * 		note - update the form with same at child level
	 * ELSE
	 * 		throw message - No Approver Process is Exist
	 */
	@Override
	public ApprovalPreference findApproverMaxLevel(ApprovalRequest approvalRequest) {
		// prepare where clause
		Long subsidiaryId = approvalRequest.getSubsidiaryId();
		String formName = approvalRequest.getFormName();
		Double transactionAmount = approvalRequest.getTransactionAmount();
		Long locationId = approvalRequest.getLocationId();
		String department = approvalRequest.getDepartment();
		String natureOfSupply = approvalRequest.getNatureOfSupply();
		
		StringBuilder whereClause = new StringBuilder("");
		whereClause
			.append(" AND ap.subsidiaryId = ").append(subsidiaryId).append(" ")
			.append(" AND ap.subType = '").append(formName).append("' ");
		
		// Transaction Amount
		if (transactionAmount == null || transactionAmount == 0) {
			whereClause.append(" AND aps.amountFrom is null AND aps.amountTo is null ");
		} else {
			whereClause.append(" AND aps.amountFrom <= ").append(transactionAmount).append(" AND aps.amountTo >= ").append(transactionAmount).append(" ");
		}
		
		// Location
		if (locationId == null || locationId == 0) {
			whereClause.append(" AND aps.locationId is null ");
		} else {
			whereClause.append(" AND (aps.locationId is null or aps.locationId = ").append(locationId).append(") ");
		}
		
		// Department is Optional
		if (StringUtils.isEmpty(department)) {
			whereClause.append(" AND aps.department is null ");
		} else {
			whereClause.append(" AND (aps.department is null or aps.department = '").append(department).append("') ");
		}
		
		// Nature of Supply is Optional For supplier
		if (StringUtils.isEmpty(natureOfSupply)) {
			whereClause.append(" AND aps.natureOfSupply is null ");
		} else {
			whereClause.append(" AND aps.natureOfSupply = '").append(natureOfSupply).append("' ");
		}
			
		// fetch Max level by subsidiary and form name
//		Optional<ApprovalPreference> approvalPreference = this.approvalPreferenceRepository.findApproverMaxLevel(approvalRequest.getSubsidiaryId(), approvalRequest.getFormName(), approvalRequest.getTransactionAmount(), approvalRequest.getLocationId(), approvalRequest.getDepartment());
		
		List<ApprovalPreference> approvalPreferences = this.approvalPreferenceDao.findApproverMaxLevel(whereClause.toString());
		
		if (CollectionUtils.isEmpty(approvalPreferences)) {
			log.error("No approval process is found against the form - " + approvalRequest.getFormName());
			throw new CustomMessageException("No approval process is found against the form - " + approvalRequest.getFormName());
		}
		
		ApprovalPreference approvalPreference = new ApprovalPreference();
		approvalPreference = approvalPreferences.get(0);
		
		if (approvalPreference.getSequenceId() == null || StringUtils.isEmpty(approvalPreference.getLevel())) {
			log.error("Approval process(sequence-id or Level) is not valid against the form - " + approvalRequest.getFormName());
			throw new CustomMessageException("Approval process is not valid against the form - " + approvalRequest.getFormName() + ". Please validate the approval Data.");
		}
		
		return approvalPreference;
	}

	/**
	 * This method finds the approver with level & Role using the approval preference id, level & sequence Id
	 * if not found then return error message
	 */
	@Override
	public ApprovalRequest findApproverByLevelAndSequence(Long id, String level, Long sequenceId) {
		Optional<ApprovalPreference> approvalPreference = this.approvalPreferenceRepository.findApproverByLevelAndSequence(id, level, sequenceId);
		
		// if level is not found then throw message - Approval level process is not found for level - :level
		if (!approvalPreference.isPresent()) {
			log.error("Approval level process is not found for level : " + level);
			throw new CustomMessageException("Approval level process is not found for level : " + level);
		}
		
		ApprovalRequest approvalRequest = new ApprovalRequest();
		approvalRequest.setNextApprover(String.valueOf(approvalPreference.get().getApproverId()));
		approvalRequest.setNextApproverRole(String.valueOf(approvalPreference.get().getRoleId()));
		approvalRequest.setNextApproverLevel(approvalPreference.get().getLevel());
		
		return approvalRequest;
	}

	@Override
	public String findPreferenceTypeById(Long approvalPreferenceId) {
		String approvalPreferenceType = this.approvalPreferenceRepository.findApprovalPreferenceTypeById(approvalPreferenceId);
		if (StringUtils.isEmpty(approvalPreferenceType)) {
			log.error("Approval Type Should not contain empty. Please check your configuration for Preference ID - " + approvalPreferenceId);
			throw new CustomMessageException("Approval Type Should not contain empty. Please check your configuration for Preference ID - " + approvalPreferenceId);
		}
		return approvalPreferenceType;
	}

	@Override
	public PaginationResponse findAll(PaginationRequest paginationRequest) {
		List<ApprovalPreference> approvalPreferences = new ArrayList<ApprovalPreference>();

		// preparing where clause
		String whereClauses = this.prepareWhereClauses(paginationRequest).toString();

		// get list
		approvalPreferences = this.approvalPreferenceDao.findAll(whereClauses, paginationRequest);

		// getting count
		Long totalRecords = this.approvalPreferenceDao.getCount(whereClauses);

		return CommonUtils.setPaginationResponse(paginationRequest.getPageNumber(), paginationRequest.getPageSize(),
				approvalPreferences, totalRecords);
	}

	private Object prepareWhereClauses(PaginationRequest paginationRequest) {
		String subType = null;
		String recordType = null;
		Long subsidiaryId = null;
		
		Map<String, ?> filters = paginationRequest.getFilters();

		if (filters.containsKey(FilterNames.SUBTYPE))
			subType = (String) filters.get(FilterNames.SUBTYPE);
		if (filters.containsKey(FilterNames.RECORDTYPE))
			recordType = (String) filters.get(FilterNames.RECORDTYPE);
		if (filters.containsKey(FilterNames.SUBSIDIARY_ID))
			subsidiaryId = ((Number) filters.get(FilterNames.SUBSIDIARY_ID)).longValue();
		
		StringBuilder whereClauses = new StringBuilder(" AND ap.isDeleted is false");
		if (StringUtils.isNotEmpty(subType)) {
			whereClauses.append(" AND lower(ap.subType) like lower('%").append(subType).append("%')");
		}
		if (StringUtils.isNotEmpty(recordType)) {
			whereClauses.append(" AND lower(ap.recordType) like lower('%").append(recordType).append("%')");
		}
		if (subsidiaryId != null && subsidiaryId != 0) {
			whereClauses.append(" AND ap.subsidiaryId = ").append(subsidiaryId);
		}
		return whereClauses;
	}

}
