
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
import com.monstarbill.masters.dao.ProjectDao;
import com.monstarbill.masters.models.Project;
import com.monstarbill.masters.payload.request.PaginationRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("projectDaoImpl")
public class ProjectDaoImpl implements ProjectDao {
	
	@PersistenceContext
	private EntityManager entityManager;
	
	public static final String GET_PROJECT = "select new com.monstarbill.masters.models.Project(p.id, p.name, p.projectId, p.schedulingStartDate, p.schedulingEndDate, s.name as subsidiaryName) "
			+ " from Project p inner join Subsidiary s ON s.id = p.subsidiaryId WHERE 1=1 ";
	
	public static final String GET_PROJECT_COUNT = "select count(p) "
			+ " from Project p inner join Subsidiary s ON s.id = p.subsidiaryId WHERE 1=1 ";

	@Override
	public List<Project> findAll(String whereClause, PaginationRequest paginationRequest) {
		List<Project> project = new ArrayList<Project>();
		
		StringBuilder finalSql = new StringBuilder(GET_PROJECT);
		// where clause
		if (StringUtils.isNotEmpty(whereClause)) finalSql.append(whereClause.toString());
		// order by clause
		finalSql.append(CommonUtils.prepareOrderByClause(paginationRequest.getSortColumn(), paginationRequest.getSortOrder()));
		
		log.info("Final SQL to get all Project w/w/o filter :: " + finalSql.toString());
		try {
			TypedQuery<Project> sql = this.entityManager.createQuery(finalSql.toString(), Project.class);
			// pagination
			sql.setFirstResult(paginationRequest.getPageNumber() * paginationRequest.getPageSize());
			sql.setMaxResults(paginationRequest.getPageSize());
			project = sql.getResultList();
		} catch (Exception ex) {
			log.error("Exception occured at the time of fetching the list of Project :: " + ex.toString());
			
			String errorExceptionMessage = ex.getLocalizedMessage();
			if (errorExceptionMessage == null) errorExceptionMessage = ex.toString();
			
			throw new CustomException(errorExceptionMessage);
		}
		return project;
	}
	
	@Override
	public Long getCount(String whereClause) {
		Long count = 0L;
		
		StringBuilder finalSql = new StringBuilder(GET_PROJECT_COUNT);
		// where clause
		if (StringUtils.isNotEmpty(whereClause)) finalSql.append(whereClause.toString());
		
		log.info("Final SQL to get all Project Count w/w/o filter :: " + finalSql.toString());
		try {
			TypedQuery<Long> sql = this.entityManager.createQuery(finalSql.toString(), Long.class);
			count = sql.getSingleResult();
		} catch (Exception ex) {
			log.error("Exception occured at the time of fetching the count of Project :: " + ex.toString());
			
			String errorExceptionMessage = ex.getLocalizedMessage();
			if (errorExceptionMessage == null) errorExceptionMessage = ex.toString();
			
			throw new CustomException(errorExceptionMessage);
		}
		return count;
	}
	
}

