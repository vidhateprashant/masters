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
@Table(	name = "role_permissions")
@ToString
@Audited
@AuditTable("role_permissions_aud")
public class RolePermissions implements Cloneable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "role_id")
	private Long roleId;
	
	@Column(name = "module_name")
	private String moduleName;
	
	@Column(name = "access_point")
	private String accessPoint;
	
	@Column(name="is_view", columnDefinition = "boolean default false")
	private boolean isView;
	
	@Column(name="is_create", columnDefinition = "boolean default false")
	private boolean isCreate;
	
	@Column(name="is_edit", columnDefinition = "boolean default false")
	private boolean isEdit;

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

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	/**
	 * Compare the fields and values of 2 objects in order to find out the
	 * difference between old and new value
	 * 
	 * @param rolePermissions
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public List<RolesHistory> compareFields(RolePermissions rolePermissions)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		List<RolesHistory> rolesHistories = new ArrayList<RolesHistory>();
		Field[] fields = this.getClass().getDeclaredFields();

		for (Field field : fields) {
			String fieldName = field.getName();

			if (!CommonUtils.getUnusedFieldsOfHistory().contains(fieldName.toLowerCase())) {
				Object oldValue = field.get(this);
				Object newValue = field.get(rolePermissions);

				if (oldValue == null) {
					if (newValue != null) {
						rolesHistories.add(this.prepareRolePermissionsHistory(rolePermissions, field));
					}
				} else if (!oldValue.equals(newValue)) {
					rolesHistories.add(this.prepareRolePermissionsHistory(rolePermissions, field));
				}
			}
		}
		return rolesHistories;
	}

	private RolesHistory prepareRolePermissionsHistory(RolePermissions rolePermissions, Field field) throws IllegalAccessException {
		RolesHistory rolesHistory = new RolesHistory();
		rolesHistory.setRoleId(rolePermissions.getRoleId());
		rolesHistory.setChildId(rolePermissions.getId());
		rolesHistory.setModuleName(AppConstants.ROLE_ACCESS_POINT);
		rolesHistory.setChangeType(AppConstants.UI);
		rolesHistory.setOperation(Operation.UPDATE.toString());
		rolesHistory.setFieldName(CommonUtils.splitCamelCaseWithCapitalize(field.getName()));
		if (field.get(this) != null) {
			rolesHistory.setOldValue(field.get(this).toString());
		}
		if (field.get(rolePermissions) != null) {
			rolesHistory.setNewValue(field.get(rolePermissions).toString());
		}
		rolesHistory.setLastModifiedBy(rolePermissions.getLastModifiedBy());
		return rolesHistory;
	}
}