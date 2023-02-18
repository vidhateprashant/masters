package com.monstarbill.masters.dao;

import java.util.List;

import org.springframework.stereotype.Component;

import com.monstarbill.masters.models.ApprovalPreference;
import com.monstarbill.masters.payload.request.PaginationRequest;

@Component("approvalPreferenceDao")
public interface ApprovalPreferenceDao {
	
	public List<ApprovalPreference> findApproverMaxLevel(String whereClause);

	public List<ApprovalPreference> findAll(String whereClauses, PaginationRequest paginationRequest);

	public Long getCount(String whereClauses);

}
