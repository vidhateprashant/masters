package com.monstarbill.masters.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

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
@Table(	name = "default_role_permission")
@ToString
public class DefaultRolePermissions {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name = "module_name")
	private String moduleName;
	
	@Column(name = "access_point")
	private String accessPoint;
	
	@Column(name="is_view", columnDefinition = "boolean default true")
	private boolean isView;
	
	@Column(name="is_create", columnDefinition = "boolean default true")
	private boolean isCreate;
	
	@Column(name="is_edit", columnDefinition = "boolean default true")
	private boolean isEdit;

	@Column(name="is_deleted", columnDefinition = "boolean default false")
	private boolean isDeleted;

	@Column(name="is_supplier", columnDefinition = "boolean default false")
	private boolean isSupplierAccess;
	
	@Column(name="is_admin", columnDefinition = "boolean default true")
	private boolean isAdminAccess;
	
	@Column(name="is_approver", columnDefinition = "boolean default false")
	private boolean isApproverAccess;
}