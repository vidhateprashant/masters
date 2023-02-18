package com.monstarbill.masters.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.monstarbill.masters.commons.ComponentUtility;
import com.monstarbill.masters.commons.CustomException;

import lombok.extern.slf4j.Slf4j;

/**
 * @author prashant
 *
 */
@CrossOrigin(origins= "*", allowedHeaders = "*", maxAge = 4800, allowCredentials = "false" )
@RestController
@RequestMapping("/utility")
@Slf4j
public class CommonUtilityController {

	@Autowired
	private ComponentUtility componentUtility;
	
	@GetMapping("/send-email-by-approver-id")
	public ResponseEntity<?> sendEmailByApproverId(@RequestParam String approverId, @RequestParam String formName) {
		log.info("sendEmailByApproverId started :: " + approverId);
		try {
			componentUtility.sendEmailByApproverId(approverId, formName);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException("Exception while sending the mail for approver : " + approverId);
		}
		log.info("sendEmailByApproverId finished");
		return new ResponseEntity<>(null, HttpStatus.OK);
	}
	
	@GetMapping("/find-is-approval-routing-active")
	public Boolean findIsApprovalRoutingActive(@RequestParam Long subsidiaryId, @RequestParam String formName) {
		log.info("findIsApprovalRoutingActive started :: " + subsidiaryId);
		return componentUtility.findIsApprovalRoutingActive(subsidiaryId, formName);
	}
	
}
