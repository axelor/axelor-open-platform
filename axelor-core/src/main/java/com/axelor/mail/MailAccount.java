package com.axelor.mail;

import javax.mail.Session;

/**
 * The {@link MailAccount} provides a single definition {@link #getSession()} to
 * create account specific {@link Session} instance.
 * 
 */
public interface MailAccount {

	/**
	 * Get a {@link Session} for this account.<br>
	 * <br>
	 * The account implementation can decide whether to cache the session
	 * instance or not.
	 * 
	 * @return a {@link Session} instance.
	 */
	Session getSession();
}
