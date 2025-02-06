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

import com.axelor.db.annotations.Track;
import com.axelor.db.annotations.TrackEvent;
import com.axelor.db.annotations.TrackMessage;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/** This class is a straightforward implementation of the {@link Track} annotation */
public class ModelTracking {

  private final List<FieldTracking> fields;
  private final List<TrackMessage> messages;
  private final List<TrackMessage> contents;
  private final boolean subscribe;
  private final boolean files;
  private final TrackEvent on;

  private ModelTracking(
      List<FieldTracking> fields,
      List<TrackMessage> messages,
      List<TrackMessage> contents,
      boolean subscribe,
      boolean files,
      TrackEvent on) {
    this.fields = fields;
    this.messages = messages;
    this.contents = contents;
    this.subscribe = subscribe;
    this.files = files;
    this.on = on;
  }

  public static ModelTracking create(Track track, List<FieldTracking> customFields) {
    if (track == null) {
      return new ModelTracking(
          customFields,
          Collections.emptyList(),
          Collections.emptyList(),
          false,
          false,
          TrackEvent.ALWAYS);
    }

    return new ModelTracking(
        Stream.concat(Arrays.stream(track.fields()).map(FieldTracking::new), customFields.stream())
            .toList(),
        Arrays.asList(track.messages()),
        Arrays.asList(track.contents()),
        track.subscribe(),
        track.files(),
        track.on());
  }

  public List<FieldTracking> getFields() {
    return fields;
  }

  public List<TrackMessage> getMessages() {
    return messages;
  }

  public List<TrackMessage> getContents() {
    return contents;
  }

  public boolean isSubscribe() {
    return subscribe;
  }

  public boolean isFiles() {
    return files;
  }

  public TrackEvent getOn() {
    return on;
  }
}
