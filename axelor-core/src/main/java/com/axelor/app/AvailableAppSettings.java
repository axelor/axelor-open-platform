/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
package com.axelor.app;

import org.hibernate.cache.jcache.ConfigSettings;
import org.hibernate.cfg.Environment;

public interface AvailableAppSettings {

  String APPLICATION_NAME = "application.name";
  String APPLICATION_DESCRIPTION = "application.description";
  String APPLICATION_HOME = "application.home";
  String APPLICATION_VERSION = "application.version";
  String APPLICATION_AUTHOR = "application.author";
  String APPLICATION_LOCALE = "application.locale";
  String APPLICATION_THEME = "application.theme";
  String APPLICATION_HELP = "application.help";
  String APPLICATION_COPYRIGHT = "application.copyright";
  String APPLICATION_LOGO = "application.logo";
  String APPLICATION_MODE = "application.mode";
  String APPLICATION_BASE_URL = "application.base-url";
  String APPLICATION_CONFIG_PROVIDER = "application.config-provider";
  String CONFIG_MULTI_TENANCY = "application.multi-tenancy";

  @Deprecated
  String APPLICATION_PERMISSION_DISABLE_RELATIONAL_FIELD =
      "application.permission.disable-relational-field";

  @Deprecated
  String APPLICATION_PERMISSION_DISABLE_ACTION = "application.permission.disable-action";

  String APPLICATION_SCRIPT_CACHE_SIZE = "application.script.cache.size";
  String APPLICATION_SCRIPT_CACHE_EXPIRE_TIME = "application.script.cache.expire-time";

  String APPLICATION_DOMAIN_BLOCKLIST_PATTERN = "application.domain-blocklist-pattern";

  String VIEW_CONFIRM_YES_NO = "view.confirm-yes-no";
  String VIEW_SINGLE_TAB = "view.single-tab";
  String VIEW_TABS_MAX = "view.max-tabs";
  String VIEW_CUSTOMIZATION = "view.allow-customization";

  String VIEW_MENUBAR_LOCATION = "view.menubar.location";

  String VIEW_TOOLBAR_TITLES = "view.toolbar.show-titles";

  String VIEW_FORM_CHECK_VERSION = "view.form.check-version";

  String VIEW_ADV_SEARCH_EXPORT_FULL = "view.adv-search.export-full";
  String VIEW_ADV_SEARCH_SHARE = "view.adv-search.share";

  String VIEW_GRID_SELECTION = "view.grid.selection";
  String VIEW_GRID_EDITOR_BUTTONS = "view.grid.editor-buttons";

  String CONTEXT_APP_STYLE = "context.appStyle";
  String CONTEXT_APP_LOGO = "context.appLogo";

  String DB_DEFAULT_DATASOURCE = "db.default.datasource";
  String DB_DEFAULT_DRIVER = "db.default.driver";
  String DB_DEFAULT_URL = "db.default.url";
  String DB_DEFAULT_USER = "db.default.user";
  String DB_DEFAULT_PASSWORD = "db.default.password";

  String REPORTS_DESIGN_DIR = "reports.design-dir";
  String REPORTS_FONTS_CONFIG = "reports.fonts-config";

  String TEMPLATE_SEARCH_DIR = "template.search-dir";

  String DATA_UPLOAD_DIR = "data.upload.dir";
  String FILE_UPLOAD_SIZE = "data.upload.max-size";
  String FILE_UPLOAD_FILENAME_PATTERN = "data.upload.filename-pattern";
  String FILE_UPLOAD_WHITELIST_PATTERN = "data.upload.allowlist.pattern";
  String FILE_UPLOAD_BLACKLIST_PATTERN = "data.upload.blocklist.pattern";
  String FILE_UPLOAD_WHITELIST_TYPES = "data.upload.allowlist.types";
  String FILE_UPLOAD_BLACKLIST_TYPES = "data.upload.blocklist.types";

  String DATA_EXPORT_DIR = "data.export.dir";
  String DATA_EXPORT_MAX_SIZE = "data.export.max-size";
  String DATA_EXPORT_FETCH_SIZE = "data.export.fetch-size";
  String DATA_EXPORT_ENCODING = "data.export.encoding";
  String DATA_EXPORT_LOCALE = "data.export.locale";
  String DATA_EXPORT_SEPARATOR = "data.export.separator";

  String DATA_IMPORT_DEMO_DATA = "data.import.demo-data";

  String CORS_ALLOW_ORIGIN = "cors.allow-origin";
  String CORS_ALLOW_CREDENTIALS = "cors.allow-credentials";
  String CORS_ALLOW_METHODS = "cors.allow-methods";
  String CORS_ALLOW_HEADERS = "cors.allow-headers";
  String CORS_EXPOSE_HEADERS = "cors.expose-headers";
  String CORS_MAX_AGE = "cors.max-age";

  String SESSION_TIMEOUT = "session.timeout";
  String SESSION_COOKIE_SECURE = "session.cookie.secure";

  String QUARTZ_ENABLE = "quartz.enable";
  String QUARTZ_THREAD_COUNT = "quartz.thread-count";

  String USER_PASSWORD_PATTERN = "user.password.pattern";
  String USER_PASSWORD_PATTERN_TITLE = /*$$(*/ "user.password.pattern-title" /*)*/;

  String ENCRYPTION_ALGORITHM = "encryption.algorithm";
  String ENCRYPTION_PASSWORD = "encryption.password";
  String ENCRYPTION_OLD_ALGORITHM = "encryption.old-algorithm";
  String ENCRYPTION_OLD_PASSWORD = "encryption.old-password";

