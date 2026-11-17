/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.loader;

import com.axelor.common.XMLUtils;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.UnmarshalException;
import jakarta.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.io.Reader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/** Unmarshals XML through a StAX reader that refuses DTDs and external entities. */
final class SecureUnmarshal {

  private static final XMLInputFactory INPUT_FACTORY = XMLUtils.createXMLInputFactory();

  private SecureUnmarshal() {}

  static Object unmarshal(Unmarshaller unmarshaller, Reader reader) throws JAXBException {
    final XMLStreamReader xml;
    try {
      xml = INPUT_FACTORY.createXMLStreamReader(reader);
    } catch (XMLStreamException e) {
      throw new UnmarshalException(e);
    }
    return unmarshal(unmarshaller, xml);
  }

  static Object unmarshal(Unmarshaller unmarshaller, InputStream stream) throws JAXBException {
    final XMLStreamReader xml;
    try {
      xml = INPUT_FACTORY.createXMLStreamReader(stream);
    } catch (XMLStreamException e) {
      throw new UnmarshalException(e);
    }
    return unmarshal(unmarshaller, xml);
  }

  private static Object unmarshal(Unmarshaller unmarshaller, XMLStreamReader xml)
      throws JAXBException {
    try {
      return unmarshaller.unmarshal(xml);
    } catch (RuntimeException e) {
      // MOXy's StAX entry point lets binding errors (e.g. an unknown enum value) escape as they
      // are, where its Reader and InputStream entry points wrap them: keep the latter contract
      throw new UnmarshalException(e);
    } finally {
      try {
        xml.close(); // releases the reader only; the caller still owns the underlying input
      } catch (XMLStreamException ignore) {
        // nothing left to release
      }
    }
  }
}
