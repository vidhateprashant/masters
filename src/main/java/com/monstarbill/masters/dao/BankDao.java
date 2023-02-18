package com.monstarbill.masters.dao;

import java.util.List;

import org.springframework.stereotype.Component;

import com.monstarbill.masters.models.Bank;
import com.monstarbill.masters.payload.request.PaginationRequest;

@Component("bankDao")
public interface BankDao {
	
	public List<Bank> findAll(String whereClause, PaginationRequest paginationRequest);

	public Long getCount(String whereClause);
	
}
