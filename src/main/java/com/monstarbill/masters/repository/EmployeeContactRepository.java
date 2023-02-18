package com.monstarbill.masters.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.monstarbill.masters.models.EmployeeContact;

@Repository
public interface EmployeeContactRepository extends JpaRepository<EmployeeContact, String> {
	
	public Optional<EmployeeContact> findByEmployeeIdAndIsDeleted(Long employeeId, boolean isDeleted);
	
	public Optional<EmployeeContact> findByIdAndIsDeleted(Long id, boolean isDeleted);

	@Query(" SELECT email from EmployeeContact WHERE employeeId = :employeeId ")
	public String findEmailByEmployeeId(Long employeeId);

}
