/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
