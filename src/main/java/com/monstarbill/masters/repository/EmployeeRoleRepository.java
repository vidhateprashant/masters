package com.monstarbill.masters.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.monstarbill.masters.models.EmployeeRole;


@Repository
public interface EmployeeRoleRepository extends JpaRepository<EmployeeRole, String> {

	public List<EmployeeRole> findByEmployeeIdAndIsDeleted(Long employeeId, boolean isDeleted);

	public Optional<EmployeeRole> findByIdAndIsDeleted(Long id, boolean isDeleted);

}
