package com.monstarbill.masters.dao;

import java.util.List;

import org.springframework.stereotype.Component;

import com.monstarbill.masters.models.Item;
import com.monstarbill.masters.payload.request.PaginationRequest;

@Component("itemDao")
public interface ItemDao {
	public List<Item> findAll(String whereClause, PaginationRequest paginationRequest);

	public Long getCount(String whereClause);
}