  String HIBERNATE_SEARCH_DEFAULT_DIRECTORY_PROVIDER =
      "hibernate.search.default.directory_provider";
  String HIBERNATE_SEARCH_DEFAULT_INDEX_BASE = "hibernate.search.default.indexBase";

  String HIBERNATE_HIKARI_MINIMUM_IDLE = "hibernate.hikari.minimumIdle";
  String HIBERNATE_HIKARI_MAXIMUM_POOL_SIZE = "hibernate.hikari.maximumPoolSize";
  String HIBERNATE_HIKARI_IDLE_TIMEOUT = "hibernate.hikari.idleTimeout";

  String HIBERNATE_JDBC_BATCH_SIZE = "hibernate.jdbc.batch_size";
  String HIBERNATE_JDBC_FETCH_SIZE = "hibernate.jdbc.fetch_size";

  String HIBERNATE_CACHE_REGION_FACTORY = Environment.CACHE_REGION_FACTORY;
  String HIBERNATE_JAVAX_CACHE_PROVIDER = ConfigSettings.PROVIDER;

  String JAVAX_PERSISTENCE_SHARED_CACHE_MODE = Environment.JPA_SHARED_CACHE_MODE;

  String MAIL_SMTP_HOST = "mail.smtp.host";
  String MAIL_SMTP_PORT = "mail.smtp.port";
  String MAIL_SMTP_USER = "mail.smtp.user";
  String MAIL_SMTP_PASSWORD = "mail.smtp.password";
  String MAIL_SMTP_CHANNEL = "mail.smtp.channel";
  String MAIL_SMTP_TIMEOUT = "mail.smtp.timeout";
  String MAIL_SMTP_CONNECTION_TIMEOUT = "mail.smtp.connection-timeout";
  String MAIL_SMTP_FROM = "mail.smtp.from";

  String MAIL_IMAP_HOST = "mail.imap.host";
  String MAIL_IMAP_PORT = "mail.imap.port";
  String MAIL_IMAP_USER = "mail.imap.user";
  String MAIL_IMAP_PASSWORD = "mail.imap.password";
  String MAIL_IMAP_CHANNEL = "mail.imap.channel";
  String MAIL_IMAP_TIMEOUT = "mail.imap.timeout";
  String MAIL_IMAP_CONNECTION_TIMEOUT = "mail.imap.connection-timeout";

  String LOGGING_PATH = "logging.path";
  String LOGGING_CONFIG = "logging.config";
  String LOGGING_PATTERN_FILE = "logging.pattern.file";
  String LOGGING_PATTERN_CONSOLE = "logging.pattern.console";

  String AUTH_CALLBACK_URL = "auth.callback.url";
  String AUTH_USER_PROVISIONING = "auth.user.provisioning";
  String AUTH_USER_DEFAULT_GROUP = "auth.user.default.group";
  String AUTH_USER_PRINCIPAL_ATTRIBUTE = "auth.user.principal.attribute";

  String AUTH_LOGOUT_URL = "auth.logout.url";
  String AUTH_LOGOUT_URL_PATTERN = "auth.logout.url.pattern";
  String AUTH_LOGOUT_LOCAL = "auth.logout.local";
  String AUTH_LOGOUT_CENTRAL = "auth.logout.central";

  String AUTH_LOCAL_BASIC_AUTH_ENABLED = "auth.local.basic.auth.enabled";

  String AUTH_LDAP_SERVER_URL = "auth.ldap.server.url";
  String AUTH_LDAP_USER_BASE = "auth.ldap.user.base";
  String AUTH_LDAP_USER_FILTER = "auth.ldap.user.filter";
  String AUTH_LDAP_USER_DN_FORMAT = "auth.ldap.user.dn.format";
  String AUTH_LDAP_USER_ID_ATTRIBUTE = "auth.ldap.user.id.attribute";
  String AUTH_LDAP_USER_USERNAME_ATTRIBUTE = "auth.ldap.user.username.attribute";
  String AUTH_LDAP_GROUP_BASE = "auth.ldap.group.base";
  String AUTH_LDAP_GROUP_FILTER = "auth.ldap.group.filter";
  String AUTH_LDAP_SYSTEM_USER = "auth.ldap.system.user";
  String AUTH_LDAP_SYSTEM_PASSWORD = "auth.ldap.system.password";
  String AUTH_LDAP_AUTH_TYPE = "auth.ldap.auth.type";
  String AUTH_LDAP_USE_SSL = "auth.ldap.use.ssl";
  String AUTH_LDAP_USE_STARTTLS = "auth.ldap.use.starttls";
  String AUTH_LDAP_CREDENTIAL_TRUST_STORE = "auth.ldap.credential.trust.store";
  String AUTH_LDAP_CREDENTIAL_KEY_STORE = "auth.ldap.credential.key.store";
  String AUTH_LDAP_CREDENTIAL_STORE_PASSWORD = "auth.ldap.credential.store.password";
  String AUTH_LDAP_CREDENTIAL_STORE_TYPE = "auth.ldap.credential.store.type";
  String AUTH_LDAP_CREDENTIAL_STORE_ALIASES = "auth.ldap.credential.store.aliases";
  String AUTH_LDAP_CREDENTIAL_TRUST_CERTIFICATES = "auth.ldap.credential.trust.certificates";
  String AUTH_LDAP_CREDENTIAL_AUTHENTICATION_CERTIFICATE =
      "auth.ldap.credential.authentication.certificate";
  String AUTH_LDAP_CREDENTIAL_AUTHENTICATION_KEY = "auth.ldap.credential.authentication.key";
  String AUTH_LDAP_CONNECT_TIMEOUT = "auth.ldap.connect.timeout";
  String AUTH_LDAP_RESPONSE_TIMEOUT = "auth.ldap.response.timeout";
}
