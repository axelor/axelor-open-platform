/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2026 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.test.db;

import com.axelor.auth.db.User;
import com.axelor.db.JpaModel;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "TEST_INVOICE")
public class Invoice extends JpaModel {

  @ManyToOne(
      fetch = FetchType.LAZY,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  private Move move;

  @ManyToOne(
      fetch = FetchType.LAZY,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  private Move oldMove;

  @ManyToOne(
      fetch = FetchType.LAZY,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  private MoveLine rejectMoveLine;

  @ManyToOne(
      fetch = FetchType.LAZY,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  private User saleUser;

  @ManyToMany(
      fetch = FetchType.LAZY,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  private Set<User> members;

  private LocalDate date;

  private LocalDate dueDate;

  public Move getMove() {
    return move;
  }

  public void setMove(Move move) {
    this.move = move;
  }

  public Move getOldMove() {
    return oldMove;
  }

  public void setOldMove(Move oldMove) {
    this.oldMove = oldMove;
  }

  public MoveLine getRejectMoveLine() {
    return rejectMoveLine;
  }

  public void setRejectMoveLine(MoveLine rejectMoveLine) {
    this.rejectMoveLine = rejectMoveLine;
  }

  public User getSaleUser() {
    return saleUser;
  }

  public void setSaleUser(User saleUser) {
    this.saleUser = saleUser;
  }

  public Set<User> getMembers() {
    return members;
  }

  public void setMembers(Set<User> members) {
    this.members = members;
  }

  public void addMember(User item) {
    if (getMembers() == null) {
      setMembers(new HashSet<>());
    }
    getMembers().add(item);
  }

  public void removeMember(User item) {
    if (getMembers() == null) {
      return;
    }
    getMembers().remove(item);
  }

  public void clearMembers() {
    if (getMembers() != null) {
      getMembers().clear();
    }
  }

  public LocalDate getDate() {
    return date;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public void setDueDate(LocalDate dueDate) {
    this.dueDate = dueDate;
  }
}
