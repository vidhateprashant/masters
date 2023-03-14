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
import javax.persistence.UniqueConstraint;
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
@Table(	name = "item",uniqueConstraints = { @UniqueConstraint(columnNames = 
{ "subsidiary_id", "name" }) })
@ToString
@Audited
@AuditTable("item_aud")
public class Item implements Cloneable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "external_id")
	private String externalId;
	
	@NotNull(message = "Subsidiary is mandatory")
	@Column(name = "subsidiary_id", nullable = false, updatable = false)
	private Long subsidiaryId;
	
	@NotBlank(message = "Item category is mandatory")
	@Column(updatable = false)
	private String category;

	@NotBlank(message = "Item Name is mandatory")
	private String name;

	private String description;

	@NotBlank(message = "UOM is mandatory")
	private String uom;

	private String costingMethod;

	@Column(name="is_purchasable", columnDefinition = "boolean default false")
	private boolean isPurchasable;
	
	@Column(name="is_salable", columnDefinition = "boolean default false")
	private boolean isSalable;

	@Column(name = "hsn_sac_code")
	private String hsnSacCode;
	
	private String integratedId;

	@Column(name = "nature_of_item")
	private String natureOfItem;
	
	// ------- ACCOUNTING -------------------------
	@Column(name = "expense_account_id")
	private Long expenseAccountId;
	
	@Column(name = "cogs_account")
	private String cogsAccount;
	
	@Column(name = "income_account")
	private String incomeAccount;
	
	@Column(name = "asset_account_id")
	private Long assetAccountId;
	// ------- ACCOUNTING -------------------------
	
	@Column(name="is_active", columnDefinition = "boolean default true")
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
	private String status;
	
	@Transient
	private String subsidiaryName;
	
	@Transient
	private Long accountId;
	
	@Transient
	private String assetAccountName;
	
	@Transient
	private String expenseAccountName;

	public Item(Long id, String name, String description, String category, String uom, boolean isActive, String subsidiaryName) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.category = category;
		this.uom = uom;
		this.subsidiaryName = subsidiaryName;
		if (isActive) {
			this.status = Status.ACTIVE.toString();
		} else {
			this.status = Status.INACTIVE.toString();
		}
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	/**
	 * Compare the fields and values of 2 objects in order to find out the
	 * difference between old and new value
	 * 
	 * @param item
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public List<ItemHistory> compareFields(Item item)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		List<ItemHistory> itemHistories = new ArrayList<ItemHistory>();
		Field[] fields = this.getClass().getDeclaredFields();

		for (Field field : fields) {
			String fieldName = field.getName();

			if (!CommonUtils.getUnusedFieldsOfHistory().contains(fieldName.toLowerCase())) {
				Object oldValue = field.get(this);
				Object newValue = field.get(item);

				if (oldValue == null) {
					if (newValue != null) {
						itemHistories.add(this.prepareItemHistory(item, field));
					}
				} else if (!oldValue.equals(newValue)) {
					itemHistories.add(this.prepareItemHistory(item, field));
				}
			}
		}
		return itemHistories;
	}

	private ItemHistory prepareItemHistory(Item item, Field field) throws IllegalAccessException {
		ItemHistory itemHistory = new ItemHistory();
		itemHistory.setItemId(item.getId());
		itemHistory.setModuleName(AppConstants.ITEM);
		itemHistory.setChangeType(AppConstants.UI);
		itemHistory.setOperation(Operation.UPDATE.toString());
		itemHistory.setFieldName(CommonUtils.splitCamelCaseWithCapitalize(field.getName()));
		if (field.get(this) != null) itemHistory.setOldValue(field.get(this).toString());
		if (field.get(item) != null) itemHistory.setNewValue(field.get(item).toString());
		itemHistory.setLastModifiedBy(item.getLastModifiedBy());
		return itemHistory;
	}
	
	/**
	 * get Items based on subsidiary
	 * @param id
	 * @param subsidiaryId
	 * @param type
	 * @param name
	 * @param description
	 * @param uom
	 * @param integratedId
	 * @param accountId
	 */
	public Item(Long id, Long subsidiaryId, String category, String name, String description, String uom, String integratedId, Long accountId) {
		this.id = id;
		this.subsidiaryId = subsidiaryId;
		this.category = category;
		this.name = name;
		this.description = description;
		this.uom = uom;
		this.integratedId = integratedId;
		this.accountId = accountId;
	}
}
