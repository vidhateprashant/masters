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
@Table(	name = "supplier_accounting")
@ToString
public class SupplierAccounting implements Cloneable {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name="supplier_id")
	private Long supplierId;
	
	//@NotNull(message = "Liability account id is mandetory")
	@Column(name="liability_account_id")
	private Long liabilityAccountId;
	
	//@NotNull(message = "Prepayment account id is mandetory")
	@Column(name="prepayment_account_id")
	private Long prepaymentAccountId;
	
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
	 * @param supplierAccounting
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public List<SupplierHistory> compareFields(SupplierAccounting supplierAccounting) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		List<SupplierHistory> supplierHistories = new ArrayList<SupplierHistory>();
		Field[] fields = this.getClass().getDeclaredFields();

		for (Field field : fields) {
			String fieldName = field.getName();

			if (!CommonUtils.getUnusedFieldsOfHistory().contains(fieldName.toLowerCase())) {
				Object oldValue = field.get(this);
				Object newValue = field.get(supplierAccounting);

				if (oldValue == null) {
					if (newValue != null) {
						supplierHistories.add(this.prepareSupplierHistory(supplierAccounting, field));
					}
				} else if (!oldValue.equals(newValue)) {
					supplierHistories.add(this.prepareSupplierHistory(supplierAccounting, field));
				}
			}
		}
		return supplierHistories;
	}

	private SupplierHistory prepareSupplierHistory(SupplierAccounting supplierAccounting, Field field) throws IllegalAccessException {
		SupplierHistory supplierHistory = new SupplierHistory();
		supplierHistory.setSupplierId(supplierAccounting.getSupplierId());
		supplierHistory.setChildId(supplierAccounting.getId());
		supplierHistory.setModuleName(AppConstants.SUPPLIER_ACCOUNTING);
		supplierHistory.setChangeType(AppConstants.UI);
		supplierHistory.setOperation(Operation.UPDATE.toString());
		supplierHistory.setLastModifiedBy(supplierAccounting.getLastModifiedBy());
		supplierHistory.setFieldName(CommonUtils.splitCamelCaseWithCapitalize(field.getName()));
		if (field.get(this) != null) supplierHistory.setOldValue(field.get(this).toString());
		if (field.get(supplierAccounting) != null) supplierHistory.setNewValue(field.get(supplierAccounting).toString());
		return supplierHistory;
	}
}
