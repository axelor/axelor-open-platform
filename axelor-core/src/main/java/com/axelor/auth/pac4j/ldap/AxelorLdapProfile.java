/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j.ldap;

import com.axelor.common.ObjectUtils;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.pac4j.ldap.profile.LdapProfile;

public class AxelorLdapProfile extends LdapProfile {
  private transient Path picturePath;

  @Override
  public String getUsername() {
    return getId();
  }

  @Override
  public String getEmail() {
    return (String) getAttribute(AxelorLdapProfileDefinition.EMAIL);
  }

  @Override
  public String getDisplayName() {
    return (String) getAttribute(AxelorLdapProfileDefinition.DISPLAY_NAME);
  }

  @Override
  public String getFirstName() {
    return (String) getAttribute(AxelorLdapProfileDefinition.FIRST_NAME);
  }

  @Override
  public String getFamilyName() {
    return (String) getAttribute(AxelorLdapProfileDefinition.FAMILY_NAME);
  }

  @Override
  public Locale getLocale() {
    return (Locale) getAttribute(AxelorLdapProfileDefinition.LOCALE);
  }

  @Override
  public URI getPictureUrl() {
    if (picturePath != null && picturePath.toFile().exists()) {
      return picturePath.toUri();
    }

    final byte[] jpegPhoto = (byte[]) getAttribute(AxelorLdapProfileDefinition.PICTURE_JPEG);
    if (ObjectUtils.notEmpty(jpegPhoto)) {
      try {
        picturePath = Files.createTempFile(null, ".jpg");
        Files.write(picturePath, jpegPhoto);
        return picturePath.toUri();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    return null;
  }
}
