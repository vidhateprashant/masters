package com.monstarbill.masters.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.monstarbill.masters.models.SupplierHistory;

/**
 * Repository for the Supplier and it's childs history
 * @author Prashant
 *
 */
@Repository
public interface SupplierHistoryRepository extends JpaRepository<SupplierHistory, String> {

	public List<SupplierHistory> findBySupplierIdOrderById(Long id, Pageable pageable);

}
