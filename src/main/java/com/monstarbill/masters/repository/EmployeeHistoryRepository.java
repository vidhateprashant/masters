package com.monstarbill.masters.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.monstarbill.masters.models.EmployeeHistory;

@Repository
public interface EmployeeHistoryRepository extends JpaRepository<EmployeeHistory, String> {

	public List<EmployeeHistory> findByEmployeeNumberOrderById(String employeeNumber, Pageable pageable);
}
