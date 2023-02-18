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
@Table(	name = "approval_preference_sequence")
@ToString
@Audited
@AuditTable("approval_preference_sequence_aud")
public class ApprovalPreferenceSequence implements Cloneable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "approval_preference_id")
	private Long approvalPreferenceId;
	
	// primary key of condition table
	@Column(name = "condition_id")
	private Long conditionId;
	
	@Column(name = "sequence_id")
	private Long sequenceId;

	@Column(name = "amount_from")
	private Double amountFrom;

	@Column(name = "amount_to")
	private Double amountTo;

	@Column(name = "location_id")
	private Long locationId;

	private String department;
	
	@Column(name = "nature_of_supply")
	private String natureOfSupply;

	@Column(name = "approver_id")
	private Long approverId;

	@Column(name = "parallel_approver_level")
	private String parallelApproverLevel;
	
	@Column(name="is_active", columnDefinition = "boolean default true")
	private boolean isActive;

	@Column(name="is_deleted", columnDefinition = "boolean default false")
	private boolean isDeleted;
	
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
	public List<ApprovalPreferenceHistory> compareFields(ApprovalPreferenceSequence approvalPreference) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

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

	private ApprovalPreferenceHistory prepareApprovalPreferenceHistory(ApprovalPreferenceSequence approvalPreference, Field field) throws IllegalAccessException {
		ApprovalPreferenceHistory approvalPreferenceHistory = new ApprovalPreferenceHistory();
		approvalPreferenceHistory.setApprovalPreferenceId(approvalPreference.getApprovalPreferenceId());
		approvalPreferenceHistory.setChildId(approvalPreference.getId());
		approvalPreferenceHistory.setModuleName(AppConstants.APPROVAL_PREFERENCE);
		approvalPreferenceHistory.setChangeType(AppConstants.UI);
		approvalPreferenceHistory.setOperation(Operation.UPDATE.toString());
		approvalPreferenceHistory.setFieldName(CommonUtils.splitCamelCaseWithCapitalize(field.getName()));
		if (field.get(this) != null) approvalPreferenceHistory.setOldValue(field.get(this).toString());
		if (field.get(approvalPreference) != null) approvalPreferenceHistory.setNewValue(field.get(approvalPreference).toString());
		approvalPreferenceHistory.setLastModifiedBy(approvalPreference.getLastModifiedBy());
		return approvalPreferenceHistory;
	}
}