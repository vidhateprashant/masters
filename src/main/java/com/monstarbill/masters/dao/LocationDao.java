package com.monstarbill.masters.dao;

import java.util.List;

import org.springframework.stereotype.Component;

import com.monstarbill.masters.models.Location;
import com.monstarbill.masters.payload.request.PaginationRequest;

@Component("locationDao")
public interface LocationDao {
	
	public List<Location> findAll(String whereClause,  PaginationRequest paginationRequest);

	public Long getCount(String whereClause);

}
