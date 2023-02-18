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
@Table(name = "supplier")
@ToString
@Audited
@AuditTable("supplier_aud")
public class Supplier implements Cloneable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name = "external_id")
	private String externalId;

	@NotBlank(message = "Vendor Name is mandatory")
	@Column(nullable = false, unique = true)
	private String name;

	@Column(name = "legal_name")
	private String legalName;
	
	@Column(name = "payment_term")
	private String paymentTerm;

	@Column(name = "vendor_number")
	private String vendorNumber;

	@NotBlank(message = "Vendor Type is mandatory")
	@Column(name = "vendor_type")
	private String vendorType;

//	@NotBlank(message = "Unique Identification Number is mandatory")
//	@Column(nullable = false)
	private String uin;

	@Column(name = "approval_status")
	private String approvalStatus;
	
	@Column(name = "reject_comments")
	private String rejectComments;

	@Column(name = "nature_of_supply")
	private String natureOfSupply;

	@Column(name = "unique_number")
	private String uniqueNumber;

	@Column(name = "invoice_mail")
	private String invoiceMail;

	@Column(name = "tds_witholding")
	private String tdsWitholding;
	
	// -----------------------------APPROVAL START--------------------------------------------------
	@Column(name = "approved_by")
	private String approvedBy;
	
	@Column(name = "next_approver")
	private String nextApprover;
	
	@Column(name = "next_approver_role")
	private String nextApproverRole;

	// stores the next approver level i.e. L1,L2,L3 etc.
	@Column(name = "next_approver_level")
	private String nextApproverLevel;
	
	// store's the id of approver preference
	@Column(name = "approver_preference_id")
	private Long approverPreferenceId;
	
	// stores the approver sequence id (useful internally in order to change the approver)
	@Column(name = "approver_sequence_id")
	private Long approverSequenceId;
	
	// stores the max level to approve, after that change status to approve
	@Column(name = "approver_max_level")
	private String approverMaxLevel;
	
	@Column(name = "note_to_approver")
	private String noteToApprover;
	
	@Column(name = "ns_message")
	private String nsMessage;

	@Column(name = "ns_status")
	private String nsStatus;

	@Column(name = "integrated_id")
	private String integratedId;
	
	@Transient
	private boolean isApprovalRoutingActive;
	// ------------------------------------------------------------------

	@Column(name = "is_active", columnDefinition = "boolean default true")
	private boolean isActive;
	
	@Column(name="active_date")
	private Date activeDate;

	@Column(name = "is_deleted", columnDefinition = "boolean default false")
	private boolean isDeleted;

	@CreationTimestamp
	@Column(name = "created_date", updatable = false)
	private Date createdDate;

	@Column(name = "created_by", updatable = false)
	private String createdBy;

	@UpdateTimestamp
	@Column(name = "last_modified_date")
	private Timestamp lastModifiedDate;

	@Column(name = "last_modified_by")
	private String lastModifiedBy;

	@Transient
	private List<SupplierContact> supplierContacts;

	@Transient
	private List<SupplierSubsidiary> supplierSubsidiary;

	@Transient
	private SupplierAccounting supplierAccounting;

	@Transient
	private List<SupplierAddress> supplierAddresses;

	@Transient
	private SupplierAccess supplierAccess;

	@Transient
	private boolean access;
	
	@Transient
	private String subsidiaryName;
	
	@Transient
	private Long subsidiaryId;
	
	@Transient
	private String contactName;
	
	@Transient
	private String contactNumber;
	
	@Transient
	private String currency;
	
	@Transient
	private boolean hasError;
	
	@Transient
	private String approverByName;
	
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
	public List<SupplierHistory> compareFields(Supplier supplier)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		List<SupplierHistory> supplierHistories = new ArrayList<SupplierHistory>();
		Field[] fields = this.getClass().getDeclaredFields();

		for (Field field : fields) {
			String fieldName = field.getName();

			if (!CommonUtils.getUnusedFieldsOfHistory().contains(fieldName.toLowerCase())) {
				Object oldValue = field.get(this);
				Object newValue = field.get(supplier);

				if (oldValue == null) {
					if (newValue != null) {
						supplierHistories.add(this.prepareSupplierHistory(supplier, field));
					}
				} else if (!oldValue.equals(newValue)) {
					supplierHistories.add(this.prepareSupplierHistory(supplier, field));
				}
			}
		}
		return supplierHistories;
	}

	private SupplierHistory prepareSupplierHistory(Supplier supplier, Field field) throws IllegalAccessException {
		SupplierHistory supplierHistory = new SupplierHistory();
		supplierHistory.setSupplierId(supplier.getId());
		supplierHistory.setModuleName(AppConstants.SUPPLIER);
		supplierHistory.setChangeType(AppConstants.UI);
		supplierHistory.setLastModifiedBy(supplier.getLastModifiedBy());
		supplierHistory.setOperation(Operation.UPDATE.toString());
		supplierHistory.setFieldName(CommonUtils.splitCamelCaseWithCapitalize(field.getName()));
		if (field.get(this) != null) supplierHistory.setOldValue(field.get(this).toString());
		if (field.get(supplier) != null) supplierHistory.setNewValue(field.get(supplier).toString());
		return supplierHistory;
	}
	
	/**
	 * Used to get list of suppliers(id, Name)
	 * @param id
	 * @param name
	 */
	public Supplier(Long id, String name) {
		this.id = id;
		this.name = name;
	}

	public Supplier(Long id, String name, String vendorNumber, String vendorType, boolean isActive, String subsidiaryName, String contactName, String contactNumber, String approvalStatus, boolean access) {
		this.id = id;
		this.name = name;
		this.vendorNumber = vendorNumber;
		this.vendorType = vendorType;
		this.isActive = isActive;
		this.subsidiaryName = subsidiaryName;
		this.contactName = contactName;
		this.contactNumber = contactNumber;
		this.approvalStatus = approvalStatus;
		this.access = access;
	}

	public Supplier(Long id, String name, String currency) {
		this.id = id;
		this.name = name;
		this.currency = currency;
	}

	public Supplier(Long id, String name, String approvalStatus, String vendorNumber, String vendorType, String uin, String rejectComments,
			boolean access, Long subsidiaryId, String subsidiaryName, String approvedBy, String nextApprover, String nextApproverRole, String approverByName) {
		this.id = id;
		this.name = name;
		this.approvalStatus = approvalStatus;
		this.vendorNumber = vendorNumber;
		this.vendorType = vendorType;
		this.uin = uin;
		this.rejectComments = rejectComments;
		this.access = access;
		this.subsidiaryId = subsidiaryId;
		this.subsidiaryName = subsidiaryName;
		this.approvedBy = approvedBy;
		this.nextApprover = nextApprover;
		this.nextApproverRole = nextApproverRole;
		this.approverByName = approverByName;
	}
	
	
}
