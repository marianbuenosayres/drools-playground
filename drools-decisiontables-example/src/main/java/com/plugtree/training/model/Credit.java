package com.plugtree.training.model;

import com.plugtree.training.enums.CreditStatus;

public class Credit {
	
	private CreditStatus status;
	
	public Credit(CreditStatus status) {
		this.status = status;
	}

	public void setStatus(CreditStatus status) {
		this.status = status;
	}

	public CreditStatus getStatus() {
		return status;
	}

}
