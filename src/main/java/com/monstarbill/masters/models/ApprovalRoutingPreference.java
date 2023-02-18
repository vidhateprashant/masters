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
@ToString
@Audited
@Table(	name = "approval_routing_preference")
@AuditTable("approval_routing_preference_aud")
public class ApprovalRoutingPreference implements Cloneable{
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name="preference_id")
	private Long preferenceId;
	
	@Column(name="form_type")
	private String formType;
	
	@Column(name="form_name")
	private String formName;
	
	@Column(name="is_routing_active", columnDefinition = "boolean default false")
	private boolean isRoutingActive;
	
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
	 * @param supplier
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public List<ApprovalPreferenceHistory> compareFields(ApprovalRoutingPreference approvalRoutingPreference)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		List<ApprovalPreferenceHistory> preferencesHistories = new ArrayList<ApprovalPreferenceHistory>();
		Field[] fields = this.getClass().getDeclaredFields();

		for (Field field : fields) {
			String fieldName = field.getName();

			if (!CommonUtils.getUnusedFieldsOfHistory().contains(fieldName.toLowerCase())) {
				Object oldValue = field.get(this);
				Object newValue = field.get(approvalRoutingPreference);

				if (oldValue == null) {
					if (newValue != null) {
						preferencesHistories.add(this.preparePreferencesHistory(approvalRoutingPreference, field));
					}
				} else if (!oldValue.equals(newValue)) {
					preferencesHistories.add(this.preparePreferencesHistory(approvalRoutingPreference, field));
				}
			}
		}
		return preferencesHistories;
	}
	
	private ApprovalPreferenceHistory preparePreferencesHistory(ApprovalRoutingPreference approvalRoutingPreference, Field field) throws IllegalAccessException {
		ApprovalPreferenceHistory preferencesHistory = new ApprovalPreferenceHistory();
		preferencesHistory.setApprovalPreferenceId(approvalRoutingPreference.getPreferenceId());
		preferencesHistory.setChildId(approvalRoutingPreference.getId());
		preferencesHistory.setModuleName(AppConstants.APPROVAL_ROUTING_PREFERENCE);
		preferencesHistory.setChangeType(AppConstants.UI);
		preferencesHistory.setLastModifiedBy(approvalRoutingPreference.getLastModifiedBy());
		preferencesHistory.setOperation(Operation.UPDATE.toString());
		preferencesHistory.setFieldName(CommonUtils.splitCamelCaseWithCapitalize(field.getName()));
		if (field.get(this) != null) preferencesHistory.setOldValue(field.get(this).toString());
		if (field.get(approvalRoutingPreference) != null) preferencesHistory.setNewValue(field.get(approvalRoutingPreference).toString());
		return preferencesHistory;
	}
}
