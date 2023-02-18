package com.monstarbill.masters.commons;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.monstarbill.masters.enums.FormNames;
import com.monstarbill.masters.models.ApprovalRoutingPreference;
import com.monstarbill.masters.repository.ApprovalRoutingPreferenceRepository;
import com.monstarbill.masters.service.EmployeeService;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author Prashant
 *
 */
@Slf4j
@Component
public class ComponentUtility {
	
	@Autowired
	private EmployeeService employeeService;
	
	@Autowired
	private ApprovalRoutingPreferenceRepository approvalRoutingPreferenceRepository;
	
	@Async
	public void sendEmailByApproverId(String approverIdStr, String formName) throws Exception {
		if (StringUtils.isNotEmpty(approverIdStr)) {
			String approverMail = this.employeeService.getEmailByEmployeeId(Long.parseLong(approverIdStr));
			if (StringUtils.isNotEmpty(approverMail)) {
				String subject = "[Notification] " + formName + " Approval Request";
				String body = prepareBodyByApprovalForm(formName);
				
				CommonUtils.sendMail(approverMail, null, subject, body);
			} else {
				log.error("Approvers email is not found.");
			}
		} else {
			log.error("No approver is found.");
		}
	}

	// Prepares the body of the email as per approver form
	private String prepareBodyByApprovalForm(String formName) {
		// Header
		StringBuilder body = new StringBuilder("Hi, <br><br>");
		
		// Main body content
		if (FormNames.PR.getFormName().equals(formName)) {
			body.append("You have new request to approve to PR.");
		}
		if (FormNames.PO.getFormName().equals(formName)) {
			body.append("You have new request to approve to PO.");
		}
		if (FormNames.SUPPLIER.getFormName().equals(formName)) {
			body.append("You have new request to approve to Supplier.");
		}
		if (FormNames.RTV.getFormName().equals(formName)) {
			body.append("You have new request to approve to RTV.");
		}
		if (FormNames.ADVANCE_PAYMENT.getFormName().equals(formName)) {
			body.append("You have new request to approve to Advance Payment.");
		}

		// Footer
		body.append("<br><br> <b>Regards,</b><br>").append("Team MonstarBill ");
		
		return body.toString();
	}
	
	/**
	 * Find the approval routing is active or not based on the subsidiary Id
	 * @param subsidiaryId
	 * @return
	 */
	public boolean findIsApprovalRoutingActive(Long subsidiaryId, String formName) {
		Optional<ApprovalRoutingPreference> approvalRoutingPreference = this.approvalRoutingPreferenceRepository.findIsRoutingActiveBySubsidiaryAndFormName(subsidiaryId, formName);
		if (approvalRoutingPreference.isPresent()) {
			log.info("Approval Routing is Found against : " + formName + ", And subsidiary :: " + subsidiaryId);
			return approvalRoutingPreference.get().isRoutingActive();
		}
		log.info("Approval Routing is NOT Found against :: " + formName + ", And subsidiary :: " + subsidiaryId);
		return false;
	}
	
}
