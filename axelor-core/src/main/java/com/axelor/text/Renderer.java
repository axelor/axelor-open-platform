/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.text;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

/**
 * The {@link Template} renderer provides ways to render an encapsulated {@link Template} instance.
 */
public abstract class Renderer {

  /**
   * Render the template.
   *
   * @return the template output
   */
  public String render() {
    StringWriter out = new StringWriter();
    try {
      render(out);
    } catch (IOException e) {
    }
    return out.toString();
  }

  /**
   * Render the template.
   *
   * @param out the writer instance to write the output
   * @throws IOException if writer throws any {@link IOException}
   */
  public abstract void render(Writer out) throws IOException;
}
