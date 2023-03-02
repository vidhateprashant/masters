package com.monstarbill.masters.service.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.monstarbill.masters.commons.AppConstants;
import com.monstarbill.masters.commons.CommonUtils;
import com.monstarbill.masters.commons.CustomException;
import com.monstarbill.masters.commons.CustomMessageException;
import com.monstarbill.masters.commons.ExcelHelper;
import com.monstarbill.masters.commons.FilterNames;
import com.monstarbill.masters.dao.ItemDao;
import com.monstarbill.masters.enums.Operation;
import com.monstarbill.masters.enums.Status;
import com.monstarbill.masters.feignclient.SetupServiceClient;
import com.monstarbill.masters.models.GraphData;
import com.monstarbill.masters.models.Item;
import com.monstarbill.masters.models.ItemHistory;
import com.monstarbill.masters.payload.request.PaginationRequest;
import com.monstarbill.masters.payload.response.PaginationResponse;
import com.monstarbill.masters.repository.AccountRepository;
import com.monstarbill.masters.repository.ItemHistoryRepository;
import com.monstarbill.masters.repository.ItemRepository;
import com.monstarbill.masters.service.ItemService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional
public class ItemServiceImpl implements ItemService {

	@Autowired
	private ItemRepository itemRepository;

	@Autowired
	private ItemHistoryRepository itemHistoryRepository;

	@Autowired
	private ItemDao itemDao;
	
	@Autowired
	private SetupServiceClient setupServiceClient;
	
	@Autowired
	private AccountRepository accountRepository;

	@Override
	public Item save(Item item) {
		Optional<Item> oldItem = Optional.empty();

		if (item.getId() == null) {
			item.setCreatedBy(setupServiceClient.getLoggedInUsername());
		} else {
			// Get the existing object using the deep copy
			oldItem = this.itemRepository.findByIdAndIsDeleted(item.getId(), false);
			if (oldItem.isPresent()) {
				try {
					oldItem = Optional.ofNullable((Item) oldItem.get().clone());
				} catch (CloneNotSupportedException e) {
					log.error("Error while Cloning the object. Please contact administrator.");
					throw new CustomException("Error while Cloning the object. Please contact administrator.");
				}
			}
		}

		item.setLastModifiedBy(setupServiceClient.getLoggedInUsername());
		if (item.isActive() == true) {
			item.setActiveDate(null);
		}

		try {
			item = this.itemRepository.save(item);
		} catch (DataIntegrityViolationException e) {
			log.error(" Item unique constrain violetd." + e.getMostSpecificCause());
			throw new CustomException("Item unique constrain violetd :" + e.getMostSpecificCause());
		}

		if (item == null) {
			log.info("Error while saving the Item.");
			throw new CustomMessageException("Error while saving the Item.");
		}

		// update the data in Item history table
		this.updateItemHistory(item, oldItem);

		return item;
	}

