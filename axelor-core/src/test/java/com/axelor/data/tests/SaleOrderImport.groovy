/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an “AS IS”
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
package com.axelor.data.tests

import org.joda.time.LocalDate;

import com.axelor.contact.db.Contact
import com.axelor.db.mapper.types.JodaAdapter.LocalDateAdapter;
import com.axelor.sale.db.Order;
import com.axelor.sale.db.OrderLine;

class SaleOrderImport {

	/**
	 * This method is called with <code>prepare-context</code> attribute from
	 * the <code>&lt;input&gt;</code> tag. It prepares the global context.
	 * 
	 */
	void createOrder(Map context) {
		
		Order order = new Order()
		order.createDate = new LocalDate()
		order.orderDate = new LocalDate()
		
		context.put("_saleOrder", order)
	}
	
	/**
	 * This method is called with <code>call</code> attribute from the
	 * <code>&lt;input&gt;</code> tag.
	 * 
	 * This method is called for each record being imported.
	 * 
	 * @param bean
	 *            the bean instance created from the imported record
	 * @param values
	 *            the value map that represents the imported data
	 * @return the bean object to persist (in most cases the same bean object)
	 */
	Object updateOrder(Object bean, Map values) {
		
		assert bean instanceof OrderLine
		assert values['_saleOrder'] instanceof Order
		assert values['_customer'] instanceof Contact
		
		Order so = values['_saleOrder']
		OrderLine line = (OrderLine) bean
		Contact cust = values['_customer']
		
		if (so.customer == null)
			so.customer = cust
		
		if (so.items == null)
			so.items = []
			
		so.items.add(line)
		line.order = so
		
		return line
	}
}
