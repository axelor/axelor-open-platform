/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
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
