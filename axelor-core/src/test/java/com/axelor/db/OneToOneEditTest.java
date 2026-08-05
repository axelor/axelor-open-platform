/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.axelor.JpaTest;
import com.axelor.test.db.Product;
import com.axelor.test.db.ProductAlias;
import com.axelor.test.db.ProductConfig;
import com.google.inject.persist.Transactional;
import jakarta.persistence.OptimisticLockException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests that {@link JPA#edit(Class, Map)} wires both sides of a bidirectional {@code @OneToOne}.
 *
 * <p>{@code ProductConfig.product} is the owning side and {@code Product.config} is the inverse
 * side. {@link com.axelor.db.mapper.Property#setAssociation(Object, Object)} wires relations only
 * when called on the property declaring {@code mappedBy}. Editing the owning side from a map
 * previously left the inverse side null, breaking computed fields like {@code @NameColumn} on the
 * target.
 *
 * @see com.axelor.db.audit.AuditTrackingOneToOneTest
 */
class OneToOneEditTest extends JpaTest {

  private final JpaRepository<Product> productRepo = JpaRepository.of(Product.class);
  private final JpaRepository<ProductConfig> configRepo = JpaRepository.of(ProductConfig.class);

  /** Editing the owning side of an existing target must wire the inverse reference. */
  @Test
  void testEditWiresInverseSideOfExistingTarget() {
    var productId = createProduct();
    flushAndClear();

    inTransaction(
        () -> {
          var product = productRepo.find(productId);
          assertNull(product.getConfig(), "fixture should start unlinked");

          var config = JPA.edit(ProductConfig.class, reference("product", productId));

          assertSame(product, config.getProduct());
          assertSame(config, product.getConfig(), "inverse side was not wired");
        });
  }

  /** Editing with an unsaved target must set the back-reference before cascade persistence. */
  @Test
  void testEditWiresInverseSideOfNewTarget() {
    inTransaction(
        () -> {
          var values = new HashMap<String, Object>();
          values.put("product", new HashMap<String, Object>());

          var config = JPA.edit(ProductConfig.class, values);
          var product = config.getProduct();

          assertNotNull(product);
          assertNull(product.getId(), "target should still be unsaved");
          assertSame(config, product.getConfig(), "inverse side was not wired");
        });
  }

  /** Inverse wiring must match only properties mapped by the edited field name. */
  @Test
  void testEditLeavesUnrelatedInverseSideAlone() {
    var productId = createProduct();
    flushAndClear();

    inTransaction(
        () -> {
          var config = JPA.edit(ProductConfig.class, reference("product", productId));
          assertNull(config.getProduct().getConfigRequiredProduct());
        });
  }

  /**
   * Inverse lookup must match candidate properties by target type in addition to {@code mappedBy}.
   *
   * <p>When two entities own a relation to the same target using identical field names, lookup must
   * not select an inverse property mapped to the other entity type.
   */
  @Test
  void testEditDoesNotWireTheInverseOfAnotherOwner() {
    var productId = createProduct();
    var configId = createConfig(productId);
    flushAndClear();

    inTransaction(
        () -> {
          var alias = JPA.edit(ProductAlias.class, reference("product", productId));

          assertEquals(productId, alias.getProduct().getId());
          assertNotNull(alias.getProduct().getConfig(), "the real inverse was cleared");
          assertEquals(
              configId,
              alias.getProduct().getConfig().getId(),
              "the inverse of a different owner was wired");
        });
  }

  /**
   * Wiring the inverse side must preserve optimistic concurrency checks.
   *
   * <p>If inverse wiring runs before {@code Mapper#set}, the old value matches the new value,
   * causing {@code Property#valueChanged} to report false and skip {@code checkVersion}.
   */
  @Test
  void testEditStillChecksVersionWhenOnlyOneToOneChanges() {
    var productId = createProduct();
    var otherProductId = createProduct();
    var configId = createConfig(productId);
    flushAndClear();

    inTransaction(
        () -> {
          var values = reference("product", otherProductId);
          values.put("id", configId);
          values.put("version", configRepo.find(configId).getVersion() + 999);

          assertThrows(OptimisticLockException.class, () -> JPA.edit(ProductConfig.class, values));
        });
  }

  /** The wired relation persists correctly to the database. */
  @Test
  void testEditedRelationIsPersisted() {
    var productId = createProduct();
    flushAndClear();

    var configId = saveEdited(productId);
    flushAndClear();

    inTransaction(
        () -> {
          var product = productRepo.find(productId);
          assertNotNull(product.getConfig());
          assertEquals(configId, product.getConfig().getId());
        });
  }

  private Map<String, Object> reference(String name, Long targetId) {
    var target = new HashMap<String, Object>();
    target.put("id", targetId);
    var values = new HashMap<String, Object>();
    values.put(name, target);
    return values;
  }

  @Transactional
  Long saveEdited(Long productId) {
    var config = JPA.edit(ProductConfig.class, reference("product", productId));
    return configRepo.save(config).getId();
  }

  @Transactional
  Long createProduct() {
    return productRepo.save(new Product()).getId();
  }

  @Transactional
  Long createConfig(Long productId) {
    var config = new ProductConfig();
    config.setProduct(productRepo.find(productId));
    return configRepo.save(config).getId();
  }

  @Transactional
  void flushAndClear() {
    JPA.flush();
    JPA.clear();
  }
}
