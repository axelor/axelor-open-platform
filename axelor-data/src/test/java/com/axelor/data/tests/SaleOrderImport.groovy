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
