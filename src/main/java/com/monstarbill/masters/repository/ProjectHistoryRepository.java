package com.monstarbill.masters.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.monstarbill.masters.models.ProjectHistory;

public interface ProjectHistoryRepository extends JpaRepository<ProjectHistory, String>{
	
	public List<ProjectHistory> findByProjectIdOrderById(Long id, Pageable pageable);

}
