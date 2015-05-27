/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2015 Axelor (<http://axelor.com>).
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
package com.axelor.auth;

import javax.inject.Inject;

import com.axelor.auth.db.User;
import com.axelor.auth.db.repo.UserRepository;
import com.google.common.base.Preconditions;

/**
 * This class can be used to run batch jobs that requires to keep track of audit
 * logs.
 *
 */
public class AuditableRunner {

	static ThreadLocal<User> batchUser = new ThreadLocal<>();

	private static final String DEFAULT_BATCH_USER = "admin";

	private UserRepository users;

	@Inject
	public AuditableRunner(UserRepository users) {
		this.users = users;
	}

	/**
	 * Run a batch job.
	 *
	 * @param job
	 *            the job to run
	 */
	public void run(Runnable job) {

		Preconditions.checkNotNull(job);
		Preconditions.checkNotNull(users);

		User user = AuthUtils.getUser();
		if (user == null) {
			user = users.findByCode(DEFAULT_BATCH_USER);
		}

		batchUser.set(user);
		try {
			job.run();
		} finally {
			batchUser.remove();
		}
	}
}
