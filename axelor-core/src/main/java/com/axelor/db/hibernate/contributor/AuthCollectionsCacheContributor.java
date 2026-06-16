/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.hibernate.contributor;

import java.util.List;
import org.hibernate.boot.ResourceStreamLocator;
import org.hibernate.boot.spi.AdditionalMappingContributions;
import org.hibernate.boot.spi.AdditionalMappingContributor;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.mapping.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enables the second-level collection cache ({@code READ_WRITE}) for a curated set of
 * authentication association collections.
 *
 * <p>Marking an entity {@code @Cacheable} caches its own state but not its {@code @OneToMany} /
 * {@code @ManyToMany} collections; loading such a collection still issues a SQL query against the
 * link table on every access. The auth entities ({@code User}, {@code Group}, {@code Role}, {@code
 * Permission}) are all cacheable, so caching the ids stored by their read-frequently /
 * mutated-rarely association collections avoids that query.
 *
 * <p>This is the equivalent of annotating each collection with {@code @Cache(usage =
 * CacheConcurrencyStrategy.READ_WRITE)}, applied during metadata bootstrap so it does not require a
 * change to the entity code generator. Only owning-side collections whose target entities are
 * themselves {@code @Cacheable} are listed, so the cached ids always resolve from the entity cache.
 *
 * <p>Each collection becomes its own L2 region named after the role (e.g. {@code
 * com.axelor.auth.db.User.permissions}); dedicated region blocks in {@code application.conf} size
 * and tune these to mirror their owning entity.
 *
 * <p>Registered via {@code META-INF/services/org.hibernate.boot.spi.AdditionalMappingContributor}.
 */
public class AuthCollectionsCacheContributor implements AdditionalMappingContributor {

  private static final Logger log = LoggerFactory.getLogger(AuthCollectionsCacheContributor.class);

  private static final String CONCURRENCY_STRATEGY = AccessType.READ_WRITE.getExternalName();

  /**
   * Owning-side collection roles whose target entities are themselves {@code @Cacheable}. Keep this
   * list in sync with the dedicated region blocks in {@code application.conf}.
   */
  static final List<String> CACHED_COLLECTION_ROLES =
      List.of(
          "com.axelor.auth.db.User.roles",
          "com.axelor.auth.db.User.permissions",
          "com.axelor.auth.db.Group.roles",
          "com.axelor.auth.db.Group.permissions",
          "com.axelor.auth.db.Role.permissions");

  @Override
  public String getContributorName() {
    return "axelor-auth-collections-cache";
  }

  @Override
  public void contribute(
      AdditionalMappingContributions contributions,
      InFlightMetadataCollector metadata,
      ResourceStreamLocator resourceStreamLocator,
      MetadataBuildingContext buildingContext) {

    for (final String role : CACHED_COLLECTION_ROLES) {
      final Collection collection = metadata.getCollectionBinding(role);
      if (collection == null) {
        log.error("Cannot enable collection cache: unknown collection role '{}'", role);
        continue;
      }
      // Only owning-side collections may be cached: mutations are tracked through the owning
      // side, so an inverse (mappedBy) collection's cache would not be invalidated and could
      // serve stale ids.
      if (collection.isInverse()) {
        log.error("Cannot enable collection cache: '{}' is an inverse collection", role);
        continue;
      }
      collection.setCacheConcurrencyStrategy(CONCURRENCY_STRATEGY);
      collection.setCacheRegionName(role);
    }
  }
}
