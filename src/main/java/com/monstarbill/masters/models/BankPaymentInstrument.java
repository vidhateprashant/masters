package com.monstarbill.masters.models;

import java.sql.Timestamp;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
@Table(	name = "bank_payment_instruments")
@ToString
public class BankPaymentInstrument {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "bank_id", nullable = false)
	private Long bankId;

	private String Type	;

	@Column(name = "document_name")
	private String documentName;

	@Column(name = "document_number_from")
	private String documentNumberFrom;

	@Column(name = "document_number_to")
	private String documentNumberTo;

	@Column(name = "effctive_from")
	private String effctiveFrom;

	@Column(name = "effctive_to")
	private String effctiveTo;
	
	@Column(name="is_active")
	private boolean isActive = true;
	
	@Column(name="is_deleted", columnDefinition = "boolean default false")
	private boolean isDeleted;
	
	@CreationTimestamp
	@Column(name="created_date", updatable = false)
	private Date createdDate;

	@Column(name="created_by")
	private String createdBy;

	@UpdateTimestamp
	@Column(name="last_modified_date")
	private Timestamp lastModifiedDate;

	@Column(name="last_modified_by")
	private String lastModifiedBy;

}