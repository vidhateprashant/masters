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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(	name = "project")
@ToString
@Audited
@AuditTable("project_aud")
public class Project implements Cloneable{
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull(message = "Subsidiary is mandatory")
	@Column(name="subsidiary_id",nullable = false, updatable = false)
	private Long subsidiaryId;

	@NotBlank(message = "Project Name is mandatory")
	@Column(name="name", nullable = false)
	private String name;

	@Column(name="project_id")
	private String projectId;

	@Column(updatable = false)
	private String customer;
	
	@NotBlank(message = "Project Status is mandatory")
	@Column(name="status", nullable = false)
	private String status;

	@NotNull(message = "Scheduling  Start Date is mandatory")
	@Column(name="scheduling_start_date", nullable = false, updatable = false)
	private Date schedulingStartDate;
	
	@Column(name="scheduling_end_date")
	private Date schedulingEndDate;

	private String integrated_id;
	
	@Column(name="is_deleted" , columnDefinition = "boolean default false")
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
	
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public Project(Long id,  String name, String projectId, Date schedulingStartDate, Date schedulingEndDate, String subsidiaryName) {
		
		this.id = id;
		this.name = name;
		this.projectId = projectId;
		this.schedulingStartDate = schedulingStartDate;
		this.schedulingEndDate = schedulingEndDate;
		this.subsidiaryName = subsidiaryName;
	}
	
	/**
	 * Compare the fields and values of 2 objects in order to find out the
	 * difference between old and new value
	 * 
	 * @param project
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public List<ProjectHistory> compareFields(Project project)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		List<ProjectHistory> projectHistories = new ArrayList<ProjectHistory>();
		Field[] fields = this.getClass().getDeclaredFields();

		for (Field field : fields) {
			String fieldName = field.getName();

			if (!CommonUtils.getUnusedFieldsOfHistory().contains(fieldName.toLowerCase())) {
				Object oldValue = field.get(this);
				Object newValue = field.get(project);

				if (oldValue == null) {
					if (newValue != null) {
						projectHistories.add(this.prepareProjectHistory(project, field));
					}
				} else if (!oldValue.equals(newValue)) {
					projectHistories.add(this.prepareProjectHistory(project, field));
				}
			}
		}
		return projectHistories;
	}

	private ProjectHistory prepareProjectHistory(Project project, Field field) throws IllegalAccessException {
		ProjectHistory projectHistory = new ProjectHistory();
		projectHistory.setProjectId(project.getId());
		projectHistory.setModuleName(AppConstants.PROJECT);
		projectHistory.setChangeType(AppConstants.UI);
		projectHistory.setOperation(Operation.UPDATE.toString());
		projectHistory.setFieldName(CommonUtils.splitCamelCaseWithCapitalize(field.getName()));
		if (field.get(this) != null) projectHistory.setOldValue(field.get(this).toString());
		if (field.get(project) != null) projectHistory.setNewValue(field.get(project).toString());
		projectHistory.setLastModifiedBy(project.getLastModifiedBy());
		return projectHistory;
	}

}
