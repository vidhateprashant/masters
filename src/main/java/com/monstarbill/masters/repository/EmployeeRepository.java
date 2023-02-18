package com.monstarbill.masters.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.monstarbill.masters.models.Employee;
import com.monstarbill.masters.payload.response.IdNameResponse;

public interface EmployeeRepository extends JpaRepository<Employee, String> {
	
	public Optional<Employee> findByIdAndIsDeleted(Long id, boolean isDeleted);

	@Query("select count(id) from Employee where lower(fullName) = lower(:name) AND isDeleted = false ")
	public Long getCountByName(@Param("name") String name);

	@Query("select new com.monstarbill.masters.models.Employee(id, concat(firstName,' ',lastName) as name) from Employee Where subsidiaryId = :subsidiaryId and isDeleted = :isDeleted order by firstName asc ")
	public List<Employee> getAllWithFieldsAndDeleted(@Param("subsidiaryId") Long subsidiaryId, @Param("isDeleted") boolean isDeleted);
	
	default Map<Long, String> findIdAndNameMap(@Param("subsidiaryId") Long subsidiaryId, boolean isDeleted) {
        return this.getAllWithFieldsAndDeleted(subsidiaryId, isDeleted).stream().collect(Collectors.toMap(Employee::getId, Employee::getFullName));
    }

	public List<Employee> getAllBySubsidiaryIdAndIsDeleted(Long subsidiaryId, boolean isDeleted);

	
	@Query(" Select new com.monstarbill.masters.payload.response.IdNameResponse(e.id, e.fullName) "
			+ " FROM Employee e "
			+ " INNER JOIN EmployeeRole er ON e.id = er.employeeId"
			+ " WHERE er.roleId = :roleId AND e.subsidiaryId = :subsidiaryId ")
	public List<IdNameResponse> findByRoleIdAndSubsidiary(Long roleId, Long subsidiaryId);

}
