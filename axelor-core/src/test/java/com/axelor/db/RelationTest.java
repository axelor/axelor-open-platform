/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2014 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.db;

import java.math.BigDecimal;

import javax.persistence.PersistenceException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.axelor.JpaTest;
import com.axelor.test.db.Invoice;
import com.axelor.test.db.Move;
import com.axelor.test.db.MoveLine;
import com.google.common.collect.Lists;
import com.google.inject.persist.Transactional;

public class RelationTest extends JpaTest {

	@Before
	@Transactional
	public void createInvoice() {
		Invoice invoice = new Invoice();
		invoice.save();
	}
	
	@Transactional
	protected void testErrors() {
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
		invoice.setMove(move);
		
		invoice.save();

		Assert.assertSame(line1, invoice.getRejectMoveLine());
		Assert.assertSame(line1, move.getMoveLines().get(0));
		
		Assert.assertEquals(new BigDecimal("20"), line1.getCredit());
		Assert.assertEquals(BigDecimal.ZERO, line1.getDebit());

		move.save();
	}
	
	@Transactional
	protected void testRelations() {
		Move move = Move.all().fetchOne();
		MoveLine line = MoveLine.all().fetchOne();
		Invoice invoice = Invoice.all().fetchOne();
		
		Assert.assertSame(move, line.getMove());
		Assert.assertSame(move, invoice.getMove());
		Assert.assertSame(line, invoice.getRejectMoveLine());
		
		Assert.assertSame(line, move.getMoveLines().get(0));
	}
	
	@Transactional
	protected void testRemoveCollection() {
		Move move = Move.all().fetchOne();
		
		// the moveLines is exclusive o2m field, so clear would delete all
		// the move lines but one of move line is referenced outside so the
		// entity manager will throws an exception
		move.getMoveLines().clear();
	}
	
	@Transactional
	protected void testRemoveCollectionImproper() {
		Move move = Move.all().fetchOne();
		Invoice invoice = Invoice.all().fetchOne();
		
		// before trying to clear moveLines, we remove invoice itself that
		// refers one of the move line but the invoice again is referenced
		// in a move
		invoice.remove();
		
		// invoice is removed but still referenced in move so on transaction
		//completion entity manager will throw an exception
		move.getMoveLines().clear();
	}

	@Transactional
	protected void testRemoveCollectionProper() {
		Move move = Move.all().fetchOne();
		Invoice invoice = Invoice.all().fetchOne();

		// clear the reference
		invoice.setRejectMoveLine(null);
		
		// then clear the collection
		move.getMoveLines().clear();
		
		// or
		
		// invoice.remove();
		// move.setInvoice(null);
		// move.getMoveLines().clear();
	}
	
	@Test
	public void test() {
		testErrors();
		testRelations();
		
		try {
			testRemoveCollection();
			Assert.fail();
		} catch (PersistenceException e) {}
		
		try {
			testRemoveCollectionImproper();
			Assert.fail();
		} catch (PersistenceException e) {}
		
		testRemoveCollectionProper();
	}
}
