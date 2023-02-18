package com.monstarbill.masters.models;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;

import com.monstarbill.masters.commons.AppConstants;
import com.monstarbill.masters.commons.CommonUtils;
import com.monstarbill.masters.enums.Operation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(	name = "approval_preference")
@ToString
@Audited
@AuditTable("approval_preference_aud")
public class ApprovalPreference implements Cloneable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull(message = "Subsidiary is mandatory")
	@Column(name = "subsidiary_id", nullable = false)
	private Long subsidiaryId;

	@Column(name = "approval_type")
	private String approvalType;

	@Column(name = "record_type")
	private String recordType;

	@Column(name = "sub_type")
	private String subType;
	
	@Column(name="is_deleted", columnDefinition = "boolean default false")
	private boolean isDeleted;
	
	@Column(name="is_active", columnDefinition = "boolean default true")
	private boolean isActive;
	
	@Column(name = "inactive_date")
	private Date inactiveDate;
	
	@CreationTimestamp
	@Column(name="created_date", updatable = false)
	private Date createdDate;

	@Column(name="created_by", updatable = false)
	private String createdBy;

	@UpdateTimestamp
	@Column(name="last_modified_date")
	private Timestamp lastModifiedDate;

	@Column(name="last_modified_by")
	private String lastModifiedBy;
	
	@Transient
	private List<ApprovalPreferenceCondition> approvalPreferenceConditions;
	
	@Transient
	private Long approverPreferenceConditionId;

	@Transient
	private Long approverPreferenceSequenceId;

	@Transient
	private Long approverId;

	@Transient
	private Long roleId;
	
	@Transient
	private Long sequenceId;
	
	@Transient
	private String level;
	
	@Transient
	private String subsidiaryName;
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	/**
	 * Compare the fields and values of 2 objects in order to find out the
	 * difference between old and new value
	 * 
	 * @param approvalPreference
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public List<ApprovalPreferenceHistory> compareFields(ApprovalPreference approvalPreference) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		List<ApprovalPreferenceHistory> approvalPreferenceHistories = new ArrayList<ApprovalPreferenceHistory>();
		Field[] fields = this.getClass().getDeclaredFields();

		for (Field field : fields) {
			String fieldName = field.getName();

			if (!CommonUtils.getUnusedFieldsOfHistory().contains(fieldName.toLowerCase())) {
				Object oldValue = field.get(this);
				Object newValue = field.get(approvalPreference);

				if (oldValue == null) {
					if (newValue != null) {
						approvalPreferenceHistories.add(this.prepareApprovalPreferenceHistory(approvalPreference, field));
					}
				} else if (!oldValue.equals(newValue)) {
					approvalPreferenceHistories.add(this.prepareApprovalPreferenceHistory(approvalPreference, field));
				}
			}
		}
		return approvalPreferenceHistories;
	}

	private ApprovalPreferenceHistory prepareApprovalPreferenceHistory(ApprovalPreference approvalPreference, Field field) throws IllegalAccessException {
		ApprovalPreferenceHistory approvalPreferenceHistory = new ApprovalPreferenceHistory();
		approvalPreferenceHistory.setApprovalPreferenceId(approvalPreference.getId());
		approvalPreferenceHistory.setModuleName(AppConstants.APPROVAL_PREFERENCE);
		approvalPreferenceHistory.setChangeType(AppConstants.UI);
		approvalPreferenceHistory.setOperation(Operation.UPDATE.toString());
		approvalPreferenceHistory.setFieldName(CommonUtils.splitCamelCaseWithCapitalize(field.getName()));
		if (field.get(this) != null) approvalPreferenceHistory.setOldValue(field.get(this).toString());
		if (field.get(approvalPreference) != null) approvalPreferenceHistory.setNewValue(field.get(approvalPreference).toString());
		approvalPreferenceHistory.setLastModifiedBy(approvalPreference.getLastModifiedBy());
		return approvalPreferenceHistory;
	}
	
	/**
	 * Constructor used when user finds the 
	 * 1. specific approver
	 * 2. findApproverByLevelAndSequence
	 * @param id
	 * @param conditionId
	 * @param sequenceId
	 * @param approverId
	 * @param roleId
	 */
	public ApprovalPreference(Long id, Long conditionId, Long sequenceId, Long approverId, Long roleId, String level) {
		this.id = id;
		this.approverPreferenceConditionId = conditionId;
		this.approverPreferenceSequenceId = sequenceId;
		this.approverId = approverId;
		this.roleId = roleId;
		this.level = level;
	}
	
	/**
	 * this used to find sequence id & max level
	 * @param approverSequenceId
	 * @param sequenceId
	 * @param level
	 */
	public ApprovalPreference(Long id, Long approverSequenceId, Long sequenceId, String level, String approvalType) {
		this.id = id;
		this.approverPreferenceSequenceId = approverSequenceId;
		this.sequenceId = sequenceId;
		this.level = level;
		this.approvalType = approvalType;
	}

	public ApprovalPreference(Long id,Long subsidiaryId, String approvalType, String recordType, String subType, String subsidiaryName) {
		this.id = id;
		this.subsidiaryId = subsidiaryId;
		this.approvalType = approvalType;
		this.recordType = recordType;
		this.subType = subType;
		this.subsidiaryName = subsidiaryName;
	}
	
	
}