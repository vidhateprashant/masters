package com.monstarbill.masters.dao;

import java.util.List;

import org.springframework.stereotype.Component;

import com.monstarbill.masters.models.Project;
import com.monstarbill.masters.payload.request.PaginationRequest;

@Component("projectDao")
public interface ProjectDao {
	
	public List<Project> findAll(String whereClause, PaginationRequest paginationRequest);

	public Long getCount(String whereClause);

}
