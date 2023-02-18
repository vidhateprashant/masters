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
import com.monstarbill.masters.dao.ItemDao;
import com.monstarbill.masters.models.Item;
import com.monstarbill.masters.payload.request.PaginationRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("itemDaoImpl")
public class ItemDaoImpl implements ItemDao {
	
	@PersistenceContext
	private EntityManager entityManager;
	
	public static final String GET_ITEM = "select new com.monstarbill.masters.models.Item(i.id, i.name, i.description, i.category, i.uom, i.isActive, s.name) from Item i inner join Subsidiary s ON i.subsidiaryId = s.id WHERE 1=1 ";

	public static final String GET_ITEM_COUNT = "select count(*) from Item i inner join Subsidiary s ON i.subsidiaryId = s.id WHERE 1=1 ";
	
	@Override
	public List<Item> findAll(String whereClause, PaginationRequest paginationRequest) {
		List<Item> items = new ArrayList<Item>();
		
		StringBuilder finalSql = new StringBuilder(GET_ITEM);
		// where clause
		if (StringUtils.isNotEmpty(whereClause)) finalSql.append(whereClause.toString());
		// order by clause
		finalSql.append(CommonUtils.prepareOrderByClause(paginationRequest.getSortColumn(), paginationRequest.getSortOrder()));
		
		log.info("Final SQL to get all Items w/w/o filter :: " + finalSql.toString());
		try {
			TypedQuery<Item> sql = this.entityManager.createQuery(finalSql.toString(), Item.class);
			// pagination
			sql.setFirstResult(paginationRequest.getPageNumber() * paginationRequest.getPageSize());
			sql.setMaxResults(paginationRequest.getPageSize());
			items = sql.getResultList();
		} catch (Exception ex) {
			log.error("Exception occured at the time of fetching the list of Items :: " + ex.toString());
			
			String errorExceptionMessage = ex.getLocalizedMessage();
			if (errorExceptionMessage == null) errorExceptionMessage = ex.toString();
			
			throw new CustomException(errorExceptionMessage);
		}
		return items;
	}

	@Override
	public Long getCount(String whereClause) {
		Long count = 0L;
		
		StringBuilder finalSql = new StringBuilder(GET_ITEM_COUNT);
		if (StringUtils.isNotEmpty(whereClause)) finalSql.append(whereClause.toString());
		
		log.info("Final SQL to get all Items Count w/w/o filter :: " + finalSql.toString());
		try {
			TypedQuery<Long> sql = this.entityManager.createQuery(finalSql.toString(), Long.class);
			count = sql.getSingleResult();
		} catch (Exception ex) {
			log.error("Exception occured at the time of fetching the count of Items :: " + ex.toString());
			
			String errorExceptionMessage = ex.getLocalizedMessage();
			if (errorExceptionMessage == null) errorExceptionMessage = ex.toString();
			
			throw new CustomException(errorExceptionMessage);
		}
		return count;
	}
}
