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
@Table(	name = "employee_role")
@ToString
@Audited
@AuditTable("employee_role_aud")
public class EmployeeRole implements Cloneable {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name="employee_id")
	private Long employeeId;
	
	@Column(name="employee_number")
	private String employeeNumber;
	
	private Long roleId;
	
	private String roleName;

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
	 * @param employeeAccess
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public List<EmployeeHistory> compareFields(EmployeeRole employeeAccess)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		List<EmployeeHistory> employeeHistories = new ArrayList<EmployeeHistory>();
		Field[] fields = this.getClass().getDeclaredFields();

		for (Field field : fields) {
			String fieldName = field.getName();

			if (!CommonUtils.getUnusedFieldsOfHistory().contains(fieldName.toLowerCase())) {
				Object oldValue = field.get(this);
				Object newValue = field.get(employeeAccess);

				if (oldValue == null) {
					if (newValue != null) {
						employeeHistories.add(this.prepareEmployeeHistory(employeeAccess, field));
					}
				} else if (!oldValue.equals(newValue)) {
					employeeHistories.add(this.prepareEmployeeHistory(employeeAccess, field));
				}
			}
		}
		return employeeHistories;
	}

	private EmployeeHistory prepareEmployeeHistory(EmployeeRole employeeRole, Field field) throws IllegalAccessException {
		EmployeeHistory employeeHistory = new EmployeeHistory();
		employeeHistory.setEmployeeNumber(employeeRole.getEmployeeNumber());
		employeeHistory.setChildId(employeeRole.getId());
		employeeHistory.setModuleName(AppConstants.EMPLOYEE_ROLE);
		employeeHistory.setChangeType(AppConstants.UI);
		employeeHistory.setOperation(Operation.UPDATE.toString());
		employeeHistory.setLastModifiedBy(employeeRole.getLastModifiedBy());
		employeeHistory.setFieldName(CommonUtils.splitCamelCaseWithCapitalize(field.getName()));
		if (field.get(this) != null) employeeHistory.setOldValue(field.get(this).toString());
		if (field.get(employeeRole) != null) employeeHistory.setNewValue(field.get(employeeRole).toString());
		return employeeHistory;
	}
}

