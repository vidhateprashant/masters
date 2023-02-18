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
@Table(	name = "employee_contact")
@ToString
@Audited
@AuditTable("employee_contact_aud")
public class EmployeeContact implements Cloneable{

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name="employee_id")
	private Long employeeId;
	
	@Column(name="employee_number")
	private String employeeNumber;
	
	private String email;
	
	private String mobile;
	
	@Column(name = "office_number")
	private String officeNumber;
	
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
	 * @param employee
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public List<EmployeeHistory> compareFields(EmployeeContact employeeContact)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		List<EmployeeHistory> employeeHistories = new ArrayList<EmployeeHistory>();
		Field[] fields = this.getClass().getDeclaredFields();

		for (Field field : fields) {
			String fieldName = field.getName();

			if (!CommonUtils.getUnusedFieldsOfHistory().contains(fieldName.toLowerCase())) {
				Object oldValue = field.get(this);
				Object newValue = field.get(employeeContact);

				if (oldValue == null) {
					if (newValue != null) {
						employeeHistories.add(this.prepareEmployeeHistory(employeeContact, field));
					}
				} else if (!oldValue.equals(newValue)) {
					employeeHistories.add(this.prepareEmployeeHistory(employeeContact, field));
				}
			}
		}
		return employeeHistories;
	}
	
	private EmployeeHistory prepareEmployeeHistory(EmployeeContact employeeContact, Field field) throws IllegalAccessException {
		EmployeeHistory employeeHistory = new EmployeeHistory();
		employeeHistory.setEmployeeNumber(employeeContact.getEmployeeNumber());
		employeeHistory.setChildId(employeeContact.getId());
		employeeHistory.setModuleName(AppConstants.EMPLOYEE_CONTACT);
		employeeHistory.setChangeType(AppConstants.UI);
		employeeHistory.setLastModifiedBy(employeeContact.getLastModifiedBy());
		employeeHistory.setOperation(Operation.UPDATE.toString());
		employeeHistory.setFieldName(CommonUtils.splitCamelCaseWithCapitalize(field.getName()));
		if (field.get(this) != null)
		employeeHistory.setOldValue(field.get(this).toString());
		if (field.get(employeeContact) != null)
		employeeHistory.setNewValue(field.get(employeeContact).toString());			//null check
		return employeeHistory;
	}
}
