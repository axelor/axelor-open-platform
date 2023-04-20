package com.axelor.auth.pac4j;

import com.axelor.auth.AuthUtils;
import io.buji.pac4j.profile.ShiroProfileManager;
import java.io.IOException;
import java.util.Optional;
import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.pac4j.core.client.Clients;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.http.client.indirect.FormClient;
import org.pac4j.jee.context.JEEContext;
import org.pac4j.jee.context.session.JEESessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AxelorLoginFilter implements Filter {

  private final AxelorSecurityFilter axelorSecurityFilter;

  private final Clients clients;

  private static final Logger logger = LoggerFactory.getLogger(AxelorLoginFilter.class);

  @Inject
  public AxelorLoginFilter(AxelorSecurityFilter axelorSecurityFilter, Clients clients) {
    this.axelorSecurityFilter = axelorSecurityFilter;
    this.clients = clients;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    final Subject subject = SecurityUtils.getSubject();
    final boolean authenticated = subject.isAuthenticated();

    if (authenticated) {
      if (AuthUtils.getUser() == null) {
        logger.warn("Authenticated, but no user: {}", subject.getPrincipal());
        subject.logout();
      } else if (getUserProfile(request, response).isEmpty()) {
        logger.warn("Authenticated, but no user profile: {}", subject.getPrincipal());
        subject.logout();
      }
    }

    // if already authenticated or if form login is not configured, redirect to base url
    if (authenticated || clients.getClients().stream().noneMatch(FormClient.class::isInstance)) {
      ((HttpServletResponse) response).sendRedirect(".");
      return;
    }

    axelorSecurityFilter.doFilter(request, response, chain);
  }

  private Optional<UserProfile> getUserProfile(ServletRequest request, ServletResponse response) {
    final JEEContext context =
        new JEEContext((HttpServletRequest) request, (HttpServletResponse) response);
    final ShiroProfileManager profileManager =
        new ShiroProfileManager(context, JEESessionStore.INSTANCE);

    return profileManager.getProfile();
  }
}
