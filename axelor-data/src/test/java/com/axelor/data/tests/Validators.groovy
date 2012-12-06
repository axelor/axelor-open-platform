package com.axelor.data.tests

import com.google.inject.Inject
import com.google.inject.Injector

import com.axelor.db.JPA
import com.axelor.contact.db.Contact
import com.axelor.sale.service.SaleOrderService
import com.axelor.sale.db.Order

class Validators {

	@Inject
	SaleOrderService soService

	Object validateSaleOrder(Object bean, Map context) {
		assert bean instanceof Order
		Order so = (Order) bean

		soService.validate(so)

		println("Date: $so.orderDate")
		println("Customer: $so.customer.firstName $so.customer.lastName")
		println("Items: $so.items.size")

		int count = JPA.all(Contact.class).count()
		assert count > 1

		return bean
	}
}
