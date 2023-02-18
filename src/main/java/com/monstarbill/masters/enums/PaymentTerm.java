package com.monstarbill.masters.enums;

import java.util.Arrays;

/**
 * Purchase order - Payment terms
 * @author Prashant
 * 06-08-2022
 */
public enum PaymentTerm {

	DAYS_30("30 Days"),
	DAYS_60("60 Days");
	
    private PaymentTerm(final String paymentTerm) {
        this.paymentTerm = paymentTerm;
    }
 
    private String paymentTerm;
 
    public String getPaymentTerm() {
        return paymentTerm;
    }
    
    public static PaymentTerm findByAbbr(final String abbr){
        return Arrays.stream(values()).filter(value -> value.getPaymentTerm().equals(abbr)).findFirst().orElse(null);
    }
    
}
