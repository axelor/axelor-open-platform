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
package com.axelor.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.JpaTest;
import com.axelor.auth.AuthUtils;
import com.axelor.test.db.StrategyClass;
import com.axelor.test.db.StrategyClassChild;
import com.axelor.test.db.StrategyJoined;
import com.axelor.test.db.StrategyJoinedChild;
import com.axelor.test.db.StrategySingle;
import com.axelor.test.db.StrategySingleChild;
import com.google.inject.persist.Transactional;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import javax.persistence.PersistenceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Query Mass Update Tests")
class QueryMassUpdateTest<M extends Model, C extends M> extends JpaTest {

  private JpaRepository<M> parentRepository;
  private JpaRepository<C> childRepository;

  static Stream<Arguments> stream() {
    return Stream.of(
        Arguments.of(StrategySingle.class, StrategySingleChild.class, "Single Table"),
        Arguments.of(StrategyJoined.class, StrategyJoinedChild.class, "Joined"),
        Arguments.of(StrategyClass.class, StrategyClassChild.class, "Table Per Class"));
  }

  @ParameterizedTest(name = "Bulk Update {2}")
  @MethodSource("stream")
  @DisplayName("Bulk update with inheritance strategy")
  void testMassUpdate(Class<M> parentClass, Class<C> childClass, String strategyName) {
    parentRepository = JpaRepository.of(parentClass);
    childRepository = JpaRepository.of(childClass);

    var user = AuthUtils.getUser();
    assertNotNull(user);

    var numParentEntities = 10;
    var numChildEntities = 5;
    var numTotalEntities = numParentEntities + numChildEntities;

    inTransaction(
        () -> {
          parentRepository.all().delete();
          childRepository.all().delete();

          for (var i = 0; i < numParentEntities; ++i) {
            var entity = parentRepository.create(Map.of("myString", i));
            parentRepository.save(entity);
          }

          for (var i = 0; i < numChildEntities; ++i) {
            var childEntity = childRepository.create(Map.of("myStringChild", i));
            childRepository.save(childEntity);
          }
        });

    assertEquals(numTotalEntities, parentRepository.all().count());
    assertEquals(numChildEntities, childRepository.all().count());

    if (EntityHelper.isSafeForBulkUpdate(parentClass)) {
      inTransaction(
          () ->
              assertEquals(
                  numTotalEntities,
                  parentRepository.all().update(Map.of("myString", "100"), user)));

      var results = parentRepository.all().select("myString").fetch(-1, -1);
      assertEquals(numTotalEntities, results.size());
      assertTrue(results.stream().allMatch(m -> Objects.equals(m.get("myString"), "100")));

      var childResults = childRepository.all().select("myString", "myStringChild").fetch(-1, -1);
      assertEquals(numChildEntities, childResults.size());
      assertTrue(childResults.stream().allMatch(m -> Objects.equals(m.get("myString"), "100")));
    } else {
      var query = parentRepository.all();
      Map<String, Object> values = Map.of("myString", "100");
      inTransaction(
          () -> assertThrows(PersistenceException.class, () -> query.update(values, user)));
    }

    if (EntityHelper.isSafeForBulkUpdate(childClass)) {
      inTransaction(
          () ->
              assertEquals(
                  numChildEntities,
                  childRepository
                      .all()
                      .update(Map.of("myString", "200", "myStringChild", "300"), user)));

      var childResults = childRepository.all().select("myString", "myStringChild").fetch(-1, -1);
      assertEquals(numChildEntities, childResults.size());
      assertTrue(childResults.stream().allMatch(m -> Objects.equals(m.get("myString"), "200")));
      assertTrue(
          childResults.stream().allMatch(m -> Objects.equals(m.get("myStringChild"), "300")));
    } else {
      var query = childRepository.all();
      Map<String, Object> values = Map.of("myString", "200", "myStringChild", "300");
      inTransaction(
          () -> assertThrows(PersistenceException.class, () -> query.update(values, user)));
    }
  }

  @BeforeEach
  @Transactional
  void ensureAuth() {
    ensureAuth("admin", "admin");
  }

  @AfterEach
  @Transactional
  void cleanUp() {
    parentRepository.all().delete();
    childRepository.all().delete();
  }
}
