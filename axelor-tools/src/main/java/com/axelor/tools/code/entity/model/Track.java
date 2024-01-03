/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
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
package com.axelor.tools.code.entity.model;

import static com.axelor.tools.code.entity.model.Utils.isTrue;

import com.axelor.tools.code.JavaAnnotation;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
public class Track {

  @XmlElement(name = "field")
  private List<TrackField> fields;

  @XmlElement(name = "message")
  private List<TrackMessage> messages;

  @XmlElement(name = "content")
  private List<TrackMessage> contents;

  @XmlAttribute(name = "subscribe")
  private Boolean subscribe;

  @XmlAttribute(name = "replace")
  private Boolean replace;

  @XmlAttribute(name = "files")
  private Boolean files;

  @XmlAttribute(name = "on")
  private TrackEvent on;

  public List<TrackField> getFields() {
    if (fields == null) {
      fields = new ArrayList<>();
    }
    return this.fields;
  }

  public List<TrackMessage> getMessages() {
    if (messages == null) {
      messages = new ArrayList<>();
    }
    return this.messages;
  }

  public List<TrackMessage> getContents() {
    if (contents == null) {
      contents = new ArrayList<>();
    }
    return this.contents;
  }

  public Boolean getSubscribe() {
    return subscribe;
  }

  public void setSubscribe(Boolean value) {
    this.subscribe = value;
  }

  public Boolean getReplace() {
    return replace;
  }

  public void setReplace(Boolean value) {
    this.replace = value;
  }

  public Boolean getFiles() {
    return files;
  }

  public void setFiles(Boolean value) {
    this.files = value;
  }

  public TrackEvent getOn() {
    if (on == null) {
      return TrackEvent.ALWAYS;
    } else {
      return on;
    }
  }

  public void setOn(TrackEvent value) {
    this.on = value;
  }

  public Track merge(Track other) {
    getFields().addAll(other.getFields());
    getMessages().addAll(other.getMessages());
    getContents().addAll(other.getContents());
    if (isTrue(other.replace)) {
      subscribe = other.subscribe;
    }
    return this;
  }

  public Track copyFor(Entity base) {
    Track track = new Track();
    track.merge(this);
    return track;
  }

  public JavaAnnotation toJavaAnnotation() {
    var annon = new JavaAnnotation("com.axelor.db.annotations.Track");

    if (on != null) annon.param("on", "{0:m}", "com.axelor.db.annotations.TrackEvent." + on);

    annon.param("fields", getFields(), TrackField::toJavaAnnotation);
    annon.param("messages", getMessages(), TrackMessage::toJavaAnnotation);
    annon.param("contents", getContents(), TrackMessage::toJavaAnnotation);

    if (isTrue(subscribe)) annon.param("subscribe", "true");
    if (isTrue(files)) annon.param("files", "true");

    return annon;
  }
}
