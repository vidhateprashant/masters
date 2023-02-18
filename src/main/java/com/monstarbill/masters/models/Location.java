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
@Table(schema = "setup", name = "location")
@ToString
@AuditTable("location_aud")

public class Location implements Cloneable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "Location Name is mandatory")
	@Column(name = "location_name", nullable = false, unique = true)
	private String locationName;

	@Column(name = "parent_location_id", updatable = false)
	private Long parentLocationId;

	@NotNull(message = "Subsidiary Id is mandatory")
	@Column(name = "subsidiary_id")
	private Long subsidiaryId;

	@NotBlank(message = "Location Type is mandetory")
	@Column(name = "location_type", nullable = false)
	private String locationType;

	@Column(name = "is_deleted", columnDefinition = "boolean default false")
	private boolean isDeleted;

	@NotNull(message = "Effective from is mandatory")
	@Column(name = "effective_from", nullable = false)
	private Date effectiveFrom;

	@Column(name = "effective_to")
	private Date effectiveTo;
	
	@Column(name = "is_parent_location", columnDefinition = "boolean default false")
	private boolean isParentLocation;

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

	@Transient
	private String parentLocationName;

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Transient
	private LocationAddress locationAddress;

	public Location(Long id, String locationName, String locationType, Date effectiveFrom, String subsidiaryName,
			String parentLocationName, Long parentLocationId, Long subsidiaryId) {
		this.id = id;
		this.locationName = locationName;
		this.parentLocationId = parentLocationId;
		this.subsidiaryId = subsidiaryId;
		this.locationType = locationType;
		this.effectiveFrom = effectiveFrom;
		this.subsidiaryName = subsidiaryName;
		this.parentLocationName = parentLocationName;
	}

	/**
	 * Compare the fields and values of 2 objects in order to find out the
	 * difference between old and new value
	 * 
	 * @param location
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public List<LocationHistory> compareFields(Location location)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		List<LocationHistory> locationHistories = new ArrayList<LocationHistory>();
		Field[] fields = this.getClass().getDeclaredFields();

		for (Field field : fields) {
			String fieldName = field.getName();

			if (!CommonUtils.getUnusedFieldsOfHistory().contains(fieldName.toLowerCase())) {
				Object oldValue = field.get(this);
				Object newValue = field.get(location);

				if (oldValue == null) {
					if (newValue != null) {
						locationHistories.add(this.prepareLocationHistory(location, field));
					}
				} else if (!oldValue.equals(newValue)) {
					locationHistories.add(this.prepareLocationHistory(location, field));
				}
			}
		}
		return locationHistories;
	}

	private LocationHistory prepareLocationHistory(Location location, Field field) throws IllegalAccessException {
		LocationHistory locationHistory = new LocationHistory();
		locationHistory.setLocationId(location.getId());
		locationHistory.setModuleName(AppConstants.LOCATION);
		locationHistory.setChangeType(AppConstants.UI);
		locationHistory.setOperation(Operation.UPDATE.toString());
		locationHistory.setFieldName(CommonUtils.splitCamelCaseWithCapitalize(field.getName()));
		if (field.get(this) != null)
			locationHistory.setOldValue(field.get(this).toString());
		if (field.get(location) != null)
			locationHistory.setNewValue(field.get(location).toString());
		locationHistory.setLastModifiedBy(location.getLastModifiedBy());
		return locationHistory;
	}

	public Location(String locationName, Long id) {
		this.locationName = locationName;
		this.id = id;
	}
}
