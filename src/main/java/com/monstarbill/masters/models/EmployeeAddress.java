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
import javax.validation.constraints.NotBlank;

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
@Table(	name = "employee_address")
@ToString
@Audited
@AuditTable("employee_address_aud")
public class EmployeeAddress implements Cloneable{

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name="employee_id")
	private Long employeeId;
	
	@Column(name="employee_number")
	private String employeeNumber;
	
	@NotBlank(message = "Address1 is mandatory")
	private String address1;

	private String address2;

	private String city;

	private String state;

	private int pin;
	
	@NotBlank(message = "Country is mandatory")
	private String country;
	
	@Column(name = "is_deleted", columnDefinition = "boolean default false")
	private boolean isDeleted;
	
	@CreationTimestamp
	@Column(name="created_date", updatable = false)
	private Date createdDate;

	@Column(name="created_by")
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
	public List<EmployeeHistory> compareFields(EmployeeAddress employeeAddress)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		List<EmployeeHistory> employeeHistories = new ArrayList<EmployeeHistory>();
		Field[] fields = this.getClass().getDeclaredFields();

		for (Field field : fields) {
			String fieldName = field.getName();

			if (!CommonUtils.getUnusedFieldsOfHistory().contains(fieldName.toLowerCase())) {
				Object oldValue = field.get(this);
				Object newValue = field.get(employeeAddress);

				if (oldValue == null) {
					if (newValue != null) {
						employeeHistories.add(this.prepareEmployeeHistory(employeeAddress, field));
					}
				} else if (!oldValue.equals(newValue)) {
					employeeHistories.add(this.prepareEmployeeHistory(employeeAddress, field));
				}
			}
		}
		return employeeHistories;
	}
	
	private EmployeeHistory prepareEmployeeHistory(EmployeeAddress employeeAddress, Field field) throws IllegalAccessException {
		EmployeeHistory employeeHistory = new EmployeeHistory();
		employeeHistory.setEmployeeNumber(employeeAddress.getEmployeeNumber());
		employeeHistory.setChildId(employeeAddress.getId());
		employeeHistory.setModuleName(AppConstants.EMPLOYEE_ADDRESS);
		employeeHistory.setChangeType(AppConstants.UI);
		employeeHistory.setLastModifiedBy(employeeAddress.getLastModifiedBy());
		employeeHistory.setOperation(Operation.UPDATE.toString());
		employeeHistory.setFieldName(CommonUtils.splitCamelCaseWithCapitalize(field.getName()));
		if (field.get(this) != null)
			employeeHistory.setOldValue(field.get(this).toString());
		if (field.get(employeeAddress) != null)
			employeeHistory.setNewValue(field.get(employeeAddress).toString());
		employeeHistory.setLastModifiedBy(employeeAddress.getLastModifiedBy());
		return employeeHistory;
	}
}
