package com.monstarbill.masters.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.monstarbill.masters.models.RolesHistory;

/**
 * Repository for the Custom Role and it's childs history
 * @author Prashant
 */
@Repository
public interface RoleHistoryRepository extends JpaRepository<RolesHistory, String> {

	public List<RolesHistory> findByRoleId(Long id, Pageable pageable);

}
