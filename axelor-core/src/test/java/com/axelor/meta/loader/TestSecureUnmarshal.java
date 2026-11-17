/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.UnmarshalException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests of {@link SecureUnmarshal}, which every JAXB entry point goes through.
 *
 * <p>The external-entity tests point the entity at a real file holding a marker: a parser that
 * resolves the entity puts the marker in the result, a safe one fails before reading the file.
 */
class TestSecureUnmarshal {

  public enum Color {
    RED
  }

  @XmlRootElement(name = "root")
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Root {
    @XmlAttribute public Color color;
    @XmlElement public String value;
  }

  private static final String MARKER = "secure-unmarshal-marker";

  private static Unmarshaller unmarshaller;

  private String withEntity;

  /** Refuses binding errors the way {@code XMLWorkflows} does, so they surface as exceptions. */
  @BeforeAll
  static void createUnmarshaller() throws JAXBException {
    unmarshaller = JAXBContext.newInstance(Root.class).createUnmarshaller();
    unmarshaller.setEventHandler(event -> event.getSeverity() == ValidationEvent.WARNING);
  }

  @BeforeEach
  void writeMarker(@TempDir Path dir) throws IOException {
    var file = Files.writeString(dir.resolve("marker.txt"), MARKER);
    withEntity =
        "<?xml version=\"1.0\"?><!DOCTYPE root [<!ENTITY xxe SYSTEM \"%s\">]>"
                .formatted(file.toUri())
            + "<root><value>&xxe;</value></root>";
  }

  /**
   * The {@link java.io.Reader} overload must refuse an external entity: the unmarshalling fails and
   * the file's content never reaches the result. This is the path of XML strings written by users
   * (workflows, queries, views).
   */
  @Test
  void reader_refuses_external_entities() {
    var error =
        assertThrows(
            JAXBException.class,
            () -> SecureUnmarshal.unmarshal(unmarshaller, new StringReader(withEntity)));
    assertMarkerNotLeaked(error);
  }

  /**
   * The {@link java.io.InputStream} overload must refuse an external entity too. This is the path
   * of XML files loaded from modules.
   */
  @Test
  void stream_refuses_external_entities() {
    var error =
        assertThrows(
            JAXBException.class, () -> SecureUnmarshal.unmarshal(unmarshaller, stream(withEntity)));
    assertMarkerNotLeaked(error);
  }

  /**
   * Hardening must not break normal documents: both overloads still bind elements, attributes and
   * enum values.
   */
  @Test
  void reads_a_plain_document() throws JAXBException {
    var xml = "<root color=\"RED\"><value>ok</value></root>";

    var fromReader = (Root) SecureUnmarshal.unmarshal(unmarshaller, new StringReader(xml));
    assertEquals("ok", fromReader.value);
    assertEquals(Color.RED, fromReader.color);

    var fromStream = (Root) SecureUnmarshal.unmarshal(unmarshaller, stream(xml));
    assertEquals("ok", fromStream.value);
    assertEquals(Color.RED, fromStream.color);
  }

  /**
   * A value that can't be bound must come out as an {@link UnmarshalException}, as with a plain
   * {@code unmarshaller.unmarshal(reader)}.
   *
   * <p>MOXy's StAX entry point, which {@code SecureUnmarshal} uses, lets such errors escape as raw
   * EclipseLink exceptions. {@link XMLWorkflows#fromXML} catches {@code UnmarshalException} to show
   * the user which value was refused, so the wrapping is part of the contract.
   */
  @Test
  void binding_error_comes_out_as_unmarshal_exception() {
    var xml = "<root color=\"BLUE\"><value>x</value></root>";

    var error =
        assertThrows(
            UnmarshalException.class,
            () -> SecureUnmarshal.unmarshal(unmarshaller, new StringReader(xml)));
    assertTrue(
        String.valueOf(error.getLinkedException()).contains("BLUE"),
        () -> "the cause must name the refused value, got: " + error.getLinkedException());
  }

  private static ByteArrayInputStream stream(String xml) {
    return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
  }

  private static void assertMarkerNotLeaked(Throwable error) {
    for (Throwable t = error; t != null; t = t.getCause()) {
      var message = String.valueOf(t.getMessage());
      assertFalse(message.contains(MARKER), () -> "external entity was resolved: " + message);
    }
  }
}
