package com.monstarbill.masters.feignclient;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.monstarbill.masters.commons.CustomException;
import com.monstarbill.masters.models.Subsidiary;
import com.monstarbill.masters.payload.response.UserValidationRequest;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@FeignClient(name = "setup-ws")
public interface SetupServiceClient {
	
	Logger logger = org.slf4j.LoggerFactory.getLogger(SetupServiceClient.class);

	@GetMapping("/setup/status/comm")
	@Retry(name = "setup-ws")
	@CircuitBreaker(name = "setup-ws", fallbackMethod = "getStatusListFallback")
	public List<String> getStatusList(@RequestParam("id") int id, @RequestParam("name") String name);

	default List<String> getStatusListFallback(int id, String name, Throwable exception) {
		logger.error("ID : " + id);
		logger.error("Name : " + name);
		logger.error("Exception : " + exception.getLocalizedMessage());
		return new ArrayList<String>();
	}
	
	/**
	 * get subsidiary by id
	 * @param id
	 * @return
	 */
	@GetMapping("/subsidiary/get")
	@Retry(name = "setup-ws")
	@CircuitBreaker(name = "setup-ws", fallbackMethod = "getsubsidiaryByIdFallback")
	public Subsidiary getsubsidiaryById(@RequestParam("id") Long id);
	
	default Subsidiary getsubsidiaryByIdFallback(Long id, Throwable exception) {
		logger.error("Subsidiary Id : " + id + ", Subsidiary is not found exception.");
		logger.error("Exception : " + exception.getLocalizedMessage());
		return null;
	}
	
	/**
	 * get subsidiary id by subsidiary name
	 * @param name
	 * @return
	 */
	@GetMapping("/subsidiary/get-subsidiary-id-by-name")
	@Retry(name = "setup-ws")
	@CircuitBreaker(name = "setup-ws", fallbackMethod = "getSubsidiaryIdByNameFallback")
	public Long getSubsidiaryIdByName(@RequestParam("name") String name);
	
	default Long getSubsidiaryIdByNameFallback(String name, Throwable exception) {
		logger.error("Subsidiary Name : " + name + ", Subsidiary is not found exception.");
		logger.error("Exception : " + exception.getLocalizedMessage());
		return null;
	}
	
	@GetMapping("/subsidiary/get-logged-username")
	@Retry(name = "setup-ws")
	@CircuitBreaker(name = "setup-ws", fallbackMethod = "getLoggedInUsernameFallback")
	public String getLoggedInUsername();
	
	default String getLoggedInUsernameFallback(Throwable exception) {
		logger.error("Logged username not found exception.");
		logger.error("Exception : " + exception.getLocalizedMessage());
		return null;
	}

	/**
	 * Find preference number by subsidiary & Master name
	 * @param subsidiaryId
	 * @param masterName
	 * @return
	 */
	@GetMapping("/preference/get-preference-number")
	@Retry(name = "setup-ws")
	@CircuitBreaker(name = "setup-ws", fallbackMethod = "getPreferenceNumberFallback")
	public String getPreferenceNumber(@RequestParam("subsidiaryId") Long subsidiaryId, @RequestParam("masterName") String masterName);
	
	default String getPreferenceNumberFallback(Long subsidiaryId, String masterName, Throwable exception) {
		logger.error("subsidiary Id : " + subsidiaryId + ", Master Name : " + masterName + ", Preference is not found exception.");
		logger.error("Exception : " + exception.getLocalizedMessage());
		return null;
	}

	/**
	 * save the credentials to login by the Supplier/employee
	 * @param user
	 */
	@GetMapping("/user/save-user-credentials")
	// @Retry(name = "setup-ws")
	@CircuitBreaker(name = "setup-ws", fallbackMethod = "saveUserCredentialsFallback")
	public void saveUserCredentials(UserValidationRequest user);
	
	default void saveUserCredentialsFallback(UserValidationRequest user, Throwable exception) {
		logger.error("user : " + user.toString() + ", Save user credentials failed due to exception.");
		logger.error("Exception : " + exception.getLocalizedMessage());
		throw new CustomException("Error while saving the Credentials.");
	}

	/**
	 * delete the credentials to login by the Supplier/employee
	 * @param accessMail
	 */
	@GetMapping("/user/delete-user-credentials")
	@Retry(name = "setup-ws")
	@CircuitBreaker(name = "setup-ws", fallbackMethod = "deleteUserCredentialsFallback")
	public void deleteUserCredentials(String accessMail);
	
	default void deleteUserCredentialsFallback(String accessMail, Throwable exception) {
		logger.error("user access Mail : " + accessMail + ", Delete user credentials failed due to exception.");
		logger.error("Exception : " + exception.getLocalizedMessage());
		throw new CustomException("Error while deleted the user Credentials.");
	}
}