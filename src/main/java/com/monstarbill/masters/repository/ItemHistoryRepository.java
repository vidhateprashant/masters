package com.monstarbill.masters.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.monstarbill.masters.models.ItemHistory;

/**
 * Repository for the Item and it's childs history
 * @author Prashant
 * 07-Jul-2022
 */
@Repository
public interface ItemHistoryRepository extends JpaRepository<ItemHistory, String> {

	public List<ItemHistory> findByItemId(Long id, Pageable pageable);
	
}
