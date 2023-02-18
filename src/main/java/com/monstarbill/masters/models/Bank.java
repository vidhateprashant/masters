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
import javax.validation.constraints.NotNull;

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
@Table(	name = "bank")
@ToString
@Audited
@AuditTable("bank_aud")
public class Bank implements Cloneable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull(message = "Subsidiary is mandatory")
	@Column(name = "subsidiary_id", nullable = false, updatable = false)
	private Long subsidiaryId;

	@NotBlank(message = "Bank Name is mandatory")
	@Column(updatable = false)
	private String name;

	@NotBlank(message = "Branch of Bank is mandatory")
	@Column(unique = true)
	private String branch;

	@Column(columnDefinition = "text")
	private String address;

	@NotBlank(message = "Account Number is mandatory")
	@Column(name = "account_number", unique = true)
	private String accountNumber;

	@NotBlank(message = "Acount Type is mandatory")
	@Column(name = "account_type")
	private String accountType;

	@NotBlank(message = "Currency is mandatory")
	@Column(updatable = false)
	private String currency;

	@Column(name = "branch_code")
	private String branchCode;

	@Column(name = "ifsc_code")
	private String ifscCode;
	
	private String iban;
	
	@Column(name = "swift_code")
	private String swiftCode;
	
	@Column(name = "sort_code")
	private String sortCode;

	@Column(name = "micr_code")
	private String micrCode;

	@NotBlank(message = "GL Bank is mandatory")
	@Column(name = "gl_bank")
	private String glBank;

	@Column(name = "gl_exchange")
	private String glExchange;

	@NotNull(message ="Effective from is mandatory")
	@Column(name = "effective_from")
	private Date effectiveFrom;

	@Column(name = "effective_to")
	private Date effectiveTo;

	@Transient
	private String status;
	
	@Column(name="is_active")
	private boolean isActive = true;
	
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
	private String subsidiaryName;
	
	@Transient
	private List<BankPaymentInstrument> bankPaymentInstruments;

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	public Bank(Long id, String name, String accountNumber, String accountType, String branch, String currency, boolean isActive, String subsidiaryName) {
		this.id = id;
		this.name = name;
		this.accountNumber = accountNumber;
		this.accountType = accountType;
		this.branch = branch;
		this.currency = currency;
		this.isActive = isActive;
		if (isActive) {
			this.status = Status.ACTIVE.toString();			
		} else {
			this.status = Status.INACTIVE.toString();
		}
		this.subsidiaryName = subsidiaryName;
	}

	/**
	 * Compare the fields and values of 2 objects in order to find out the
	 * difference between old and new value
	 * 
	 * @param bank
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public List<BankHistory> compareFields(Bank bank)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		List<BankHistory> bankHistories = new ArrayList<BankHistory>();
		Field[] fields = this.getClass().getDeclaredFields();

		for (Field field : fields) {
			String fieldName = field.getName();

			if (!CommonUtils.getUnusedFieldsOfHistory().contains(fieldName.toLowerCase())) {
				Object oldValue = field.get(this);
				Object newValue = field.get(bank);

				if (oldValue == null) {
					if (newValue != null) {
						bankHistories.add(this.prepareBankHistory(bank, field));
					}
				} else if (!oldValue.equals(newValue)) {
					bankHistories.add(this.prepareBankHistory(bank, field));
				}
			}
		}
		return bankHistories;
	}

	private BankHistory prepareBankHistory(Bank bank, Field field) throws IllegalAccessException {
		BankHistory bankHistory = new BankHistory();
		bankHistory.setBankId(bank.getId());
		bankHistory.setModuleName(AppConstants.BANK);
		bankHistory.setChangeType(AppConstants.UI);
		bankHistory.setOperation(Operation.UPDATE.toString());
		bankHistory.setFieldName(CommonUtils.splitCamelCaseWithCapitalize(field.getName()));
		if (field.get(this) != null) bankHistory.setOldValue(field.get(this).toString());
		if (field.get(bank) != null) bankHistory.setNewValue(field.get(bank).toString());
		bankHistory.setLastModifiedBy(bank.getLastModifiedBy());
		return bankHistory;
	}
}
