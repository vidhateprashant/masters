package com.monstarbill.masters.controllers;

import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.monstarbill.masters.models.Bank;
import com.monstarbill.masters.models.BankHistory;
import com.monstarbill.masters.models.BankPaymentInstrument;
import com.monstarbill.masters.payload.request.PaginationRequest;
import com.monstarbill.masters.payload.response.PaginationResponse;
import com.monstarbill.masters.service.BankService;

import lombok.extern.slf4j.Slf4j;

/**
 * All WS's of the Bank and it's child components 
 * @author Prashant
 * 04-07-2022
 */
@Slf4j
@RestController
@RequestMapping("/bank")
//@CrossOrigin(origins= "*", allowedHeaders = "*", maxAge = 4800, allowCredentials = "false" )
public class BankController {

	@Autowired
	private BankService bankService;
	
	/**
	 * Save/update the Bank	
	 * @param bank
	 * @return
	 */
	@PostMapping("/save")
	public ResponseEntity<Bank> saveBank(@Valid @RequestBody Bank bank) {
		log.info("Saving the Bank :: " + bank.toString());
		bank = bankService.save(bank);
		log.info("Bank saved successfully");
		return ResponseEntity.ok(bank);
	}
	
	/**
	 * get Bank based on it's id
	 * @param id
	 * @return Bank
	 */
	@GetMapping("/get")
	public ResponseEntity<Bank> findById(@RequestParam Long id) {
		log.info("Get Bank for ID :: " + id);
		Bank bank = bankService.getBankById(id);
		if (bank == null) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		log.info("Returning from find by id Bank");
		return new ResponseEntity<>(bank, HttpStatus.OK);
	}
	
	/**
	 * get list of Banks with/without Filter 
	 * @return
	 */
	@PostMapping("/get/all")
	public ResponseEntity<PaginationResponse> findAll(@RequestBody PaginationRequest paginationRequest) {
		log.info("Get all banks started.");
		PaginationResponse paginationResponse = bankService.findAll(paginationRequest);
		log.info("Get all Bank completed.");
		return new ResponseEntity<>(paginationResponse, HttpStatus.OK);
	}

	/**
	 * soft delete the Bank by it's id
	 * @param id
	 * @return
	 */
	@GetMapping("/delete")
	public ResponseEntity<Boolean> deleteById(@RequestParam Long id) {
		log.info("Delete Bank by ID :: " + id);
		boolean isDeleted = false;
		isDeleted = bankService.deleteById(id);
		log.info("Delete Bank by ID Completed.");
		return new ResponseEntity<>(isDeleted, HttpStatus.OK);
	}
	
	/**
	 * Find history by Bank Id
	 * Supported for server side pagination
	 * @param id
	 * @param pageSize
	 * @param pageNumber
	 * @return
	 */
	@GetMapping("/get/history")
	public ResponseEntity<List<BankHistory>> findHistoryById(@RequestParam Long id, @RequestParam(defaultValue = "10") int pageSize, @RequestParam(defaultValue = "0") int pageNumber, @RequestParam(defaultValue = "id") String sortColumn) {
		log.info("Get Bank Audit for Supplier ID :: " + id);
		Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(sortColumn));
		List<BankHistory> bankHistoris = this.bankService.findHistoryById(id, pageable);
		log.info("Returning from Bank Audit by id.");
		return new ResponseEntity<>(bankHistoris, HttpStatus.OK);
	}
	
	/**
	 * Prashant - 04-07-2022
	 * Save the Bank Payment Instruments	
	 * Only insert operation here as per requirement
	 * @param bankPaymentInstrument
	 * @return
	 */
	@PostMapping("/bank-payment-instruments/save")
	public ResponseEntity<BankPaymentInstrument> saveBankPaymentInstrument(@Valid @RequestBody BankPaymentInstrument bankPaymentInstrument) {
		log.info("Saving the BankPaymentInstrument :: " + bankPaymentInstrument.toString());
		bankPaymentInstrument = bankService.save(bankPaymentInstrument);
		log.info("BankPaymentInstrument saved successfully");
		return ResponseEntity.ok(bankPaymentInstrument);
	}
	
	/**
	 * Prashant - 04-07-2022
	 * Delete(soft Delete) the Bank Payment Instruments	
	 * @param bankPaymentInstrument
	 * @return
	 */
	@GetMapping("/bank-payment-instruments/delete")
	public ResponseEntity<Boolean> deleteBankPaymentInstrument(@RequestParam Long id) {
		log.info("Delete Bank PaymentInstrument by ID :: " + id);
		boolean isDeleted = false;
		isDeleted = bankService.deleteBankPaymentInstrumentById(id);
		log.info("Delete Bank PaymentInstrument by ID Completed.");
		return new ResponseEntity<>(isDeleted, HttpStatus.OK);
	}
	
	@GetMapping("/is-valid-name")
	public ResponseEntity<Boolean> validateName(@RequestParam String name) {
		return new ResponseEntity<>(this.bankService.getValidateName(name), HttpStatus.OK);
	}
	
	/**
	 * get Bank based on it's subsidiary id
	 * @param subsidiary id
	 * @return Bank
	 */
	@GetMapping("/get-by-subsidiary-id")
	public ResponseEntity<List<Bank>> findBySubsidiaryId(@RequestParam Long subsidiaryId) {
		log.info("Get Bank for subsidiary Id :: " + subsidiaryId);
		List<Bank> bank = bankService.getBankBySubsidiaryId(subsidiaryId);
		if (bank == null) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		log.info("Returning from find by subsidiary Id Bank");
		return new ResponseEntity<>(bank, HttpStatus.OK);
	}
}
