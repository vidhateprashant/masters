package com.monstarbill.masters.service;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.monstarbill.masters.models.GraphData;
import com.monstarbill.masters.models.Item;
import com.monstarbill.masters.models.ItemHistory;
import com.monstarbill.masters.payload.request.PaginationRequest;
import com.monstarbill.masters.payload.response.PaginationResponse;

public interface ItemService {

	public Item save(Item item);

	public Item findById(Long id);

	public List<ItemHistory> findHistoryById(Long id, Pageable pageable);

	public boolean deleteById(Long id);

	public PaginationResponse findAll(PaginationRequest paginationRequest);

	public List<Item> findBySubsidiaryId(Long subsidiaryId);

	public Boolean getValidateName(String name);

	public byte[] upload(MultipartFile file);

	public List<Item> getByDescriptionMatching(String description, Long subsidiaryId);

	public byte[] downloadTemplate();
	
	public List<Item> findDescriptionLike(String description ,Long SubsidiaryId);
	
	public List<GraphData> getdashboard(Long subsidiaryId);

	public Item findByName(String itemName);

}
