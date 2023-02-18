package com.monstarbill.masters.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.monstarbill.masters.models.Bank;

@Repository
public interface BankRepository extends JpaRepository<Bank, String> {

	public Optional<Bank> findByIdAndIsDeleted(Long id, boolean isDeleted);

	@Query("select count(id) from Bank where lower(accountNumber) = lower(:name) AND isDeleted = false ")
	public Long getCountByName(@Param("name") String name);

	public List<Bank> getAllBySubsidiaryIdAndIsDeleted(Long subsidiaryId, boolean isDeleted);

}
