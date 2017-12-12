/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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
package com.axelor.db.tenants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.axelor.common.StringUtils;
import com.google.common.base.MoreObjects;

/**
 * The default implementation of {@link TenantConfig} uses configuration
 * provided from application.properties.
 * 
 * <p>
 * The format of application.properties are as follows:
 * </p>
 * 
 * <pre>
 * db.default.visible = false
 * db.default.driver = org.postgresql.Driver
 * db.default.url = jdbc:postgresql://localhost:5432/axelor-db-demo
 * db.default.user = axelor
 * db.default.password =
 * 
 * db.company1.name = Company 1
 * db.company1.driver = org.postgresql.Driver
 * db.company1.url = jdbc:postgresql://localhost:5432/axelor-db1
 * db.company1.user = axelor
 * db.company1.password =
 * 
 * db.company2.name = Company 2
 * db.company2.driver = org.postgresql.Driver
 * db.company2.url = jdbc:postgresql://localhost:5432/axelor-db2
 * db.company2.user = axelor
 * db.company2.password =
 * </pre>
 * 
 * <p>
 * The format of key name is <code>db.[tenant-id].[config-name]</code>
 * </p>
 * 
 */
public class TenantConfigImpl implements TenantConfig {

	private Boolean active;
	private Boolean visible;

	private String tenantId;
	private String tenantName;
	private String tenantHosts;
	private String tenantRoles;

	private String jndiDataSource;

	private String jdbcDriver;
	private String jdbcUrl;
	private String jdbcUser;
	private String jdbcPassword;

	private static final Pattern PATTERN_DB_NAME = Pattern.compile("db\\.(.*?)\\.name");

	private static final Map<String, TenantConfig> CONFIGS = new ConcurrentHashMap<>();

	private TenantConfigImpl() {
	}

	public static List<TenantConfig> findByHost(Properties props, String host) {
		final List<TenantConfig> all = new ArrayList<>();
		for (String key : props.stringPropertyNames()) {
			Matcher matcher = PATTERN_DB_NAME.matcher(key);
			if (matcher.matches()) {
				String tenantId = matcher.group(1);
				if (matches(props, tenantId, host)) {
					all.add(findById(props, matcher.group(1)));
				}
			}
		}
		if (all.isEmpty() && matches(props, DEFAULT_TENANT_ID, host)) {
			all.add(findById(props, DEFAULT_TENANT_ID));
		}

		// sort by name
		try {
			all.sort((a, b) -> a.getTenantName().compareTo(b.getTenantName()));
		} catch (Exception e) {
		}

		return all;
	}

	public static TenantConfig findById(Properties props, String tenantId) {
		if (CONFIGS.containsKey(tenantId)) {
			return CONFIGS.get(tenantId);
		}

		final String prefix = "db." + tenantId;
		final TenantConfigImpl cfg = new TenantConfigImpl();

		final String active = get(props, prefix, "active");
		final String visible = get(props, prefix, "visible");

		cfg.active = active == null || active == "true";
		cfg.visible = visible == null || visible == "true";

		cfg.tenantId = tenantId;
		cfg.tenantName = get(props, prefix, "name");
		cfg.tenantHosts = get(props, prefix, "hosts");
		cfg.tenantRoles = get(props, prefix, "roles");

		cfg.jndiDataSource = get(props, prefix, "datasource");

		cfg.jdbcDriver = get(props, prefix, "driver");
		cfg.jdbcUrl = get(props, prefix, "url");
		cfg.jdbcUser = get(props, prefix, "user");
		cfg.jdbcPassword = get(props, prefix, "password");

		if (cfg.jndiDataSource == null && (cfg.jdbcDriver == null || cfg.jdbcUrl == null)) {
			return null;
		}

		CONFIGS.put(tenantId, cfg);

		return cfg;
	}

	private static boolean matches(Properties props, String tenantId, String host) {
		final String key = "db." + tenantId + ".hosts";
		final String hosts = props.getProperty(key, "");
		return StringUtils.isBlank(hosts) || Arrays.asList(hosts.split(",")).contains(host);
	}

	private static String get(Properties props, String prefix, String name) {
		String key = prefix + "." + name;
		String val = props.getProperty(key);
		return StringUtils.isBlank(val) ? null : val;
	}

	@Override
	public Boolean getActive() {
		return active;
	}

	@Override
	public Boolean getVisible() {
		return visible;
	}

	@Override
	public String getTenantId() {
		return tenantId;
	}

	@Override
	public String getTenantName() {
		return tenantName;
	}

	@Override
	public String getTenantHosts() {
		return tenantHosts;
	}

	@Override
	public String getTenantRoles() {
		return tenantRoles;
	}

	@Override
	public String getJndiDataSource() {
		return jndiDataSource;
	}

	@Override
	public String getJdbcDriver() {
		return jdbcDriver;
	}

	@Override
	public String getJdbcUrl() {
		return jdbcUrl;
	}

	@Override
	public String getJdbcUser() {
		return jdbcUser;
	}

	@Override
	public String getJdbcPassword() {
		return jdbcPassword;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("tenantId", tenantId).add("tenantName", tenantName)
				.add("jndiDataSource", jndiDataSource).add("jdbcDriver", jdbcDriver).add("jdbcUrl", jdbcUrl)
				.omitNullValues().toString();
	}
}
