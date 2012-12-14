package com.axelor.test;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.axelor.MyModule;
import com.axelor.test.db.Invoice;
import com.axelor.test.db.Move;
import com.axelor.test.db.MoveLine;
import com.google.common.collect.Lists;
import com.google.inject.persist.Transactional;

@RunWith(GuiceRunner.class)
@GuiceModules({ MyModule.class })
public class InvoiceTest {

	@Before
	@Transactional
	public void createInvoice() {
		Invoice invoice = new Invoice();
		invoice.save();
	}
	
	@Transactional
	public void testErrors() {
		Invoice invoice = Invoice.all().fetchOne();
		
		Move move = new Move();
		move.setMoveLines(Lists.<MoveLine>newArrayList());
		move.setInvoice(invoice);

		MoveLine line1 = new MoveLine();
		line1.setCredit(new BigDecimal("20"));
		line1.setDebit(BigDecimal.ZERO);
		line1.setMove(move);
		line1.setInvoiceReject(invoice);

		move.getMoveLines().add(line1);
		
		invoice.setRejectMoveLine(line1);
		
		invoice.save();

		Assert.assertSame(line1, invoice.getRejectMoveLine());
		Assert.assertSame(line1, move.getMoveLines().get(0));
		
		Assert.assertEquals(new BigDecimal("20"), line1.getCredit());
		Assert.assertEquals(BigDecimal.ZERO, line1.getDebit());

		move.save();
	}

	@Test
	public void test() {
		testErrors();
	}
}
