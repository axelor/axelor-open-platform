package com.axelor.auth;

import java.util.Properties;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.realm.ldap.JndiLdapContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.auth.db.Group;
import com.axelor.auth.db.User;
import com.google.common.base.Objects;
import com.google.inject.persist.Transactional;

@Singleton
public class AuthLdap {

	private final Logger log = LoggerFactory.getLogger(getClass());

	public static final String LDAP_SERVER_URL = "ldap.server.url";
	public static final String LDAP_AUTH_TYPE = "ldap.auth.type";

	public static final String LDAP_SYSTEM_USER = "ldap.system.user";
	public static final String LDAP_SYSTEM_PASSWORD = "ldap.system.password";

	public static final String LDAP_GROUP_BASE = "ldap.group.base";
	public static final String LDAP_GROUP_FILTER = "ldap.group.filter";

	public static final String LDAP_USER_BASE = "ldap.user.base";
	public static final String LDAP_USER_FILTER = "ldap.user.filter";

	public static final String DEFAULT_AUTH_TYPE = "simple";

	private String ldapServerUrl;

	private String ldapAuthType;

	private String ldapSysUser;

	private String ldapSysPassword;

	private String ldapGroupsDn;

	private String ldapUsersDn;

	private String ldapGroupFilter;

	private String ldapUserFilter;

	private JndiLdapContextFactory factory = new JndiLdapContextFactory();

	private AuthService authService;

	@Inject
	public AuthLdap(@Named("auth.ldap.config") Properties properties, AuthService authService) {
		ldapServerUrl = properties.getProperty(LDAP_SERVER_URL);
		ldapAuthType = properties.getProperty(LDAP_AUTH_TYPE, DEFAULT_AUTH_TYPE);
		ldapSysUser = properties.getProperty(LDAP_SYSTEM_USER);
		ldapSysPassword = properties.getProperty(LDAP_SYSTEM_PASSWORD);
		ldapGroupsDn = properties.getProperty(LDAP_GROUP_BASE);
		ldapUsersDn = properties.getProperty(LDAP_USER_BASE);
		ldapGroupFilter = properties.getProperty(LDAP_GROUP_FILTER);
		ldapUserFilter = properties.getProperty(LDAP_USER_FILTER);

		factory.setUrl(ldapServerUrl);
		factory.setSystemUsername(ldapSysUser);
		factory.setSystemPassword(ldapSysPassword);
		factory.setAuthenticationMechanism(ldapAuthType);

		this.authService = authService;
	}

	public User findOrCreateUser(String subject) {
		return null;
	}

	public boolean isEnabled() {
		return ldapServerUrl != null && !"".equals(ldapServerUrl.trim());
	}

	@Transactional
	public boolean login(String user, String password) throws AuthenticationException {
		if (!this.isEnabled()) {
			throw new IllegalStateException("LDAP is not enabled.");
		}
		try {
			return doLogin(user, password);
		} catch (NamingException e) {
			throw new AuthenticationException(e);
		}
	}

	private boolean doLogin(final String user, final String password) throws NamingException {
		final NamingEnumeration<?> all = search(ldapUsersDn, ldapUserFilter, user);
		if (!all.hasMore()) {
			throw new NamingException("LDAP user does not exist: " + user);
		}
		while (all.hasMore()) {
			final SearchResult result = (SearchResult) all.next();
			final String dn = result.getNameInNamespace();
			factory.getLdapContext((Object) dn, password);
			findOrCreateUser(user, result);
			return true;
		}
		return false;
	}

	private NamingEnumeration<?> search(String where, String filter, String user)
			throws NamingException {

		final SearchControls controls = new SearchControls();
		controls.setSearchScope(SearchControls.SUBTREE_SCOPE);

		String filterString = filter.replaceAll("\\{0\\}", user);

		LdapContext context = factory.getSystemLdapContext();
		try {
			return context.search(where, filterString, controls);
		} finally {
			if (context != null) {
				context.close();
			}
		}
	}

	private User findOrCreateUser(String code, SearchResult result)
			throws NamingException {

		User user = User.findByCode(code);
		if (user != null) {
			return user;
		}

		Attributes attributes = result.getAttributes();
		Attribute cn = attributes.get("cn");
		String name = code;
		try {
			name = (String) cn.get();
		} catch (NamingException e) {};

		user = new User(code, name);
		user.setPassword(UUID.randomUUID().toString());

		authService.encrypt(user);

		try {
			Group group = findOrCreateGroup(user);
			user.setGroup(group);
		} catch (Exception e) {
		}

		return user.save();
	}

	private Group findOrCreateGroup(User user) throws NamingException {
		Group group = user.getGroup();
		if (group != null) {
			return group;
		}

		final NamingEnumeration<?> all = search(ldapGroupsDn, ldapGroupFilter, user.getCode());
		while (all.hasMore()) {
			SearchResult result = (SearchResult) all.next();
			Attributes attributes = result.getAttributes();
			String name = (String) attributes.get("cn").get();
			group = Group.findByCode(name);
			if (group == null) {
				group = new Group(name, name.substring(0, 1).toUpperCase() + name.substring(1));
			}
			break;
		}

		if (all.hasMore()) {
			log.warn("more then one groups defined.");
		}

		return group;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(getClass())
				.add("url", ldapServerUrl)
				.toString();
	}
}
