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
import com.monstarbill.masters.dao.BankDao;
import com.monstarbill.masters.models.Bank;
import com.monstarbill.masters.payload.request.PaginationRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("bankDaoImpl")
public class BankDaoImpl implements BankDao {
	
	@PersistenceContext
	private EntityManager entityManager;
	
	public static final String GET_BANKS = "select new com.monstarbill.masters.models.Bank(b.id, b.name, b.accountNumber, b.accountType, b.branch, b.currency, b.isActive, s.name as subsidiaryName) "
			+ " from Bank b "
			+ " inner join Subsidiary s ON s.id = b.subsidiaryId WHERE 1=1 ";
	
	public static final String GET_BANKS_COUNT = "select count(b) "
			+ " from Bank b "
			+ " inner join Subsidiary s ON s.id = b.subsidiaryId WHERE 1=1 ";

	@Override
	public List<Bank> findAll(String whereClause, PaginationRequest paginationRequest) {
		List<Bank> banks = new ArrayList<Bank>();
		
		StringBuilder finalSql = new StringBuilder(GET_BANKS);
		// where clause
		if (StringUtils.isNotEmpty(whereClause)) finalSql.append(whereClause.toString());
		// order by clause
		finalSql.append(CommonUtils.prepareOrderByClause(paginationRequest.getSortColumn(), paginationRequest.getSortOrder()));
		log.info("Final SQL to get all Banks w/w/o filter :: " + finalSql.toString());
		
		try {
			TypedQuery<Bank> sql = this.entityManager.createQuery(finalSql.toString(), Bank.class);
			// pagination
			sql.setFirstResult(paginationRequest.getPageNumber() * paginationRequest.getPageSize());
			sql.setMaxResults(paginationRequest.getPageSize());
			banks = sql.getResultList();
		} catch (Exception ex) {
			log.error("Exception occured at the time of fetching the list of banks :: " + ex.toString());
			
			String errorExceptionMessage = ex.getLocalizedMessage();
			if (errorExceptionMessage == null) errorExceptionMessage = ex.toString();
			
			throw new CustomException(errorExceptionMessage);
		}
		return banks;
	}
	
	@Override
	public Long getCount(String whereClause) {
		Long count = 0L;
		
		StringBuilder finalSql = new StringBuilder(GET_BANKS_COUNT);
		// where clause
		if (StringUtils.isNotEmpty(whereClause)) finalSql.append(whereClause.toString());
		
		log.info("Final SQL to get all Bank Count w/w/o filter :: " + finalSql.toString());
		try {
			TypedQuery<Long> sql = this.entityManager.createQuery(finalSql.toString(), Long.class);
			count = sql.getSingleResult();
		} catch (Exception ex) {
			log.error("Exception occured at the time of fetching the count of Bank :: " + ex.toString());
			
			String errorExceptionMessage = ex.getLocalizedMessage();
			if (errorExceptionMessage == null) errorExceptionMessage = ex.toString();
			
			throw new CustomException(errorExceptionMessage);
		}
		return count;
	}

}
