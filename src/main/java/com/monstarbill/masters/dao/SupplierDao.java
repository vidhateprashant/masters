package com.monstarbill.masters.dao;

import java.util.List;

import org.springframework.stereotype.Component;

import com.monstarbill.masters.models.Supplier;
import com.monstarbill.masters.payload.request.PaginationRequest;

@Component("supplierDao")
public interface SupplierDao {
	
	public List<Supplier> findAll(String whereClause, PaginationRequest paginationRequest);

	public Long getCount(String whereClause);
	
}
