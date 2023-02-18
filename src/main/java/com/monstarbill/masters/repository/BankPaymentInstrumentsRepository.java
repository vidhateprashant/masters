package com.monstarbill.masters.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.monstarbill.masters.models.BankPaymentInstrument;

@Repository
public interface BankPaymentInstrumentsRepository extends JpaRepository<BankPaymentInstrument, String> {	

	public Optional<BankPaymentInstrument> findByIdAndIsDeleted(Long id, boolean isDeleted);
	
	public List<BankPaymentInstrument> findByBankIdAndIsDeleted(Long bankId, boolean isDeleted);

}
