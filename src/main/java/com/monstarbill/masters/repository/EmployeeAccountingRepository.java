package com.monstarbill.masters.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.monstarbill.masters.models.EmployeeAccounting;

@Repository
public interface EmployeeAccountingRepository extends JpaRepository<EmployeeAccounting, String> {
	
	public Optional<EmployeeAccounting> findByEmployeeIdAndIsDeleted(Long employeeId, boolean isDeleted);
	
	public Optional<EmployeeAccounting> findByIdAndIsDeleted(Long id, boolean isDeleted);
}
