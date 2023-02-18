package com.monstarbill.masters.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.monstarbill.masters.models.Item;

@Repository
public interface ItemRepository extends JpaRepository<Item, String> {

	public Optional<Item> findByIdAndIsDeleted(Long id, boolean isDeleted);

	@Query(" SELECT new com.monstarbill.masters.models.Item(id, subsidiaryId, category, name, description, uom, integratedId,"
			+ " case "
			+ " 	when lower(i.category) = lower('Inventory Item')  "
			+ " 	then i.assetAccountId  "
			+ " 	else i.expenseAccountId end as accountId) "
			+ " from Item i WHERE isDeleted is false AND i.isActive is true AND subsidiaryId = :subsidiaryId ")
	public List<Item> findBySubsidiaryId(@Param("subsidiaryId") Long subsidiaryId);

	@Query("select count(id) from Item where lower(name) = lower(:name) AND isDeleted = false ")
	public Long getCountByName(@Param("name") String name);
	
	public Optional<Item> findByDescriptionAndIsDeleted(String description, boolean isDeleted);
	
	public Optional<Item> findByNameAndIsDeleted(String name, boolean isDeleted);
	
	public List<Item> findByDescriptionContainingIgnoreCaseAndSubsidiaryIdAndIsDeleted(String description, Long subsidiaryId, boolean isDeleted);

	List<Item> findByDescriptionStartingWithAndSubsidiaryId(String description,Long SubsidiaryId);
	
	public Long countBySubsidiaryIdAndNatureOfItem(Long subsidiaryId, String natureOfItem);
}
