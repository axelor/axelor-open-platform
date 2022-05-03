/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
package com.axelor.auth.extensions;

import com.axelor.common.ResourceUtils;
import com.unboundid.ldap.listener.Base64PasswordEncoderOutputFormatter;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.listener.UnsaltedMessageDigestInMemoryPasswordEncoder;
import com.unboundid.ldif.LDIFReader;
import java.io.InputStream;
import java.security.MessageDigest;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LdapExtension implements BeforeAllCallback, AfterAllCallback {

  private static final Logger LOG = LoggerFactory.getLogger(LdapExtension.class);

  private final String LDIF_FILE = "test.ldif";

  // By default, a random available port is chosen if not provided.
  private int ldapPort;
  private InMemoryDirectoryServer ldapServer;

  public LdapExtension() {}

  public LdapExtension(int port) {
    this.ldapPort = port;
  }

  @SuppressWarnings("ConstantConditions")
  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    LOG.info("Starting LDAP server...");

    final MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
    final InMemoryDirectoryServerConfig config =
        new InMemoryDirectoryServerConfig("dc=test,dc=com");

    config.addAdditionalBindCredentials("uid=admin,ou=system", "secret");
    config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("LDAP", ldapPort));
    config.setEnforceSingleStructuralObjectClass(false);
    config.setEnforceAttributeSyntaxCompliance(true);
    config.setPasswordEncoders(
        new UnsaltedMessageDigestInMemoryPasswordEncoder(
            "{SHA}", Base64PasswordEncoderOutputFormatter.getInstance(), sha1Digest));

    ldapServer = new InMemoryDirectoryServer(config);
    try (InputStream is = ResourceUtils.getResourceStream(LDIF_FILE)) {
      ldapServer.importFromLDIF(false, new LDIFReader(is));
    }

    ldapServer.startListening();
    ldapPort = ldapServer.getListenPort();

    LOG.info("LDAP server started. Listen on port " + ldapPort);
  }

  @Override
  public void afterAll(ExtensionContext context) {
    LOG.info("Shutdown LDAP server...");

    ldapServer.close();
    ldapServer = null;
  }

  public int getLdapPort() {
    return ldapPort;
  }
}
