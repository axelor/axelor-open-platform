package com.axelor.test.db;

import com.axelor.db.annotations.Widget;

public enum EnumStatus {

	DRAFT,
	
	OPEN,

	@Widget(title = "Close")
	CLOSED
}
