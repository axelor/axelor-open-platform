/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.hibernate.sequence;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import java.lang.reflect.Member;
import java.util.Properties;
import org.hibernate.MappingException;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.factory.spi.CustomIdGeneratorCreationContext;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

/**
 * A custom sequence identifier generator for entities. The main Goal is to resolve the allocation
 * size of the sequence using a hierarchical configuration approach, ensuring flexibility and
 * adaptability based on specific entity or global settings.
 */
public class EntitySequenceIdGenerator extends SequenceStyleGenerator {

  static final int DEFAULT_ALLOCATION_SIZE = 1;
  static final int GLOBAL_DEFAULT_ALLOCATION_SIZE =
      AppSettings.get()
          .getInt(
              AvailableAppSettings.APPLICATION_ENTITY_SEQUENCE_GLOBAL_DEFAULT_ALLOCATION_SIZE, 0);

  private final String sequenceName;
  private final int allocationSize;

  public EntitySequenceIdGenerator(
      EntitySequence annotation, Member member, CustomIdGeneratorCreationContext context) {
    sequenceName = annotation.name();
    allocationSize = determineAllocationSize(annotation);
  }

  /**
   * Determines the allocation size for the sequence generator based on various levels of
   * configuration.<br>
   * The method prioritizes specific settings in the following order:
   *
   * <ul>
   *   <li>1. Explicit allocation size defined in the {@code EntitySequence} annotation.
   *   <li>2. Sequence-specific configuration defined in application settings.
   *   <li>3. Global default allocation size.
   *   <li>4. A fallback value of 1 if no other configurations are defined.
   * </ul>
   *
   * @param annotation the {@code EntitySequence} annotation containing the entity sequence
   *     configuration.
   * @return the resolved allocation size to be used for the sequence generator.
   */
  private int determineAllocationSize(EntitySequence annotation) {
    if (annotation.allocationSize() > 0) {
      // Explicit allocationSize in annotation
      return annotation.allocationSize();
    }

    int sequenceAllocationSize =
        AppSettings.get()
            .getInt(
                AvailableAppSettings.APPLICATION_ENTITY_SEQUENCE_PATH
                    + sequenceName
                    + ".allocation_size",
                0);
    if (sequenceAllocationSize > 0) {
      // Sequence-specific config
      return sequenceAllocationSize;
    }

    if (GLOBAL_DEFAULT_ALLOCATION_SIZE > 0) {
      // Global default
      return GLOBAL_DEFAULT_ALLOCATION_SIZE;
    }

    return DEFAULT_ALLOCATION_SIZE;
  }

  @Override
  public void configure(Type type, Properties parameters, ServiceRegistry serviceRegistry)
      throws MappingException {
    parameters.setProperty(SEQUENCE_PARAM, sequenceName);
    parameters.setProperty(INCREMENT_PARAM, String.valueOf(allocationSize));

    super.configure(type, parameters, serviceRegistry);
  }
}
