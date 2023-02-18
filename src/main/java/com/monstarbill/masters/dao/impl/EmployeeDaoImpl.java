package com.monstarbill.masters.dao.impl;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.monstarbill.masters.commons.CommonUtils;
import com.monstarbill.masters.commons.CustomException;
import com.monstarbill.masters.dao.EmployeeDao;
import com.monstarbill.masters.models.Employee;
import com.monstarbill.masters.payload.request.PaginationRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("employeeDaoImpl")
public class EmployeeDaoImpl implements EmployeeDao {

	@PersistenceContext
	private EntityManager entityManager;
	
	public static final String GET_ALL_EMPLOYEES = " SELECT new com.monstarbill.masters.models.Employee(e.id, e.firstName, e.middleName, e.lastName, e.employeeNumber, ec.mobile, e.designation, e.isActive, e.fullName, ea.access) from Employee e "
			+ " INNER JOIN EmployeeContact ec ON ec.employeeId = e.id "
			+ " INNER JOIN EmployeeAccess ea ON ea.employeeId = e.id "
			+ " WHERE 1=1 ";
	
	public static final String GET_EMPLOYEE_COUNT = " SELECT count(1) from Employee e "
			+ " INNER JOIN EmployeeContact ec ON ec.employeeId = e.id "
			+ " INNER JOIN EmployeeAccess ea ON ea.employeeId = e.id "
			+ " WHERE 1=1 ";

	@Override
	public List<Employee> findAll(String whereClause, PaginationRequest paginationRequest) {
		List<Employee> employees = new ArrayList<Employee>();
		
		StringBuilder finalSql = new StringBuilder(GET_ALL_EMPLOYEES);
		// where clause
		if (StringUtils.isNotEmpty(whereClause)) finalSql.append(whereClause.toString());
		// order by clause
		finalSql.append(CommonUtils.prepareOrderByClause(paginationRequest.getSortColumn(), paginationRequest.getSortOrder()));
		log.info("Final SQL to get all employees w/w/o filter :: " + finalSql.toString());
		
		try {
			TypedQuery<Employee> sql = this.entityManager.createQuery(finalSql.toString(), Employee.class);
			// pagination
			sql.setFirstResult(paginationRequest.getPageNumber() * paginationRequest.getPageSize());
			sql.setMaxResults(paginationRequest.getPageSize());
			employees = sql.getResultList();
		} catch (Exception ex) {
			log.error("Exception occured at the time of fetching the list of employees :: " + ex.toString());
			
			String errorExceptionMessage = ex.getLocalizedMessage();
			if (errorExceptionMessage == null) errorExceptionMessage = ex.toString();
			
			throw new CustomException(errorExceptionMessage);
		}
		return employees;
	}
	
	@Override
	public Long getCount(String whereClause) {
		Long count = 0L;
		
		StringBuilder finalSql = new StringBuilder(GET_EMPLOYEE_COUNT);
		// where clause
		if (StringUtils.isNotEmpty(whereClause)) finalSql.append(whereClause.toString());
		
		log.info("Final SQL to get all employees Count w/w/o filter :: " + finalSql.toString());
		try {
			TypedQuery<Long> sql = this.entityManager.createQuery(finalSql.toString(), Long.class);
			count = sql.getSingleResult();
		} catch (Exception ex) {
			log.error("Exception occured at the time of fetching the count of employees :: " + ex.toString());
			
			String errorExceptionMessage = ex.getLocalizedMessage();
			if (errorExceptionMessage == null) errorExceptionMessage = ex.toString();
			
			throw new CustomException(errorExceptionMessage);
		}
		return count;
	}


}
