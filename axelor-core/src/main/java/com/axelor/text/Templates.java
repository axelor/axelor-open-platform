/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.text;

import java.io.File;
import java.io.IOException;
import java.io.Reader;

/** This interface defines API for template engine integration. */
public interface Templates {

  /**
   * Create a new {@link Template} instance from the given template text.
   *
   * @param text the template text
   * @return an instance of {@link Template}
   */
  Template fromText(String text);

  /**
   * Create a new {@link Template} instance from the given template file.
   *
   * @param file the template file
   * @return an instance of {@link Template}
   * @throws IOException if file read throws {@link IOException}
   */
  Template from(File file) throws IOException;

  /**
   * Create a new {@link Template} instance from the given reader.
   *
   * @param reader the {@link Reader} to read the template
   * @return an instance of {@link Template}
   * @throws IOException if reader throws {@link IOException}
   */
  Template from(Reader reader) throws IOException;
}
