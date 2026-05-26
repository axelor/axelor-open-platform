/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.axelor.JpaTest;
import com.axelor.test.db.Product;
import com.axelor.test.db.ProductConfig;
import com.axelor.test.db.ProductConfigRequiredProduct;
import com.google.inject.persist.Transactional;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;

/**
 * Removal behavior for bidirectional {@code @OneToOne} relations.
 *
 * <p>Topology: {@code Product} is the inverse side; {@code ProductConfig} / {@code
 * ProductConfigRequiredProduct} are the owning sides holding the foreign key back to {@code
 * Product}. The owning side declares {@code cascade = {PERSIST, MERGE}}, which is what makes
 * deleting the inverse side tricky: on flush the cascade can re-persist (resurrect) the entity being
 * deleted unless its link to the owning side is broken first.
 *
 * <p>{@code JpaRepository#detachChildren} breaks that link in one of two ways: null the owning-side
 * back-reference when it is optional, or detach the loaded owning-side entity when the back-reference
 * is required (and therefore cannot be nulled).
 */
class OneToOneRemoveTest extends JpaTest {

  private JpaRepository<Product> productRepo = JpaRepository.of(Product.class);
  private JpaRepository<ProductConfig> configRepo = JpaRepository.of(ProductConfig.class);

  /**
   * Removing the owning side (the one holding the FK) is the trivial case: there is no inverse link
   * to resurrect it, so the config is deleted and the product it pointed to is left untouched.
   */
  @Test
  void testRemoveOwning() {
    var product = createProductWithConfig();
    var productId = product.getId();
    var configId = product.getConfig().getId();

    flush();

    JPA.runInTransaction(
        () -> {
          var foundConfig = configRepo.find(configId);
          assertNotNull(foundConfig);
          configRepo.remove(foundConfig);
        });

    flush();

    assertNotNull(productRepo.find(productId));
    assertNull(configRepo.find(configId));
  }

  /**
   * Removing the inverse side with an optional back-reference: the owning-side config can be kept,
   * so {@code detachChildren} nulls {@code config.product}. That clears the link, the product
   * deletes cleanly, and the config survives (no orphanRemoval). This is the case the unconditional
   * detach regressed — detaching the config first prevented the null from being flushed.
   */
  @Test
  void testRemoveNonOwning() {
    var product = createProductWithConfig();
    var productId = product.getId();
    var configId = product.getConfig().getId();

    flush();

    JPA.runInTransaction(
        () -> {
          var foundProduct = productRepo.find(productId);
          assertNotNull(foundProduct);
          productRepo.remove(foundProduct);
        });

    flush();

    assertNull(productRepo.find(productId));
    assertNotNull(configRepo.find(configId)); // No orphanRemoval
  }

  /**
   * Removing the inverse side with a required back-reference: {@code config.requiredProduct} can't
   * be nulled, so {@code detachChildren} detaches the config to stop cascade=PERSIST from
   * resurrecting the product. The delete then proceeds and the database rejects it with a
   * foreign-key violation (instead of silently succeeding). This is the original bug being fixed.
   */
  @Test
  void testRemoveNonOwningRequired() {
    var product = createProductWithConfigRequiredProduct();
    var productId = product.getId();

    flush();

    assertThrows(
        PersistenceException.class,
        () -> {
          JPA.runInTransaction(
              () -> {
                var foundProduct = productRepo.find(productId);
                assertNotNull(foundProduct);
                productRepo.remove(foundProduct);
              });
        });

    assertNotNull(productRepo.find(productId));
  }

  @Transactional
  Product createProductWithConfig() {
    var config = new ProductConfig();
    var product = new Product();
    product.setConfig(config);
    return productRepo.save(product);
  }

  @Transactional
  Product createProductWithConfigRequiredProduct() {
    var config = new ProductConfigRequiredProduct();
    var product = new Product();
    product.setConfigRequiredProduct(config);
    return productRepo.save(product);
  }

  @Transactional
  void flush() {
    JPA.flush();
    JPA.clear();
  }
}
