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

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "supplier_subsidiary")
@ToString
@Audited
@AuditTable("supplier_subsidiary_aud")
@EqualsAndHashCode
public class SupplierSubsidiary implements Cloneable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull(message = "Supplier Id is mandatory")
	@Column(name = "supplier_id")
	private Long supplierId;

	@NotNull(message = "Subsidiary Id is mandatory")
	@Column(name = "subsidiary_id")
	private Long subsidiaryId;

	private String currency;
	
	@NotBlank(message = "Supplier currency is mandetory")
	@Column(name = "supplier_currency")
	private String supplierCurrency;
	
	@Column(name="is_preferred_currency", columnDefinition = "boolean default false")
	private boolean isPreferredCurrency;
	
	@Column(name="is_deleted", columnDefinition = "boolean default false")
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
	private String subsidiaryName;

	public SupplierSubsidiary(Long id, Long supplierId, Long subsidiaryId, String name, String currency, String supplierCurrency, boolean isPreferredCurrency) {
		this.id = id;
		this.supplierId = supplierId;
		this.subsidiaryId = subsidiaryId;
		this.subsidiaryName = name;
		this.currency = currency;
		this.supplierCurrency = supplierCurrency;
		this.isPreferredCurrency = isPreferredCurrency;
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	

	/**
	 * Compare the fields and values of 2 objects in order to find out the
	 * difference between old and new value
	 * 
	 * @param supplierSubsidiary
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public List<SupplierHistory> compareFields(SupplierSubsidiary supplierSubsidiary)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		List<SupplierHistory> supplierHistories = new ArrayList<SupplierHistory>();
		Field[] fields = this.getClass().getDeclaredFields();

		for (Field field : fields) {
			String fieldName = field.getName();

			if (!CommonUtils.getUnusedFieldsOfHistory().contains(fieldName.toLowerCase())) {
				Object oldValue = field.get(this);
				Object newValue = field.get(supplierSubsidiary);

				if (oldValue == null) {
					if (newValue != null) {
						supplierHistories.add(this.prepareSupplierHistory(supplierSubsidiary, field));
					}
				} else if (!oldValue.equals(newValue)) {
					supplierHistories.add(this.prepareSupplierHistory(supplierSubsidiary, field));
				}
			}
		}
		return supplierHistories;
	}

	private SupplierHistory prepareSupplierHistory(SupplierSubsidiary supplierSubsidiary, Field field) throws IllegalAccessException {
		SupplierHistory supplierHistory = new SupplierHistory();
		supplierHistory.setSupplierId(supplierSubsidiary.getSupplierId());
		supplierHistory.setChildId(supplierSubsidiary.getId());
		supplierHistory.setModuleName(AppConstants.SUPPLIER_SUBSIDIARY);
		supplierHistory.setChangeType(AppConstants.UI);
		supplierHistory.setLastModifiedBy(supplierSubsidiary.getLastModifiedBy());
		supplierHistory.setOperation(Operation.UPDATE.toString());
		supplierHistory.setFieldName(CommonUtils.splitCamelCaseWithCapitalize(field.getName()));
		if (field.get(this) != null) supplierHistory.setOldValue(field.get(this).toString());
		if (field.get(supplierSubsidiary) != null) supplierHistory.setNewValue(field.get(supplierSubsidiary).toString());
		return supplierHistory;
	}
}
