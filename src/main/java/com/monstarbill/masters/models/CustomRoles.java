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
import javax.validation.constraints.NotBlank;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

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
@Table(	name = "custom_roles")
@ToString
@Audited
@AuditTable("custom_roles_aud")
public class CustomRoles implements Cloneable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "Role name is Mandatory")
	@Column(unique = true)
	private String name;
	
	@Column(name = "subsidiary_id")
	private Long subsidiaryId;
	
	@Column(name = "selected_role")
	private String selectedAccess;

	@Column(name="is_active")
	private boolean isActive;
	
	@Column(name="is_deleted", columnDefinition = "boolean default false")
	private boolean isDeleted;
	
	@CreationTimestamp
	@Column(name="created_date", updatable = false)
	private Date createdDate;

	@CreatedBy
	@Column(name="created_by", updatable = false)
	private String createdBy;

	@UpdateTimestamp
	@Column(name="last_modified_date")
	private Timestamp lastModifiedDate;

	@LastModifiedBy
	@Column(name="last_modified_by")
	private String lastModifiedBy;
	
	@Transient
	private String subsidiaryName;
	
	@Transient
	private List<RolesDepartment> restrictedDepartments;
	
	@Transient
	private List<RolePermissions> rolePermissions;
	
	@Transient
	private String status;

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	/**
	 * Compare the fields and values of 2 objects in order to find out the
	 * difference between old and new value
	 * 
	 * @param customRole
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public List<RolesHistory> compareFields(CustomRoles customRole) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		List<RolesHistory> rolesHistories = new ArrayList<RolesHistory>();
		Field[] fields = this.getClass().getDeclaredFields();

		for (Field field : fields) {
			String fieldName = field.getName();

			if (!CommonUtils.getUnusedFieldsOfHistory().contains(fieldName.toLowerCase())) {
				Object oldValue = field.get(this);
				Object newValue = field.get(customRole);

				if (oldValue == null) {
					if (newValue != null) {
						rolesHistories.add(this.prepareRolesHistory(customRole, field));
					}
				} else if (!oldValue.equals(newValue)) {
					rolesHistories.add(this.prepareRolesHistory(customRole, field));
				}
			}
		}
		return rolesHistories;
	}

	private RolesHistory prepareRolesHistory(CustomRoles customRoles, Field field) throws IllegalAccessException {
		RolesHistory rolesHistory = new RolesHistory();
		rolesHistory.setRoleId(customRoles.getId());
		rolesHistory.setModuleName(AppConstants.ROLE);
		rolesHistory.setChangeType(AppConstants.UI);
		rolesHistory.setOperation(Operation.UPDATE.toString());
		rolesHistory.setFieldName(CommonUtils.splitCamelCaseWithCapitalize(field.getName()));
		if (field.get(this) != null) {
			rolesHistory.setOldValue(field.get(this).toString());
		}
		if (field.get(customRoles) != null) {
			rolesHistory.setNewValue(field.get(customRoles).toString());
		}
		rolesHistory.setLastModifiedBy(customRoles.getLastModifiedBy());
		return rolesHistory;
	}
	
	/**
	 * used to set the data for the list
	 */
	public CustomRoles(Long id, Long subsidiaryId,boolean isActive, String name, Date createdDate, String createdBy, String subsidiaryName) {
		this.id = id;
		this.subsidiaryId = subsidiaryId;
		this.isActive = isActive;
		this.name = name;
		this.createdDate = createdDate;
		this.createdBy = createdBy;
		this.subsidiaryName = subsidiaryName;
	}

	public CustomRoles(Long id,String name, String selectedAccess, boolean isActive) {
		this.id = id;
		this.name = name;
		this.selectedAccess = selectedAccess;
		this.isActive = isActive;
	}
	
}
