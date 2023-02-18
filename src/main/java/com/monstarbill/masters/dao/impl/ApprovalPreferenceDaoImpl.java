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
import com.monstarbill.masters.dao.ApprovalPreferenceDao;
import com.monstarbill.masters.models.ApprovalPreference;
import com.monstarbill.masters.payload.request.PaginationRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("approvalPreferenceDaoImpl")
public class ApprovalPreferenceDaoImpl implements ApprovalPreferenceDao {
	
	@PersistenceContext
	private EntityManager entityManager;
	
	public static final String GET_APPROVAL_PREFERENCE = "SELECT new com.monstarbill.masters.models.ApprovalPreference(ap.id, aps.id, aps.sequenceId, apc.level, ap.approvalType) FROM ApprovalPreference ap "
			+ " LEFT JOIN ApprovalPreferenceCondition apc ON ap.id = apc.approvalPreferenceId "
			+ " LEFT JOIN ApprovalPreferenceSequence aps ON apc.id = aps.conditionId "
			+ " WHERE 1 = 1 ";
	public static final String GET_APPROVAL_PREFERENCE_LIST = "SELECT new com.monstarbill.masters.models.ApprovalPreference(ap.id, ap.subsidiaryId, ap.approvalType, ap.recordType, ap.subType, s.name as subsidiaryName ) FROM ApprovalPreference ap "
			+ " INNER JOIN Subsidiary s ON s.id = ap.subsidiaryId "
			+ " WHERE 1 = 1 ";
	
	public static final String GET_APPROVAL_PREFERENCE_COUNT = "select count(*) FROM ApprovalPreference ap "
			+ " INNER JOIN Subsidiary s ON s.id = ap.subsidiaryId  WHERE 1 = 1";

	@Override
	public List<ApprovalPreference> findApproverMaxLevel(String whereClause) {
		List<ApprovalPreference> approvalPreferences = new ArrayList<ApprovalPreference>();
		
		StringBuilder finalSql = new StringBuilder(GET_APPROVAL_PREFERENCE);
		// where clause
		if (StringUtils.isNotEmpty(whereClause)) finalSql.append(whereClause.toString());
		
		finalSql.append(" order by aps.id desc ");
		
		log.info("SQL to get all ApprovalPreference With where clause :: " + finalSql.toString());
		try {
			TypedQuery<ApprovalPreference> sql = this.entityManager.createQuery(finalSql.toString(), ApprovalPreference.class);
			approvalPreferences = sql.getResultList();
		} catch (Exception ex) {
			log.error("Exception occured at the time of fetching the list of banks :: " + ex.toString());
			
			String errorExceptionMessage = ex.getLocalizedMessage();
			if (errorExceptionMessage == null) errorExceptionMessage = ex.toString();
			
			throw new CustomException(errorExceptionMessage);
		}
		return approvalPreferences;
	}

	@Override
	public List<ApprovalPreference> findAll(String whereClauses, PaginationRequest paginationRequest) {
		List<ApprovalPreference> approvalPreferences = new ArrayList<ApprovalPreference>();
		StringBuilder finalSql = new StringBuilder(GET_APPROVAL_PREFERENCE_LIST);
		if (StringUtils.isNotEmpty(whereClauses))
			finalSql.append(whereClauses.toString());
		finalSql.append(CommonUtils.prepareOrderByClause(paginationRequest.getSortColumn(), paginationRequest.getSortOrder()));
		log.info("Final SQL to get all Approval Prefrences " + finalSql.toString());
		try {
			TypedQuery<ApprovalPreference> sql = this.entityManager.createQuery(finalSql.toString(), ApprovalPreference.class);
			sql.setFirstResult(paginationRequest.getPageNumber() * paginationRequest.getPageSize());
			sql.setMaxResults(paginationRequest.getPageSize());
			approvalPreferences = sql.getResultList();
		} catch (Exception ex) {
			log.error("Exception occured at the time of fetching the list of Approval Prefrences :: " + ex.toString());
			String errorExceptionMessage = ex.getLocalizedMessage();
			if (errorExceptionMessage == null)
				errorExceptionMessage = ex.toString();
			throw new CustomException(errorExceptionMessage);
		}
		return approvalPreferences;

	}

	@Override
	public Long getCount(String whereClauses) {
		Long count = 0L;

		StringBuilder finalSql = new StringBuilder(GET_APPROVAL_PREFERENCE_COUNT);
		// where clause
		if (StringUtils.isNotEmpty(whereClauses))
			finalSql.append(whereClauses.toString());

		log.info("Final SQL to get all Approval Prefrence Count w/w/o filter :: " + finalSql.toString());
		try {
			TypedQuery<Long> sql = this.entityManager.createQuery(finalSql.toString(), Long.class);
			count = sql.getSingleResult();
		} catch (Exception ex) {
			log.error("Exception occured at the time of fetching the count of Approval Prefrence :: " + ex.toString());

			String errorExceptionMessage = ex.getLocalizedMessage();
			if (errorExceptionMessage == null)
				errorExceptionMessage = ex.toString();

			throw new CustomException(errorExceptionMessage);
		}
		return count;
	}

}
