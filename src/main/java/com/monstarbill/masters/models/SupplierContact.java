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
@Table(schema = "setup", name = "supplier_contact")
@ToString
@Audited
@AuditTable("supplier_contact_aud")
@EqualsAndHashCode
public class SupplierContact implements Cloneable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name="supplier_id")
	private Long supplierId;

	@NotBlank(message = " Name is mandatory")
	private String name;
	
	@NotBlank(message = "contact number is mandatory")
	@Column(name="contact_number")
	private String contactNumber;

	@Column(name="alt_contact_number")
	private String altContactNumber;

	private String email;

	private String web;

	private String fax;
	
	@Column(name="is_primary_contact", columnDefinition = "boolean default false")
	private boolean isPrimaryContact;
	
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
	 * Compare the fields and values of 2 objects in order 
	 * to find out the difference between old and new value
	 * @param supplierContact
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public List<SupplierHistory> compareFields(SupplierContact supplierContact) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		
	    List<SupplierHistory> supplierHistories = new ArrayList<SupplierHistory>();      
	    Field[] fields = this.getClass().getDeclaredFields();

	    for(Field field : fields){
	    	String fieldName = field.getName();
	    	
	    	if (!CommonUtils.getUnusedFieldsOfHistory().contains(fieldName.toLowerCase())) {
		        Object oldValue = field.get(this);
		        Object newValue = field.get(supplierContact);
		        
		        if (oldValue == null) {
		        	if (newValue != null) {
		        		supplierHistories.add(this.prepareSupplierHistory(supplierContact, field));
		        	}
		        } else if (!oldValue.equals(newValue)){
		        	supplierHistories.add(this.prepareSupplierHistory(supplierContact, field));
		        }
	    	}
	    }
	    return supplierHistories;
	}

	private SupplierHistory prepareSupplierHistory(SupplierContact supplierContact, Field field) throws IllegalAccessException {
		SupplierHistory supplierHistory = new SupplierHistory();
		supplierHistory.setSupplierId(supplierContact.getSupplierId());
		supplierHistory.setChildId(supplierContact.getId());
		supplierHistory.setModuleName(AppConstants.SUPPLIER_CONTACT);
		supplierHistory.setChangeType(AppConstants.UI);
		supplierHistory.setOperation(Operation.UPDATE.toString());
		supplierHistory.setLastModifiedBy(supplierContact.getLastModifiedBy());
		supplierHistory.setFieldName(CommonUtils.splitCamelCaseWithCapitalize(field.getName()));
		if (field.get(this) != null) supplierHistory.setOldValue(field.get(this).toString());
		if (field.get(supplierContact) != null) supplierHistory.setNewValue(field.get(supplierContact).toString());
		return supplierHistory;
	}
}
