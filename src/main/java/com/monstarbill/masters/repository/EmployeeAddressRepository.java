package com.monstarbill.masters.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.monstarbill.masters.models.EmployeeAddress;

public interface EmployeeAddressRepository extends JpaRepository<EmployeeAddress, String>{

	public List<EmployeeAddress> findByEmployeeId(Long employeeId);
	
	public Optional<EmployeeAddress> findById(Long id);

	public List<EmployeeAddress> findByEmployeeIdAndIsDeleted(Long employeeId, boolean isDeleted);

	public Optional<EmployeeAddress> findByIdAndIsDeleted(Long id, boolean isDeleted);
}
