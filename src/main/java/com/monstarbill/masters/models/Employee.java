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
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;

import com.monstarbill.masters.commons.AppConstants;
import com.monstarbill.masters.commons.CommonUtils;
import com.monstarbill.masters.enums.Operation;
import com.monstarbill.masters.enums.Status;

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
@Table(name = "employee")
@ToString
@Audited
@AuditTable("employee_aud")

public class Employee implements Cloneable {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name="subsidiary_id")
	private Long subsidiaryId; 

	private String accountId;

	private String initials;
	
	@NotBlank(message = "Designation is mandatory")
	private String designation;
	
	@Column(name="employee_number", unique = true)
	private String employeeNumber;
	
	@Column(unique = true)
	private String email;
	
	private String department;
	
	private String salutation;
	
	@Column( unique = true)
	private String pan;
	
	private String currency;
	
	@Column(name="first_name")
	private String firstName;				
	
	@Column(name="middle_name")
	private String middleName;
	
	@Column(name="last_name")
	private String lastName;
	
	@Column(name="full_name", unique = true)
	private String fullName;
	
	private String supervisor;
	
	@Column(name = "signature_metadata")
	private String signatureMetadata;
	
	@Lob
	private byte[] signature;
	
	@Column(name = "image_metadata")
	private String imageMetadata;
	
	@Lob
	private byte[] image;
	
	@Column(name = "is_active", columnDefinition = "boolean default true")
	private boolean isActive;
	
	@Column(name="active_date")
	private Date activeDate;
	
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
	
	@Transient
	private EmployeeContact employeeContact;

	@Transient
	private EmployeeAccounting employeeAccounting;

	@Transient
	private List<EmployeeAddress> employeeAddresses;

	@Transient
	private EmployeeAccess employeeAccess;
	
	@Transient
	private String status;
	
	@Transient
	private String contactNumber;

	@Transient
	private boolean isAccess;
	
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
	public List<EmployeeHistory> compareFields(Employee employee)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		List<EmployeeHistory> employeeHistories = new ArrayList<EmployeeHistory>();
		Field[] fields = this.getClass().getDeclaredFields();

		for (Field field : fields) {
			String fieldName = field.getName();

			if (!CommonUtils.getUnusedFieldsOfHistory().contains(fieldName.toLowerCase())) {
				Object oldValue = field.get(this);
				Object newValue = field.get(employee);

				if (oldValue == null) {
					if (newValue != null) {
						employeeHistories.add(this.prepareEmployeeHistory(employee, field));
					}
				} else if (!oldValue.equals(newValue)) {
					employeeHistories.add(this.prepareEmployeeHistory(employee, field));
				}
			}
		}
		return employeeHistories;
	}
	
	private EmployeeHistory prepareEmployeeHistory(Employee employee, Field field) throws IllegalAccessException {
		EmployeeHistory employeeHistory = new EmployeeHistory();
		employeeHistory.setEmployeeNumber(employee.getEmployeeNumber());
		employeeHistory.setModuleName(AppConstants.EMPLOYEE);
		employeeHistory.setChangeType(AppConstants.UI);
		employeeHistory.setLastModifiedBy(employee.getLastModifiedBy());
		employeeHistory.setOperation(Operation.UPDATE.toString());
		employeeHistory.setFieldName(CommonUtils.splitCamelCaseWithCapitalize(field.getName()));
		if (field.get(this) != null) employeeHistory.setOldValue(field.get(this).toString());
		if (field.get(employee) != null) employeeHistory.setNewValue(field.get(employee).toString());
		return employeeHistory;
	}
	
	
	public Employee(Long id, String firstName, String middleName, String lastName, String employeeNumber, String contactNumber, String designation, boolean isActive, String fullName, boolean access) {
		this.id = id;
		this.firstName = firstName;
		this.middleName = middleName;
		this.lastName = lastName;
		this.employeeNumber = employeeNumber;
		this.contactNumber = contactNumber;
		this.designation = designation;
		this.isActive = isActive;
		if (isActive) this.status = Status.ACTIVE.toString(); else this.status = Status.INACTIVE.toString();
		this.fullName = fullName;
		this.isAccess = access;
	}
	
	public Employee(Long id, String name) {
		this.id = id;
		this.fullName = name;
	}
	
}
