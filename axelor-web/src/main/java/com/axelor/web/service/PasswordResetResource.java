/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
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
package com.axelor.web.service;

import com.axelor.auth.AuthPasswordResetService;
import com.axelor.auth.AuthService;
import com.axelor.common.StringUtils;
import com.axelor.i18n.I18n;
import com.google.inject.servlet.RequestScoped;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/public/password-reset")
public class PasswordResetResource {

  private final AuthPasswordResetService authPasswordResetService;

  private static final Logger logger = LoggerFactory.getLogger(PasswordResetResource.class);

  @Inject
  public PasswordResetResource(AuthPasswordResetService authPasswordResetService) {
    this.authPasswordResetService = authPasswordResetService;
  }

  @POST
  @Hidden
  @Path("/forgot")
  public Response forgot(Map<String, Object> data) {
    if (!authPasswordResetService.isEnabled()) {
      return serviceDisabled();
    }

    if (data == null) {
      return badRequest();
    }

    final var email = (String) data.get("email");

    if (StringUtils.isBlank(email)) {
      return badRequest();
    }

    try {
      authPasswordResetService.submitForgotPassword(email);
    } catch (final Exception e) {
      return error(e);
    }

    return ok();
  }

  @POST
  @Hidden
  @Path("/verify")
  public Response verify(Map<String, Object> data) {
    if (!authPasswordResetService.isEnabled()) {
      return serviceDisabled();
    }

    if (data == null) {
      return badRequest();
    }

    final var token = (String) data.get("token");

    if (StringUtils.isBlank(token)) {
      return badRequest();
    }

    try {
      authPasswordResetService.checkToken(token);
    } catch (final IllegalArgumentException e) {
      return errorMessage(e);
    } catch (final Exception e) {
      return error(e);
    }

    final var authService = AuthService.getInstance();

    return ok(
        Map.of(
            "passwordPattern",
            authService.getPasswordPattern(),
            "passwordPatternTitle",
            authService.getPasswordPatternTitle()));
  }

  @POST
  @Hidden
  @Path("/reset")
  public Response reset(Map<String, Object> data) {
    if (!authPasswordResetService.isEnabled()) {
      return serviceDisabled();
    }

    if (data == null) {
      return badRequest();
    }

    final var token = (String) data.get("token");
    final var password = (String) data.get("password");

    if (StringUtils.isBlank(token) || StringUtils.isBlank(password)) {
      return badRequest();
    }

    try {
      authPasswordResetService.changePassword(token, password);
    } catch (final IllegalArgumentException e) {
      return errorMessage(e);
    } catch (final Exception e) {
      return error(e);
    }

    return ok();
  }

  private Response ok() {
    return Response.ok().build();
  }

  private Response ok(Object data) {
    return Response.ok(data).build();
  }

  private Response error(Exception e) {
    logger.error(e.getMessage());
    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
  }

  private Response errorMessage(IllegalArgumentException e) {
    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
        .entity(errorEntity(e.getMessage()))
        .build();
  }

  private Response badRequest() {
    return Response.status(Response.Status.BAD_REQUEST)
        .entity(errorEntity(I18n.get("Invalid request")))
        .build();
  }

  private Response serviceDisabled() {
    return Response.status(Response.Status.FORBIDDEN)
        .entity(errorEntity(I18n.get("Service disabled")))
        .build();
  }

  private Object errorEntity(String message) {
    return Map.of("message", message);
  }
}
