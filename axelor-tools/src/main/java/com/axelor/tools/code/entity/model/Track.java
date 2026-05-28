/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.tools.code.entity.model;

import static com.axelor.tools.code.entity.model.Utils.isTrue;

import com.axelor.tools.code.JavaAnnotation;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

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
