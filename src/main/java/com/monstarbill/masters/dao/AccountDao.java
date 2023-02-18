package com.monstarbill.masters.dao;

import java.util.List;

import org.springframework.stereotype.Component;

import com.monstarbill.masters.models.Account;
import com.monstarbill.masters.payload.request.PaginationRequest;

@Component("accountDao")
public interface AccountDao {
	
	public List<Account> findAll(String whereClause, PaginationRequest paginationRequest);
	public Long getCount(String whereClause);

}




