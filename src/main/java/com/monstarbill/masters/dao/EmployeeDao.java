package com.monstarbill.masters.dao;

import java.util.List;

import org.springframework.stereotype.Component;

import com.monstarbill.masters.models.Employee;
import com.monstarbill.masters.payload.request.PaginationRequest;

@Component("employeeDao")
public interface EmployeeDao {
	
	public List<Employee> findAll(String whereClause, PaginationRequest paginationRequest);

	public Long getCount(String whereClause);
	
}