	/**
	 * This method save the data in history table Add entry as a Insert if Item is
	 * new Add entry as a Update if Item is exists
	 * 
	 * @param item
	 * @param oldItem
	 */
	private void updateItemHistory(Item item, Optional<Item> oldItem) {
		if (oldItem.isPresent()) {
			// insert the updated fields in history table
			List<ItemHistory> itemHistories = new ArrayList<ItemHistory>();
			try {
				itemHistories = oldItem.get().compareFields(item);
				if (CollectionUtils.isNotEmpty(itemHistories)) {
					this.itemHistoryRepository.saveAll(itemHistories);
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				log.error("Error while comparing the new and old objects. Please contact administrator.");
				throw new CustomException(
						"Error while comparing the new and old objects. Please contact administrator.");
			}
			log.info("Item History is updated successfully");
		} else {
			// Insert in history table as Operation - INSERT
			this.itemHistoryRepository.save(this.prepareItemHistory(item.getId(), null, AppConstants.ITEM,
					Operation.CREATE.toString(), item.getLastModifiedBy(), null, String.valueOf(item.getId())));
		}
	}

	/**
	 * Prepares the history for the Item
	 * 
	 * @param ItemId
	 * @param moduleName
	 * @param operation
	 * @param lastModifiedBy
	 * @param oldValue
	 * @param newValue
	 * @return
	 */
	public ItemHistory prepareItemHistory(Long itemId, Long childId, String moduleName, String operation,
			String lastModifiedBy, String oldValue, String newValue) {
		ItemHistory itemHistory = new ItemHistory();
		itemHistory.setItemId(itemId);
		itemHistory.setChildId(childId);
		itemHistory.setModuleName(moduleName);
		itemHistory.setChangeType(AppConstants.UI);
		itemHistory.setOperation(operation);
		itemHistory.setOldValue(oldValue);
		itemHistory.setNewValue(newValue);
		itemHistory.setLastModifiedBy(lastModifiedBy);
		return itemHistory;
	}

	@Override
	public Item findById(Long id) {
		Optional<Item> item = Optional.ofNullable(null);
		item = this.itemRepository.findByIdAndIsDeleted(id, false);
		if (!item.isPresent()) {
			log.info("Item is not exist for id - " + id);
			throw new CustomMessageException("Item is not exist for id - " + id);
		}
		return item.get();
	}

	@Override
	public PaginationResponse findAll(PaginationRequest paginationRequest) {
		List<Item> items = new ArrayList<Item>();

		// preparing where clause
		String whereClause = this.prepareWhereClause(paginationRequest).toString();

		// get list
		items = this.itemDao.findAll(whereClause, paginationRequest);

		// getting count
		Long totalRecords = this.itemDao.getCount(whereClause);

		// preparing the Response object
		return CommonUtils.setPaginationResponse(paginationRequest.getPageNumber(), paginationRequest.getPageSize(),
				items, totalRecords);
	}

	private StringBuilder prepareWhereClause(PaginationRequest paginationRequest) {
		Map<String, ?> filters = paginationRequest.getFilters();

		String category = null;
		String name = null;
		String natureOfItem = null;
		String description = null;
		String status = null;
		Long subsidiaryId = null;

		if (filters.containsKey(FilterNames.CATEGORY))
			category = (String) filters.get(FilterNames.CATEGORY);
		if (filters.containsKey(FilterNames.NAME))
			name = (String) filters.get(FilterNames.NAME);
		if (filters.containsKey(FilterNames.NATURE_OF_ITEM))
			natureOfItem = (String) filters.get(FilterNames.NATURE_OF_ITEM);
		if (filters.containsKey(FilterNames.DESCRIPTION))
			description = (String) filters.get(FilterNames.DESCRIPTION);
		if (filters.containsKey(FilterNames.STATUS))
			status = (String) filters.get(FilterNames.STATUS);
		if (filters.containsKey(FilterNames.SUBSIDIARY_ID))
			subsidiaryId = ((Number) filters.get(FilterNames.SUBSIDIARY_ID)).longValue();

		StringBuilder whereClause = new StringBuilder(" AND i.isDeleted is false ");
		if (StringUtils.isNotEmpty(category)) {
			whereClause.append(" AND lower(i.category) like lower ('%").append(category).append("%')");
		}
		if (StringUtils.isNotEmpty(name)) {
			whereClause.append(" AND lower(i.name) like lower('%").append(name).append("%')");
		}
		if (StringUtils.isNotEmpty(natureOfItem)) {
			whereClause.append(" AND lower(i.natureOfItem) like lower('%").append(natureOfItem).append("%')");
		}
		if (StringUtils.isNotEmpty(description)) {
			whereClause.append(" AND lower(i.description) like lower('%").append(description).append("%')");
		}
		if (subsidiaryId != null && subsidiaryId != 0) {
			whereClause.append(" AND i.subsidiaryId = ").append(subsidiaryId);
		}
		if (StringUtils.isNotEmpty(status)) {
			if (Status.ACTIVE.toString().equalsIgnoreCase(status)) {
				whereClause.append(" AND i.isActive is true ");
			} else if (Status.INACTIVE.toString().equalsIgnoreCase(status)) {
				whereClause.append(" AND i.isActive is false ");
			}
		}
		return whereClause;
	}

	@Override
	public boolean deleteById(Long id) {
		Item item = new Item();
		item = this.findById(id);
		item.setDeleted(true);

		item = this.itemRepository.save(item);

		if (item == null) {
			log.error("Error while deleting the Item : " + id);
			throw new CustomMessageException("Error while deleting the Item : " + id);
		}

		// update the operation in the history
		this.itemHistoryRepository.save(this.prepareItemHistory(item.getId(), null, AppConstants.ITEM,
				Operation.DELETE.toString(), item.getLastModifiedBy(), String.valueOf(item.getId()), null));

		return true;
	}

	@Override
	public List<ItemHistory> findHistoryById(Long id, Pageable pageable) {
		return this.itemHistoryRepository.findByItemId(id, pageable);
	}

	@Override
	public List<Item> findBySubsidiaryId(Long subsidiaryId) {
		List<Item> items = new ArrayList<Item>();
		try {
			items = this.itemRepository.findBySubsidiaryId(subsidiaryId);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new CustomException(
					"Error while fetching the Items based on the Subsidiary - " + subsidiaryId + ", " + ex.toString());
		}
		return items;
	}

	@Override
	public Boolean getValidateName(String name) {
		// if name is empty then name is not valid
		if (StringUtils.isEmpty(name))
			return false;

		Long countOfRecordsWithSameName = this.itemRepository.getCountByName(name.trim());
		// if we we found the count greater than 0 then it is not valid. If it is zero
		// then it is valid string
		if (countOfRecordsWithSameName > 0)
			return false;
		else
			return true;
	}

	@Override
	public byte[] upload(MultipartFile file) {
		try {
			return this.importItemsFromExcel(file);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException("Something went wrong. Please Contact Administrator...");
		}
	}

	public byte[] importItemsFromExcel(MultipartFile inputFile) {
		try {
			InputStream inputStream = inputFile.getInputStream();
			Workbook workbook = new XSSFWorkbook(inputStream);
			Sheet sheet = workbook.getSheet("Sheet1");
			Iterator<Row> rows = sheet.iterator();

			int statusColumnNumber = 0;
			int rowNumber = 0;
			
			boolean isError = false;
			StringBuilder errorMessage = new StringBuilder();
			
			while (rows.hasNext()) {
				errorMessage = new StringBuilder();
				int errorCount = 1;
				Row inputCurrentRow = rows.next();
				if (rowNumber == 0) {
					statusColumnNumber = inputCurrentRow.getLastCellNum();
					Cell cell = inputCurrentRow.createCell(statusColumnNumber);
					cell.setCellValue("Imported Status");
					rowNumber++;
					continue;
				}
				
				boolean isRowEmpty = ExcelHelper.checkIfRowIsEmpty(inputCurrentRow);

				// if row is empty it means all records completed.
				if (isRowEmpty) break;
				
				Item item = new Item();

				// External ID - REQUIRED
				try {
					if (inputCurrentRow.getCell(0) != null) {
//						item.setExternalId(inputCurrentRow.getCell(0).getStringCellValue());
						String externalId = new DataFormatter().formatCellValue(inputCurrentRow.getCell(0));
						item.setExternalId(externalId);
					} else {
						errorMessage.append(errorCount + ") External ID should not be empty. ");
						log.info("External ID should not be empty.");
						isError = true;
						errorCount++;
					}
				} catch (Exception e) {
					log.error("Exception External ID " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of External ID is invalid.");
					isError = true;
					errorCount++;
				}
				// Subsidiary Name & Id - REQUIRED
				try {
					if (inputCurrentRow.getCell(1) != null) {
						String subsidiaryName = inputCurrentRow.getCell(1).getStringCellValue();
						item.setSubsidiaryName(subsidiaryName);
						Long subsidiaryId = this.setupServiceClient.getSubsidiaryIdByName(subsidiaryName);
						if (subsidiaryId == null) {
							errorMessage.append(errorCount + ") Subsidiary : " + subsidiaryName + " is not found Please enter the valid Subsidiary Name. ");
							log.error("Subsidiary : " + subsidiaryName + " is not found. Please enter the valid Subsidiary Name. ");
							isError = true;
							errorCount++;
						}
						item.setSubsidiaryId(subsidiaryId);
					} else {
						errorMessage.append(errorCount + ") Subsidiary is required. ");
						log.error("Subsidiary is required. Please enter the valid Subsidiary Name. ");
						isError = true;
						errorCount++;
					}
				} catch (Exception e) {
					log.error("Exception subsidiary " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Subsidiary Name is invalid.");
					isError = true;
					errorCount++;
				}
				
				// Category/Type - REQUIRED
				try {
					if (inputCurrentRow.getCell(2) != null) {
						item.setCategory(inputCurrentRow.getCell(2).getStringCellValue());
					} else {
						errorMessage.append(errorCount + ") Item Type is required. ");
						log.error("Item Type is required. ");
						isError = true;
						errorCount++;
					}
				} catch (Exception e) {
					log.error("Exception Item type " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Item Type is invalid.");
					isError = true;
					errorCount++;
				}
				// Item code - REQUIRED
				try {
					if (inputCurrentRow.getCell(3) != null) {
						item.setName(inputCurrentRow.getCell(3).getStringCellValue());
					} else {
						errorMessage.append(errorCount + ") Item Code is required.");
						log.error("Item Code is required.");
						isError = true;
						errorCount++;
					}
				} catch (Exception e) {
					log.error("Exception COde " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Item Code is invalid.");
					isError = true;
					errorCount++;
				}
				// Description
				try {
					if (inputCurrentRow.getCell(4) != null) {
						item.setDescription(inputCurrentRow.getCell(4).getStringCellValue());
					}
				} catch (Exception e) {
					log.error("Exception Description " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Item Description is invalid.");
					isError = true;
					errorCount++;
				}
				// UOM - REQUIRED
				try {
					if (inputCurrentRow.getCell(5) != null) {
						item.setUom(inputCurrentRow.getCell(5).getStringCellValue());
					} else {
						errorMessage.append(errorCount + ") Item UOM is required.");
						log.error("Item UOM is required.");
						isError = true;
						errorCount++;
					}
				} catch (Exception e) {
					log.error("Exception UOM " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Item UOM is invalid.");
					isError = true;
					errorCount++;
				}
				// Costing method - REQUIRED
				try {
					if (inputCurrentRow.getCell(6) != null) {
						item.setCostingMethod(inputCurrentRow.getCell(6).getStringCellValue());
					} else {
						errorMessage.append(errorCount + ") Costing Method is required.");
						log.error("Costing Method is required.");
						isError = true;
						errorCount++;
					}
				} catch (Exception e) {
					log.error("Exception Costing method " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Costing Method is invalid.");
					isError = true;
					errorCount++;
				}
				// Purchasable - REQUIRED
				try {
					if (inputCurrentRow.getCell(7) != null) {
						boolean isPurchasable = false;
						String purchasable = inputCurrentRow.getCell(7).getStringCellValue();
						if ("yes".equalsIgnoreCase(purchasable)) isPurchasable = true;
						item.setPurchasable(isPurchasable);
					} else {
						errorMessage.append(errorCount + ") Purchasable is required.");
						log.error("Purchasable is required.");
						isError = true;
						errorCount++;
					}
				} catch (Exception e) {
					log.error("Exception Purchasable " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Purchasable is invalid.");
					isError = true;
					errorCount++;
				}
				// Salable
				try {
					if (inputCurrentRow.getCell(8) != null) {
						boolean isSalable = false;
						String salable = inputCurrentRow.getCell(8).getStringCellValue();
						if ("yes".equalsIgnoreCase(salable)) isSalable = true;
						item.setSalable(isSalable);
					}
				} catch (Exception e) {
					log.error("Exception Salable " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Salable is invalid.");
					isError = true;
					errorCount++;
				}
				// Inventory Type
				try {
					if (inputCurrentRow.getCell(9) != null) {
						item.setNatureOfItem(inputCurrentRow.getCell(9).getStringCellValue());
					}
				} catch (Exception e) {
					log.error("Exception Inventory Type " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Inventory Type is invalid.");
					isError = true;
					errorCount++;
				}
				// Integrated ID
				try {
					if (inputCurrentRow.getCell(10) != null) {
						Double number = inputCurrentRow.getCell(10).getNumericCellValue();
						item.setIntegratedId(String.valueOf(number.longValue()));
					}
				} catch (Exception e) {
					log.error("Exception Integrated ID " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Integrated ID is invalid.");
					isError = true;
					errorCount++;
				}
				// Active
				try {
					if (inputCurrentRow.getCell(11) != null) {
						boolean isActive = false;
						String active = inputCurrentRow.getCell(11).getStringCellValue();
						if ("yes".equalsIgnoreCase(active)) isActive = true;
						item.setActive(isActive);
					}
				} catch (Exception e) {
					log.error("Exception UOM " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Active is invalid.");
					isError = true;
					errorCount++;
				}
				// Inventory Account
				try {
					if (inputCurrentRow.getCell(12) != null) {
//						String accountName = inputCurrentRow.getCell(12).getStringCellValue();
						String accountName = new DataFormatter().formatCellValue(inputCurrentRow.getCell(12));
						item.setAssetAccountName(accountName);
						
						Long accountId = this.accountRepository.findIdByNameAndDeleted(accountName, false);
						if (accountId == null) {
							errorMessage.append(errorCount + ") Account : " + accountName + " is not found. Please enter the valid Account Name. ");
							log.error("Account : " + accountName + " is not found. Please enter the valid Account Name. ");
							isError = true;
							errorCount++;
						}
						item.setAssetAccountId(accountId);
					}
				} catch (Exception e) {
					log.error("Exception inventory account " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Inventory Account is invalid.");
					isError = true;
					errorCount++;
				}
				// Expense Account
				try {
					if (inputCurrentRow.getCell(13) != null) {
//						String accountName = inputCurrentRow.getCell(13).getStringCellValue();
						String accountName = new DataFormatter().formatCellValue(inputCurrentRow.getCell(13));
						item.setExpenseAccountName(accountName);
						
						Long accountId = this.accountRepository.findIdByNameAndDeleted(accountName, false);
						if (accountId == null) {
							errorMessage.append(errorCount + ") Account : " + accountName + " is not found. Please enter the valid Account Name. ");
							log.error("Account : " + accountName + " is not found. Please enter the valid Account Name. ");
							isError = true;
							errorCount++;
						}
						item.setExpenseAccountId(accountId);
					}
				} catch (Exception e) {
					log.error("Exception expense account " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Expense Account is invalid.");
					isError = true;
					errorCount++;
				}

				Cell cell = inputCurrentRow.createCell(statusColumnNumber);
				if (isError) {
					cell.setCellValue(errorMessage.toString());
					continue;
				} else {
					cell.setCellValue("Imported");
					// TODO : enable to save
					 this.save(item);
				}
				System.out.println(item.toString());
			}
			
			FileOutputStream out = null;
			
			File outputFile = new File("item_export.xlsx");
			try {
				// Writing the workbook
				out = new FileOutputStream(outputFile);
				workbook.write(out);
				log.info("item_export.xlsx written successfully on disk.");
			} catch (Exception e) {
				// Display exceptions along with line number
				// using printStackTrace() method
				e.printStackTrace();
				throw new CustomException("Something went wrong. Please Contact Administrator.");
			} finally {
				out.close();
				workbook.close();
			}
			
			return Files.readAllBytes(outputFile.toPath());
		} catch (IOException e) {
			e.printStackTrace();
			throw new CustomException("Something went wrong. Please Contact Administrator..");
		}
	}
	/**
	 * Get Items based on it's Desc Containing 1st Word
	 * @return Items
	 */
	@Override
	public List<Item> getByDescriptionMatching(String description, Long subsidiaryId) {
		List<Item> items;
		log.info("Get Items by Desc started.");
		items = itemRepository.findByDescriptionContainingIgnoreCaseAndSubsidiaryIdAndIsDeleted(description.split(" ")[0], subsidiaryId, false);
		log.info("Get Items by Desc: " + items);
		return items;
	}

	@Override
	public byte[] downloadTemplate() {
		DefaultResourceLoader loader = new DefaultResourceLoader();
		try {
			File is = loader.getResource("classpath:/templates/item_template.xlsx").getFile();
			return Files.readAllBytes(is.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public List<Item> findDescriptionLike(String description, Long SubsidiaryId){
		List<Item> items = new ArrayList<Item>();
		try {
			items = this.itemRepository.findByDescriptionStartingWithAndSubsidiaryId(description,SubsidiaryId);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new CustomException("Error while fetching the Items based on the description - " + description + ", " + ex.toString());
		}
		return items;
	}
	
	@Override
	public List<GraphData> getdashboard(Long subsidiaryId) {
		List<GraphData> dashboard = new ArrayList<>();
		try {
			Long nonStockableCount = itemRepository.countBySubsidiaryIdAndNatureOfItem(subsidiaryId, "Non-Stockable");
			Long stockableCount = itemRepository.countBySubsidiaryIdAndNatureOfItem(subsidiaryId, "Stockable");
			GraphData nonStoackableData = new GraphData();
			GraphData stockableData = new GraphData();
			nonStoackableData.setCategory("Non-Stockable");
			nonStoackableData.setValue(nonStockableCount);
			stockableData.setCategory("Stockable");
			stockableData.setValue(stockableCount);
			dashboard.add(nonStoackableData);
			dashboard.add(stockableData);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new CustomException("Error while fetching the Items based on the subsidiaryId and NatureOfItem - "
					+ subsidiaryId + ", " + ex.toString());
	}
		return dashboard;
	}
	
	@Override
	public Item findByName(String itemName) {
		Optional<Item> item = this.itemRepository.findByNameAndIsDeleted(itemName, false);
		if (item.isEmpty()) {
			return null;
		}
		return item.get();
	}

}
