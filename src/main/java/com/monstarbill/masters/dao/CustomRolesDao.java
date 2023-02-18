package com.monstarbill.masters.dao;

import java.util.List;

import org.springframework.stereotype.Component;

import com.monstarbill.masters.models.CustomRoles;
import com.monstarbill.masters.payload.request.PaginationRequest;

@Component("customRolesDao")
public interface CustomRolesDao {

	public Long getCount(String whereClause);

	public List<CustomRoles> findAll(String whereClause, PaginationRequest paginationRequest);
}
