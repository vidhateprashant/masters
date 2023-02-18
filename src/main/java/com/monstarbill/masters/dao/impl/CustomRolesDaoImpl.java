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
import com.monstarbill.masters.dao.CustomRolesDao;
import com.monstarbill.masters.models.CustomRoles;
import com.monstarbill.masters.payload.request.PaginationRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("customRolesDaoImpl")
public class CustomRolesDaoImpl implements CustomRolesDao {
	
	@PersistenceContext
	private EntityManager entityManager;
	
	public static final String GET_ROLES = "select new com.monstarbill.masters.models.CustomRoles(r.id, r.subsidiaryId,r.isActive, r.name, r.createdDate, r.createdBy, s.name) from CustomRoles r"
			+ "	INNER JOIN Subsidiary s ON r.subsidiaryId = s.id WHERE 1=1 ";
	
	
	public static final String GET_ROLES_COUNT = "select count(1) FROM CustomRoles r INNER JOIN Subsidiary s ON r.subsidiaryId = s.id WHERE 1=1  ";

	@Override
	public List<CustomRoles> findAll(String whereClause, PaginationRequest paginationRequest) {
		List<CustomRoles> customRoles = new ArrayList<CustomRoles>();
		StringBuilder finalSql = new StringBuilder(GET_ROLES);
		if (StringUtils.isNotEmpty(whereClause))
			finalSql.append(whereClause.toString());
		finalSql.append(
				CommonUtils.prepareOrderByClause(paginationRequest.getSortColumn(), paginationRequest.getSortOrder()));
		log.info("Final SQL to get all Roles " + finalSql.toString());
		try {
			TypedQuery<CustomRoles> sql = this.entityManager.createQuery(finalSql.toString(), CustomRoles.class);
			sql.setFirstResult(paginationRequest.getPageNumber() * paginationRequest.getPageSize());
			sql.setMaxResults(paginationRequest.getPageSize());
			customRoles = sql.getResultList();
		} catch (Exception ex) {
			log.error("Exception occured at the time of fetching the list of roles :: " + ex.toString());
			String errorExceptionMessage = ex.getLocalizedMessage();
			if (errorExceptionMessage == null)
				errorExceptionMessage = ex.toString();
			throw new CustomException(errorExceptionMessage);
		}
		return customRoles;
	}

	@Override
	public Long getCount(String whereClause) {
		Long count = 0L;

		StringBuilder finalSql = new StringBuilder(GET_ROLES_COUNT);
		// where clause
		if (StringUtils.isNotEmpty(whereClause))
			finalSql.append(whereClause.toString());

		log.info("Final SQL to get all roles w/w/o filter :: " + finalSql.toString());
		try {
			TypedQuery<Long> sql = this.entityManager.createQuery(finalSql.toString(), Long.class);
			count = sql.getSingleResult();
		} catch (Exception ex) {
			log.error("Exception occured at the time of fetching the count of roles :: " + ex.toString());

			String errorExceptionMessage = ex.getLocalizedMessage();
			if (errorExceptionMessage == null)
				errorExceptionMessage = ex.toString();

			throw new CustomException(errorExceptionMessage);
		}
		return count;
	}
}
