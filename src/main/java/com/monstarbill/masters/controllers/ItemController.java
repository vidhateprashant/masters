package com.monstarbill.masters.controllers;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.monstarbill.masters.commons.CustomMessageException;
import com.monstarbill.masters.commons.ExcelHelper;
import com.monstarbill.masters.models.GraphData;
import com.monstarbill.masters.models.Item;
import com.monstarbill.masters.models.ItemHistory;
import com.monstarbill.masters.payload.request.PaginationRequest;
import com.monstarbill.masters.payload.response.PaginationResponse;
import com.monstarbill.masters.service.ItemService;

import lombok.extern.slf4j.Slf4j;

/**
 * All WS's of the Item and it's child components if any
 * 
 * @author Prashant 06-07-2022
 */
@Slf4j
@RestController
@RequestMapping("/item")
//@CrossOrigin(origins = "*", allowedHeaders = "*", maxAge = 4800, allowCredentials = "false")
public class ItemController {

	@Autowired
	private ItemService itemService;

	/**
	 * Save/update the Item
	 * 
	 * @param item
	 * @return
	 */
	@PostMapping("/save")
	public ResponseEntity<Item> save(@Valid @RequestBody Item item) {
		log.info("Saving the Item :: " + item.toString());
		item = itemService.save(item);
		log.info("Item saved successfully");
		return ResponseEntity.ok(item);
	}

	/**
	 * get Item based on it's id
	 * 
	 * @param id
	 * @return Item
	 */
	@GetMapping("/get")
	public ResponseEntity<Item> findById(@RequestParam Long id) {
		log.info("Get Item for ID :: " + id);
		Item item = itemService.findById(id);
		if (item == null) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		log.info("Returning from find by id Item");
		return new ResponseEntity<>(item, HttpStatus.OK);
	}

	/**
	 * get list of Items with/without Filter
	 * 
	 * @return
	 */
	@PostMapping("/get/all")
	public ResponseEntity<PaginationResponse> findAll(@RequestBody PaginationRequest paginationRequest) {
		log.info("Get all Items started.");
		PaginationResponse paginationResponse = new PaginationResponse();
		paginationResponse = itemService.findAll(paginationRequest);
		log.info("Get all Item completed.");
		return new ResponseEntity<>(paginationResponse, HttpStatus.OK);
	}

	/**
	 * soft delete the Item by it's id
	 * 
	 * @param id
	 * @return
	 */
	@GetMapping("/delete")
	public ResponseEntity<Boolean> deleteById(@RequestParam Long id) {
		log.info("Delete Item by ID :: " + id);
		boolean isDeleted = false;
		isDeleted = itemService.deleteById(id);
		log.info("Delete Item by ID Completed.");
		return new ResponseEntity<>(isDeleted, HttpStatus.OK);
	}

	/**
	 * Find history by Item Id Supported for server side pagination
	 * 
	 * @param id
	 * @param pageSize
	 * @param pageNumber
	 * @return
	 */
	@GetMapping("/get/history")
	public ResponseEntity<List<ItemHistory>> findHistoryById(@RequestParam Long id,
			@RequestParam(defaultValue = "10") int pageSize, @RequestParam(defaultValue = "0") int pageNumber,
			@RequestParam(defaultValue = "id") String sortColumn) {
		log.info("Get Item Audit for Supplier ID :: " + id);
		Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(sortColumn));
		List<ItemHistory> itemHistoris = this.itemService.findHistoryById(id, pageable);
		log.info("Returning from Item Audit by id.");
		return new ResponseEntity<>(itemHistoris, HttpStatus.OK);
	}

	/**
	 * get Item based on subsidiary
	 * 
	 * @param subsidiaryId
	 * @return
	 */
	@GetMapping("/find-by-subsidiary")
	public ResponseEntity<List<Item>> findBySubsidiaryId(@RequestParam Long subsidiaryId) {
		log.info("Get Items based on subsidiary ID is started for :: " + subsidiaryId);
		List<Item> items = new ArrayList<Item>();
		items = itemService.findBySubsidiaryId(subsidiaryId);
		if (items == null) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		log.info("Get Items based on subsidiary ID is complteted");
		return new ResponseEntity<>(items, HttpStatus.OK);
	}

	@GetMapping("/is-valid-name")
	public ResponseEntity<Boolean> validateName(@RequestParam String name) {
		return new ResponseEntity<>(this.itemService.getValidateName(name), HttpStatus.OK);
	}

	@GetMapping("/download-template")
	public HttpEntity<ByteArrayResource> downloadTemplate() {
		try {
			byte[] excelContent = itemService.downloadTemplate();

			HttpHeaders header = new HttpHeaders();
			header.setContentType(new MediaType("application", "force-download"));
			header.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=item_template.xlsx");

			return new HttpEntity<>(new ByteArrayResource(excelContent), header);
		} catch (Exception e) {
			log.error("Something went wrong while downloading the Template. Please contact Administrator. Message : " + e.getLocalizedMessage());
			throw new CustomMessageException("Something went wrong while downloading the Template. Please contact Administrator. Message : " + e.getLocalizedMessage());
		}
	}
	
	@PostMapping("/upload")
	public HttpEntity<ByteArrayResource> uploadFile(@RequestParam("file") MultipartFile file) {
		String message = "";

		if (ExcelHelper.hasExcelFormat(file)) {
			try {
				byte[] excelContent = itemService.upload(file);

				HttpHeaders header = new HttpHeaders();
				header.setContentType(new MediaType("application", "force-download"));
				header.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=item_import_status.xlsx");

				return new HttpEntity<>(new ByteArrayResource(excelContent), header);
			} catch (Exception e) {
				message = "Could not upload the file: " + file.getOriginalFilename() + "!";
				throw new CustomMessageException(message + ", Message : " + e.getLocalizedMessage());
			}
		}
		return null;
	}

	/**
	 * get Item based on it's Desc Containing 1st Word
	 * 
	 * @param description
	 * @return Items
	 */
	@GetMapping("/getByDescMatch")
	public List<Item> getByDescriptionMatching(@RequestParam String description, @RequestParam Long subsidiaryId) {
		return itemService.getByDescriptionMatching(description, subsidiaryId);
	}
	
	@GetMapping("/find-by-desc")
	public ResponseEntity<List<Item>> findByDescription(@RequestParam String description, Long SubsidiaryId) {
		log.info("Get Items based on description  and SubsidiaryId is started for :: " + description);
		List<Item> items = new ArrayList<Item>();
		items = itemService.findDescriptionLike(description, SubsidiaryId);
		if (items == null) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		log.info("Get Items based on description and SubsidiaryId is complteted");
		return new ResponseEntity<>(items, HttpStatus.OK);
	}
	
	@GetMapping("/get-dashboard-by-nature-of-item")
	public ResponseEntity<List<GraphData>> getdashboard(@RequestParam Long SubsidiaryId) {
		log.info("Get Items based on SubsidiaryId and Nature of Item is started for :: " + SubsidiaryId);
		List<GraphData> dashboard = itemService.getdashboard(SubsidiaryId);
		log.info("Get Items based on SubsidiaryId and NatureOfItem is complteted");
		return ResponseEntity.ok(dashboard);
	}
	
	@GetMapping("/find-by-name")
	public ResponseEntity<Item> findByName(@RequestParam String name) {
		log.info("Get Item for itemName :: " + name);
		Item item = this.itemService.findByName(name);
		log.info("Returning from find by itemName");
		return new ResponseEntity<>(item, HttpStatus.OK);
	}
}
