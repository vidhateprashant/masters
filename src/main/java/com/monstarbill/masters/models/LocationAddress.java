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
@Table(	name = "location_address")
@ToString
@AuditTable("location_address_aud")
public class LocationAddress implements Cloneable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "location_id", nullable = false)
	private Long locationId;

	@NotBlank(message = "country is mandatory")
	private String country;

	private String phone;

	@NotBlank(message = "Address Line 1 is mandatory")
	private String address1;

	private String address2;

	private String city;

	private String state;

	private String pin;
	
	@Column(name="is_deleted", columnDefinition = "boolean default false")
	private boolean isDeleted;
	
	@CreationTimestamp
	@Column(name="created_date", updatable = false)
	private Date createdDate;

	@Column(name="created_by",updatable = false)
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
	 * @param locationAddress
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public List<LocationHistory> compareFields(LocationAddress locationAddress)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		List<LocationHistory> locationAddressHistories = new ArrayList<LocationHistory>();
		Field[] fields = this.getClass().getDeclaredFields();

		for (Field field : fields) {
			String fieldName = field.getName();

			if (!CommonUtils.getUnusedFieldsOfHistory().contains(fieldName.toLowerCase())) {
				Object oldValue = field.get(this);
				Object newValue = field.get(locationAddress);

				if (oldValue == null) {
					if (newValue != null) {
						locationAddressHistories.add(this.prepareLocationAddressHistory(locationAddress, field));
					}
				} else if (!oldValue.equals(newValue)) {
					locationAddressHistories.add(this.prepareLocationAddressHistory(locationAddress, field));
				}
			}
		}
		return locationAddressHistories;
	}

	private LocationHistory prepareLocationAddressHistory(LocationAddress locationAddress, Field field) throws IllegalAccessException {
		LocationHistory locationHistory = new LocationHistory();
		locationHistory.setLocationId(locationAddress.getLocationId());
		locationHistory.setChildId(locationAddress.getId());
		locationHistory.setModuleName(AppConstants.LOCATION_ADDRESS);
		locationHistory.setChangeType(AppConstants.UI);
		locationHistory.setOperation(Operation.UPDATE.toString());
		locationHistory.setFieldName(CommonUtils.splitCamelCaseWithCapitalize(field.getName()));
		if (field.get(this) != null) locationHistory.setOldValue(field.get(this).toString());
		if (field.get(locationAddress) != null) locationHistory.setNewValue(field.get(locationAddress).toString());
		locationHistory.setLastModifiedBy(locationAddress.getLastModifiedBy());
		return locationHistory;
	}
}

