package com.monstarbill.masters.dao.impl;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.monstarbill.masters.commons.CommonUtils;
import com.monstarbill.masters.commons.CustomException;
import com.monstarbill.masters.dao.AccountDao;
import com.monstarbill.masters.models.Account;
import com.monstarbill.masters.payload.request.PaginationRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("accountDaoImpl")
public class AccountDaoImpl implements AccountDao {
	
	@PersistenceContext
	private EntityManager entityManager;
	
	public static final String GET_ACCOUNT = " select a.id, a.code, a.description, a.type, a.is_inactive, string_agg(s.subsidiary_name, ', '), "
	+ " CONCAT(a.code ,'-', a.type) "
	+ " from setup.account a "
	+ " LEFT JOIN setup.account_subsidiary s ON a.id = s.account_id "
	+ " where a.is_deleted is false and s.is_deleted is false ";
	
	public static final String GET_ACCOUNT_COUNT = "select count(*) from setup.account a "
			+ " LEFT JOIN setup.account_subsidiary s ON a.id = s.account_id "
			+ " where a.is_deleted is false and s.is_deleted is false ";

	@SuppressWarnings("unchecked")
	@Override
	public List<Account> findAll(String whereClause, PaginationRequest paginationRequest) {
		List<Account> accounts = new ArrayList<Account>();
		
		StringBuilder finalSql = new StringBuilder(GET_ACCOUNT);
		// where clause
		if (StringUtils.isNotEmpty(whereClause)) finalSql.append(whereClause.toString());
		finalSql.append(" group by a.id, a.code, a.description, a.type, a.is_inactive, s.subsidiary_name ");
		// order by clause
		finalSql.append(CommonUtils.prepareOrderByClause(paginationRequest.getSortColumn(), paginationRequest.getSortOrder()));
		
		log.info("Final SQL to get all Account w/w/o filter :: " + finalSql.toString());
		try {
			Query sql = this.entityManager.createNativeQuery(finalSql.toString());
			// pagination
			sql.setFirstResult(paginationRequest.getPageNumber() * paginationRequest.getPageSize());
			sql.setMaxResults(paginationRequest.getPageSize());
			List<Object> results = sql.getResultList();
			
			for (int i=0; i < results.size(); i++) {
				Account account = new Account();
				Object[] row = (Object[]) results.get(i);
				// a.id, a.code, a.description, a.type, a.is_inactive, string_agg(s.subsidiary_name, ', ')
				account.setId(((Number) row[0]).longValue());
				account.setCode((String) row[1]);
				account.setDescription((String) row[2]);
				account.setType((String) row[3]);
				account.setInactive((Boolean) row[4]);
				account.setSubsidiaryName((String) row[5]);
				account.setCodeWithType((String) row [6]);
				accounts.add(account);
			}
		} catch (Exception ex) {
			log.error("Exception occured at the time of fetching the list of accounts :: " + ex.toString());
			
			String errorExceptionMessage = ex.getLocalizedMessage();
			if (errorExceptionMessage == null) errorExceptionMessage = ex.toString();
			
			throw new CustomException(errorExceptionMessage);
		}
		return accounts;
	}

	@Override
	public Long getCount(String whereClause) {
		Long count = 0L;
		
		StringBuilder finalSql = new StringBuilder(GET_ACCOUNT_COUNT);
		// where clause
		if (StringUtils.isNotEmpty(whereClause)) finalSql.append(whereClause.toString());
		
		log.info("Final SQL to get all Account Count w/w/o filter :: " + finalSql.toString());
		try {
			Query sql = this.entityManager.createNativeQuery(finalSql.toString());
			count = ((Number) sql.getSingleResult()).longValue();
		} catch (Exception ex) {
			log.error("Exception occured at the time of fetching the count of Account :: " + ex.toString());
			
			String errorExceptionMessage = ex.getLocalizedMessage();
			if (errorExceptionMessage == null) errorExceptionMessage = ex.toString();
			
			throw new CustomException(errorExceptionMessage);
		}
		return count;
	}
}
