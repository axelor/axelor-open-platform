/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web.service;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.MFAService;
import com.axelor.auth.MFATooManyRequestsException;
import com.axelor.auth.db.User;
import com.axelor.auth.pac4j.AxelorProfileManager;
import com.axelor.common.StringUtils;
import com.axelor.i18n.I18n;
import com.google.inject.servlet.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;

@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/public/mfa/email-code")
public class MFAEmailService {
  private final MFAService mfaService;

  @Inject
  public MFAEmailService(MFAService mfaService) {
    this.mfaService = mfaService;
  }

  @POST
  @Path("/send")
  public Response sendEmail(Map<String, Object> data) {
    if (data == null) {
      return badRequest();
    }

    final String username = (String) data.get("username");
    if (StringUtils.isBlank(username)) {
      return badRequest();
    }

    User user = null;

    try {
      Subject subject = SecurityUtils.getSubject();
      Session session = subject.getSession(false);

      if (session == null) {
        return forbidden();
      }

      Object pendingUsername = session.getAttribute(AxelorProfileManager.PENDING_USER_NAME);
      if (pendingUsername == null || StringUtils.isBlank(pendingUsername.toString())) {
        return forbidden();
      }

      if (!username.equals(pendingUsername.toString())) {
        return forbidden();
      }

      user = AuthUtils.getUser(username);
      if (user == null) {
        return forbidden();
      }

      LocalDateTime emailRetryAfter = mfaService.sendEmailCode(user);

      return success(
          Map.of(
              "message",
              I18n.get("Verification code sent successfully. Please check your emails."),
              "emailRetryAfter",
              format(emailRetryAfter)));
    } catch (Exception e) {
      if (e instanceof MFATooManyRequestsException tooManyRequests && user != null) {
        return tooManyRequests(tooManyRequests);
      }

      return error();
    }
  }

  private Response success(Object data) {
    return Response.ok(data).build();
  }

  private Response badRequest() {
    return Response.status(Response.Status.BAD_REQUEST).build();
  }

  private Response forbidden() {
    return Response.status(Response.Status.FORBIDDEN).build();
  }

  private Response error() {
    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
  }

  private Response tooManyRequests(MFATooManyRequestsException e) {
    return Response.status(Response.Status.TOO_MANY_REQUESTS)
        .entity(Map.of("message", e.getMessage(), "emailRetryAfter", format(e.getRetryAfter())))
        .build();
  }

  private String format(LocalDateTime localDateTime) {
    return localDateTime
        .atZone(ZoneId.systemDefault())
        .withZoneSameInstant(ZoneOffset.UTC)
        .toString();
  }
}
