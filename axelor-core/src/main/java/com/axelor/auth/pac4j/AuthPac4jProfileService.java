/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.auth.pac4j;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.auth.db.Group;
import com.axelor.auth.db.Permission;
import com.axelor.auth.db.Role;
import com.axelor.auth.db.repo.GroupRepository;
import com.axelor.auth.db.repo.PermissionRepository;
import com.axelor.auth.db.repo.RoleRepository;
import com.axelor.common.StringUtils;
import com.axelor.meta.db.MetaSelect;
import com.axelor.meta.db.MetaSelectItem;
import com.axelor.meta.db.repo.MetaSelectRepository;
import com.google.inject.Singleton;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.inject.Inject;
import org.pac4j.core.profile.CommonProfile;

@Singleton
public class AuthPac4jProfileService {
  @Inject protected GroupRepository groupRepo;
  @Inject protected RoleRepository roleRepo;
  @Inject protected PermissionRepository permissionRepo;
  @Inject protected MetaSelectRepository metaSelectRepo;

  public String getCodeOrEmail(CommonProfile profile) {
    return Stream.of(profile.getUsername(), profile.getEmail(), profile.getId())
        .filter(StringUtils::notBlank)
        .findFirst()
        .orElseThrow(NullPointerException::new);
  }

  public String getName(CommonProfile profile) {
    // Backward-compatible CAS configuration
    if (AuthPac4jModuleCas.isEnabled()) {
      final Optional<String> name =
          Optional.ofNullable(
                  AppSettings.get().get(AvailableAppSettings.AUTH_CAS_ATTRS_USER_NAME, null))
              .map(profile::getAttribute)
              .map(Object::toString)
              .filter(StringUtils::notBlank);
      if (name.isPresent()) {
        return name.get();
      }
    }

    if (StringUtils.notBlank(profile.getDisplayName())) {
      return profile.getDisplayName();
    }

    return getCodeOrEmail(profile);
  }

  @Nullable
  public String getEmail(CommonProfile profile) {
    // Backward-compatible CAS configuration
    if (AuthPac4jModuleCas.isEnabled()) {
      final Optional<String> email =
          Optional.ofNullable(
                  AppSettings.get().get(AvailableAppSettings.AUTH_CAS_ATTRS_USER_EMAIL, null))
              .map(profile::getAttribute)
              .map(Object::toString)
              .filter(StringUtils::notBlank);
      if (email.isPresent()) {
        return email.get();
      }
    }

    return Stream.of(profile.getEmail(), profile.getId())
        .filter(StringUtils::notBlank)
        .filter(email -> email.matches(".+\\@.+\\..+"))
        .findFirst()
        .orElse(null);
  }

  @Nullable
  public String getLanguage(CommonProfile profile) {
    final Locale locale = profile.getLocale();

    if (locale != null) {
      final Set<String> languages = getLanguages();
      final String language = locale.toString();

      if (languages.contains(language)) {
        return language;
      }

      final String shortLanguage = language.split("_")[0];

      if (languages.contains(shortLanguage)) {
        return shortLanguage;
      }
    }

    return null;
  }

  @Nullable
  public String getLanguage(CommonProfile profile, String defaultLanguage) {
    return Optional.ofNullable(getLanguage(profile)).orElse(defaultLanguage);
  }

  @Nullable
  public byte[] getImage(CommonProfile profile) throws IOException {
    final URI uri;

    if (profile.getPictureUrl() != null) {
      uri = profile.getPictureUrl();
    } else if (profile.getProfileUrl() != null) {
      uri = profile.getProfileUrl();
    } else {
      uri = null;
    }

    if (uri == null) {
      return null;
    }

    return downloadUrl(uri.toURL());
  }

  @Nullable
  public Group getGroup(CommonProfile profile) {
    return Optional.ofNullable(profile.getAttribute("group"))
        .map(String::valueOf)
        .map(groupRepo::findByCode)
        .orElse(null);
  }

  @Nullable
  public Group getGroup(CommonProfile profile, String defaultGroupCode) {
    final String groupCode =
        Optional.ofNullable(profile.getAttribute("group"))
            .map(String::valueOf)
            .orElse(defaultGroupCode);
    return groupRepo.findByCode(groupCode);
  }

  public Set<Role> getRoles(CommonProfile profile) {
    return profile.getRoles().stream()
        .map(roleRepo::findByName)
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  public Set<Permission> getPermissions(CommonProfile profile) {
    return profile.getPermissions().stream()
        .map(permissionRepo::findByName)
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  protected Set<String> getLanguages() {
    final MetaSelect languageSelect = metaSelectRepo.findByName("select.language");

    if (languageSelect == null) {
      return Collections.emptySet();
    }

    return Optional.ofNullable(languageSelect.getItems()).orElse(Collections.emptyList()).stream()
        .map(MetaSelectItem::getValue)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private static final int DOWNLOAD_TIMEOUT = 5000;
  private static final int DOWNLOAD_CHUNK_SIZE = 32768;

  protected byte[] downloadUrl(URL url) throws IOException {
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    final URLConnection conn = url.openConnection();
    final byte[] chunk = new byte[DOWNLOAD_CHUNK_SIZE];

    conn.setConnectTimeout(DOWNLOAD_TIMEOUT);
    conn.setReadTimeout(DOWNLOAD_TIMEOUT);

    try (final InputStream is = conn.getInputStream()) {
      int bytesRead;

      while ((bytesRead = is.read(chunk)) > 0) {
        os.write(chunk, 0, bytesRead);
      }
    }

    return os.toByteArray();
  }
}
