package com.monstarbill.masters.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.monstarbill.masters.models.ApprovalPreference;
import com.monstarbill.masters.models.ApprovalPreferenceHistory;
import com.monstarbill.masters.payload.request.ApprovalRequest;
import com.monstarbill.masters.payload.request.PaginationRequest;
import com.monstarbill.masters.payload.response.PaginationResponse;

public interface ApprovalPreferenceService {

	public List<ApprovalPreferenceHistory> findHistoryById(Long id, Pageable pageable);

	public ApprovalPreference save(ApprovalPreference approvalPreference);

	public ApprovalPreference getApprovalPreferenceById(Long id);

	public ApprovalPreference findApproverMaxLevel(ApprovalRequest approvalRequest);

	public ApprovalRequest findApproverByLevelAndSequence(Long id, String level, Long sequenceId);

	public String findPreferenceTypeById(Long approvalPreferenceId);

	public PaginationResponse findAll(PaginationRequest paginationRequest);

}
