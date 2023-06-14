package com.axelor.auth.pac4j;

import com.axelor.auth.AuthSessionService;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import java.time.LocalDateTime;
import java.util.List;
import javax.inject.Singleton;
import org.apache.shiro.SecurityUtils;
import org.pac4j.core.authorization.authorizer.Authorizer;
import org.pac4j.core.context.WebContext;

@Singleton
public class AxelorUserAuthorizer implements Authorizer {

  public static final String USER_AUTHORIZER_NAME = "AxelorUserAuthorizer";

  private AuthSessionService authSessionService;

  public AxelorUserAuthorizer(AuthSessionService authSessionService) {
    this.authSessionService = authSessionService;
  }

  @Override
  public boolean isAuthorized(WebContext context, List profiles) {
    User user = AuthUtils.getUser();
    if (user == null) {
      return false;
    }
    if (!isAllowed(user)) {
      removeSession();
      return false;
    }

    return true;
  }

  private boolean isAllowed(User user) {
    final LocalDateTime loginDate =
        authSessionService.getLoginDate(AuthUtils.getSubject().getSession());
    return AuthUtils.isActive(user)
        && (user.getPasswordUpdatedOn() == null
            || loginDate != null && !loginDate.isBefore(user.getPasswordUpdatedOn()));
  }

  private void removeSession() {
    try {
      SecurityUtils.getSubject().logout();
    } catch (Exception e) {
      // ignore
    }
  }
}
