/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.tracking;

import com.axelor.db.annotations.TrackEvent;
import com.axelor.db.annotations.TrackField;
import com.axelor.meta.db.MetaJsonField;

/** This class is a straightforward implementation of the {@link TrackField} annotation */
public class FieldTracking {

  private final String fieldName;
  private final String condition;
  private final TrackEvent on;

  private final boolean isCustomField;
  private final String jsonFieldName;

  public FieldTracking(TrackField trackField) {
    this.fieldName = trackField.name();
    this.condition = trackField.condition();
    this.on = trackField.on();
    this.isCustomField = false;
    this.jsonFieldName = null;
  }

  public FieldTracking(MetaJsonField jsonField) {
    this.fieldName = jsonField.getName();
    this.condition = jsonField.getTrackCondition();
    this.on =
        jsonField.getTrackEvent() != null
            ? TrackEvent.valueOf(jsonField.getTrackEvent())
            : TrackEvent.DEFAULT;
    this.isCustomField = true;
    this.jsonFieldName = jsonField.getModelField();
  }

  public String getFieldName() {
    return fieldName;
  }

  public String getCondition() {
    return condition;
  }

  public TrackEvent getOn() {
    return on;
  }

  public boolean isCustomField() {
    return isCustomField;
  }

  public String getJsonFieldName() {
    return jsonFieldName;
  }

  public String getName() {
    return isCustomField ? "%s.%s".formatted(jsonFieldName, fieldName) : fieldName;
  }
}
