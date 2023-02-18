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
import com.monstarbill.masters.dao.SupplierDao;
import com.monstarbill.masters.models.Supplier;
import com.monstarbill.masters.payload.request.PaginationRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("supplierDaoImpl")
public class SupplierDaoImpl implements SupplierDao {
	
	@PersistenceContext
	private EntityManager entityManager;
	
	public static final String GET_SUPPLIERS = " select new com.monstarbill.masters.models.Supplier(s.id, s.name, s.vendorNumber, s.vendorType, s.isActive, sub.name, sc.name, sc.contactNumber, s.approvalStatus, sa.access) "
			+ " from Supplier s "
			+ " INNER JOIN SupplierSubsidiary ss ON ss.supplierId = s.id "
			+ " INNER JOIN Subsidiary sub ON sub.id = ss.subsidiaryId "
			+ " LEFT JOIN SupplierContact sc ON sc.supplierId = s.id AND sc.isPrimaryContact is true"
			+ " INNER JOIN SupplierAccess sa ON sa.supplierId = s.id "
			+ " WHERE 1=1 ";
	
	public static final String GET_SUPPLIERS_COUNT = " select count(s) "
			+ " from Supplier s "
			+ " INNER JOIN SupplierSubsidiary ss ON ss.supplierId = s.id "
			+ " INNER JOIN Subsidiary sub ON sub.id = ss.subsidiaryId "
			+ " LEFT JOIN SupplierContact sc ON sc.supplierId = s.id AND sc.isPrimaryContact is true "
			+ " INNER JOIN SupplierAccess sa ON sa.supplierId = s.id "
			+ " WHERE 1=1 ";

	@Override
	public List<Supplier> findAll(String whereClause, PaginationRequest paginationRequest) {
		List<Supplier> suppliers = new ArrayList<Supplier>();
		
		StringBuilder finalSql = new StringBuilder(GET_SUPPLIERS);
		// where clause
		if (StringUtils.isNotEmpty(whereClause)) finalSql.append(whereClause.toString());
		
		// group by clause
		finalSql.append(" group by s.id, s.name, s.vendorNumber, s.vendorType, s.isActive, sub.name, sc.name, sc.contactNumber, sa.access ");
		
		// order by clause
		finalSql.append(CommonUtils.prepareOrderByClause(paginationRequest.getSortColumn(), paginationRequest.getSortOrder()));
		log.info("Final SQL to get all suppliers w/w/o filter :: " + finalSql.toString());
		
		try {
			TypedQuery<Supplier> sql = this.entityManager.createQuery(finalSql.toString(), Supplier.class);
			// pagination
			sql.setFirstResult(paginationRequest.getPageNumber() * paginationRequest.getPageSize());
			sql.setMaxResults(paginationRequest.getPageSize());
			suppliers = sql.getResultList();
		} catch (Exception ex) {
			log.error("Exception occured at the time of fetching the list of suppliers :: " + ex.toString());
			
			String errorExceptionMessage = ex.getLocalizedMessage();
			if (errorExceptionMessage == null) errorExceptionMessage = ex.toString();
			
			throw new CustomException(errorExceptionMessage);
		}
		return suppliers;
	}
	
	@Override
	public Long getCount(String whereClause) {
		Long count = 0L;
		
		StringBuilder finalSql = new StringBuilder(GET_SUPPLIERS_COUNT);
		// where clause
		if (StringUtils.isNotEmpty(whereClause)) finalSql.append(whereClause.toString());
		
		log.info("Final SQL to get all suppliers Count w/w/o filter :: " + finalSql.toString());
		try {
			TypedQuery<Long> sql = this.entityManager.createQuery(finalSql.toString(), Long.class);
			count = sql.getSingleResult();
		} catch (Exception ex) {
			log.error("Exception occured at the time of fetching the count of suppliers :: " + ex.toString());
			
			String errorExceptionMessage = ex.getLocalizedMessage();
			if (errorExceptionMessage == null) errorExceptionMessage = ex.toString();
			
			throw new CustomException(errorExceptionMessage);
		}
		return count;
	}

}
