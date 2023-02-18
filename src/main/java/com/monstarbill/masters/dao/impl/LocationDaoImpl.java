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
import com.monstarbill.masters.dao.LocationDao;
import com.monstarbill.masters.models.Location;
import com.monstarbill.masters.payload.request.PaginationRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("locationDaoImpl")
public class LocationDaoImpl implements LocationDao {
	
	@PersistenceContext
	private EntityManager entityManager;
	
	public static final String GET_LOCATION = "select new com.monstarbill.masters.models.Location(b.id, b.locationName, b.locationType, b.effectiveFrom, s.name as subsidiaryName, a.locationName as parentLocationName, b.parentLocationId, b.subsidiaryId) "
			+ " from Location b "
			+ " inner join Subsidiary s ON s.id = b.subsidiaryId "
			+ " Left join Location a on b.parentLocationId = a.id WHERE 1=1 ";
	
	public static final String GET_LOCATION_COUNT = "select count(*) from Location b WHERE 1=1 ";

	@Override
	public List<Location> findAll(String whereClause, PaginationRequest paginationRequest) {
		List<Location> locations = new ArrayList<Location>();
		StringBuilder finalSql = new StringBuilder(GET_LOCATION);
		if (StringUtils.isNotEmpty(whereClause))
			finalSql.append(whereClause.toString());
		finalSql.append(
				CommonUtils.prepareOrderByClause(paginationRequest.getSortColumn(), paginationRequest.getSortOrder()));
		log.info("Final SQL to get all Location " + finalSql.toString());
		try {
			TypedQuery<Location> sql = this.entityManager.createQuery(finalSql.toString(),
					Location.class);
			sql.setFirstResult(paginationRequest.getPageNumber() * paginationRequest.getPageSize());
			sql.setMaxResults(paginationRequest.getPageSize());
			locations = sql.getResultList();
		} catch (Exception ex) {
			log.error("Exception occured at the time of fetching the list of location :: " + ex.toString());
			String errorExceptionMessage = ex.getLocalizedMessage();
			if (errorExceptionMessage == null)
				errorExceptionMessage = ex.toString();
			throw new CustomException(errorExceptionMessage);
		}
		return locations;
	}

	public Long getCount(String whereClause) {
		Long count = 0L;

		StringBuilder finalSql = new StringBuilder(GET_LOCATION_COUNT);
		// where clause
		if (StringUtils.isNotEmpty(whereClause))
			finalSql.append(whereClause.toString());

		log.info("Final SQL to get all Location Count w/w/o filter :: " + finalSql.toString());
		try {
			TypedQuery<Long> sql = this.entityManager.createQuery(finalSql.toString(), Long.class);
			count = sql.getSingleResult();
		} catch (Exception ex) {
			log.error("Exception occured at the time of fetching the count of location :: " + ex.toString());

			String errorExceptionMessage = ex.getLocalizedMessage();
			if (errorExceptionMessage == null)
				errorExceptionMessage = ex.toString();

			throw new CustomException(errorExceptionMessage);
		}
		return count;
	}
	}
	

