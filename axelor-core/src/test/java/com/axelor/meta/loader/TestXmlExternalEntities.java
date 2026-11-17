/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.loader;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.common.XMLUtils;
import com.axelor.meta.schema.ObjectViews;
import java.io.StringReader;
import java.net.URI;
import java.nio.file.Path;
import java.util.UUID;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

/**
 * XML coming from users or remote services must never make the server read a file or call a URL
 * named by the document itself (XXE: local file disclosure, SSRF).
 *
 * <p>Each test feeds one of AOP's XML entry points a document whose external entity (or external
 * DTD) points at a file that doesn't exist. A parser that tries to resolve it has to open that file
 * and fails with an error naming it; a safe parser never opens it.
 */
class TestXmlExternalEntities {

  private String probe;
  private URI probeUri;

  /** Gives each test its own missing file, so no error message can match by accident. */
  @BeforeEach
  void setUp(@TempDir Path dir) {
    probe = "xxe-probe-" + UUID.randomUUID() + ".txt";
    probeUri = dir.resolve(probe).toUri();
  }

  /**
   * Returns an XML prolog declaring the external entity {@code &xxe;} on the probe file, for a
   * document whose root element is {@code root}.
   */
  private String doctype(String root) {
    return "<?xml version=\"1.0\"?><!DOCTYPE %s [<!ENTITY xxe SYSTEM \"%s\">]>"
        .formatted(root, probeUri);
  }

  /**
   * Passes if the parse succeeds, or fails without any error in the cause chain naming the probe
   * file.
   *
   * <p>Deliberately strict: an error naming the file means the parser at least tried to open it,
   * which counts as a failure even if the access was then refused. The entry points under test must
   * not attempt the access at all.
   */
  private void assertNotResolved(Executable parse) {
    try {
      parse.execute();
    } catch (Throwable e) {
      for (Throwable t = e; t != null; t = t.getCause()) {
        var message = String.valueOf(t.getMessage());
        assertFalse(message.contains(probe), () -> "external entity was resolved: " + message);
      }
    }
  }

  /**
   * Control test for the detection method used by all the other tests.
   *
   * <p>Builds a parser that is vulnerable on purpose (DTD and external entities explicitly enabled)
   * and checks that it does fail with an error naming the probe file. Without this, the other tests
   * could stay green while proving nothing, e.g. if parsers stopped naming the file in their errors
   * or the probe URI were malformed.
   */
  @Test
  void probe_detects_a_parser_that_resolves_entities() {
    var factory = XMLInputFactory.newInstance();
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, true);
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, true);

    var error =
        assertThrows(
            XMLStreamException.class,
            () -> {
              var reader =
                  factory.createXMLStreamReader(new StringReader(doctype("r") + "<r>&xxe;</r>"));
              while (reader.hasNext()) {
                reader.next();
              }
            });
    assertTrue(String.valueOf(error.getMessage()).contains(probe), error::getMessage);
  }

  /**
   * must not resolve an external entity.
   *
   * <p>Checks that external entities are now off whatever parser the classpath provides, so {@code
   * &xxe;} is skipped and the file never opened.
   */
  @Test
  void dom_parsing_ignores_external_entities() {
    assertNotResolved(() -> XMLUtils.parse(new StringReader(doctype("r") + "<r>&xxe;</r>")));
  }

  /**
   * must not load an external DTD.
   *
   * <p>Checks that external DTD loading is off: the document parses without the DTD ever being
   * opened.
   */
  @Test
  void dom_parsing_does_not_load_external_dtd() {
    var xml = "<?xml version=\"1.0\"?><!DOCTYPE r SYSTEM \"%s\"><r/>".formatted(probeUri);
    assertNotResolved(() -> XMLUtils.parse(new StringReader(xml)));
  }

  /**
   * {@link XMLViews#fromXML} must not resolve an external entity.
   *
   * <p>{@code &xxe;} is an undeclared entity and the unmarshalling fails without opening the file.
   */
  @Test
  void views_reject_external_entities() {
    var xml =
        doctype("object-views")
            + "<object-views xmlns=\"%s\">&xxe;</object-views>".formatted(ObjectViews.NAMESPACE);
    assertNotResolved(() -> XMLViews.fromXML(xml));
  }
}
