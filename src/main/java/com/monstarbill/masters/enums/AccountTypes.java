package com.monstarbill.masters.enums;

/**
 * Account types
 * @author prashant
 * 25-10-2022
 */
public enum AccountTypes {

	// enum fields
	BANK("Bank"),
	EXPENSE("Expenses"),
	ASSETS("Assets"),
	LIABILITY("Liability"),
	PRE_PAYMENT("Prepayment");
	
	// constructor
    private AccountTypes(final String accountType) {
        this.accountType = accountType;
    }
 
    // internal state
    private String accountType;
 
    public String getAccountType() {
        return accountType;
    }
    
}
