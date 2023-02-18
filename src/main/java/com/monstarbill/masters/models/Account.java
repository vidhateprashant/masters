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
@Table(schema = "setup", name = "account")
@ToString
@Audited
@AuditTable("account_aud")
public class Account implements Cloneable{
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "Account Code is mandatory")
	@Column(name="code",nullable = false, updatable = false, unique = true)
	private String code;

	@NotBlank(message = "Account Description  is mandatory")
	@Column(name="description", nullable = false)
	private String description;

	@Column(name="parent")
	private Long parent;

	@NotBlank(message = "Account Type is mandatory")
	@Column(name="type")
	private String type;
	
	private String currency;

	@Column(name="is_inactive", columnDefinition = "boolean default false")
	private boolean isInactive;
	
	@Column(name="inactive_date")
	private Date inactiveDate;
	
	@Column(name="account_summary", columnDefinition = "boolean default false")
	private boolean isAccountSummary;
	
	@Column(name="tds_tax_code")
	private String tdsTaxCode;
	
	@Column(name="tax_code")
	private String taxCode;
	
//	@Column(name="restrict_cost_centre")
//	private String restrictCostCentre;
//	
//	@Column(name="subsidiary_id")
//	private Long subsidiaryId;
//	
//	@Column(name="restrict_department")
//	private String restrictDepartment;

	@Column(name="is_deleted", columnDefinition = "boolean default false")
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
	
	// subsidiary mappings
	@Transient
	private List<AccountSubsidiary> accountSubsidiaries;
	
	// restricted departments mappings
	@Transient
	private List<AccountDepartment> accountDepartments;
	
	// cost center mappings
	@Transient
	private List<AccountLocation> accountLocations;
	
	@Transient
	private String status;
	
	@Transient
	private Long subsidiaryId;
	
	@Transient
	private String subsidiaryName;
	
	@Transient
	private String codeWithType;

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public Account(Long id, String code, String description, String type, boolean isInactive, String subsidiaryName, String codeWithType) {
		this.id = id;
		this.code = code;
		this.description = description;
		this.type = type;
		this.isInactive = isInactive;
		this.status = Status.ACTIVE.toString();
		if (isInactive) {
			status = Status.INACTIVE.toString();	
		}
		this.subsidiaryName = subsidiaryName;
		this.codeWithType = codeWithType;
	}

	/**
	 * Compare the fields and values of 2 objects in order to find out the
	 * difference between old and new value
	 * 
	 * @param account
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public List<AccountHistory> compareFields(Account account)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		List<AccountHistory> accountHistories = new ArrayList<AccountHistory>();
		Field[] fields = this.getClass().getDeclaredFields();

		for (Field field : fields) {
			String fieldName = field.getName();

			if (!CommonUtils.getUnusedFieldsOfHistory().contains(fieldName.toLowerCase())) {
				Object oldValue = field.get(this);
				Object newValue = field.get(account);

				if (oldValue == null) {
					if (newValue != null) {
						accountHistories.add(this.prepareAccountHistory(account, field));
					}
				} else if (!oldValue.equals(newValue)) {
					accountHistories.add(this.prepareAccountHistory(account, field));
				}
			}
		}
		return accountHistories;
	}

	private AccountHistory prepareAccountHistory(Account account, Field field) throws IllegalAccessException {
		AccountHistory accountHistory = new AccountHistory();
		accountHistory.setAccountId(account.getId());
		accountHistory.setModuleName(AppConstants.ACCOUNT);
		accountHistory.setChangeType(AppConstants.UI);
		accountHistory.setOperation(Operation.UPDATE.toString());
		accountHistory.setFieldName(CommonUtils.splitCamelCaseWithCapitalize(field.getName()));
		if (field.get(this) != null) {
			accountHistory.setOldValue(field.get(this).toString());
		}
		if (field.get(account) != null) {
			accountHistory.setNewValue(field.get(account).toString());
		}
		accountHistory.setLastModifiedBy(account.getLastModifiedBy());
		return accountHistory;
	}
	
	public Account(Long id, String code, String description, String type, String currency, boolean isInactive, boolean isAccountSummary) {
		this.id = id;
		this.code = code + "-" + description;
		this.description = description;
		this.type = type;
		this.currency = currency;
		this.isInactive = isInactive;
		this.isAccountSummary = isAccountSummary;
		
	}
	public Account(Long id, String code, String description) {
		this.id = id;
		this.code = code + "-" + description;
		this.description = description;
	}

//	public Account(Long id, String code) {
//		this.id = id;
//		this.code = code;
//	}
	
	public Account(Long id, String code, String description, String type) {
		this.id = id;
		this.code = code + "-" + description;
		this.description = description;
		this.type = type;
	}
}
