/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web.service;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.MFAService;
import com.axelor.auth.identity.IdentityInfo;
import com.axelor.auth.identity.IdentityVerificationService;
import com.axelor.i18n.I18n;
import com.google.inject.servlet.RequestScoped;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/auth/identity")
public class IdentityResource {

  private final IdentityVerificationService verificationService;
  private final MFAService mfaService;

  @Inject
  public IdentityResource(IdentityVerificationService verificationService, MFAService mfaService) {
    this.verificationService = verificationService;
    this.mfaService = mfaService;
  }

  @GET
  @Hidden
  public Response getIdentityInfo() {
    IdentityInfo info = verificationService.getIdentityInfo();
    Map<String, Object> data = new HashMap<>();
    data.put("requiresPassword", info.requiresPassword());
    data.put("requiresMfa", info.requiresMfa());
    data.put("mfaMethods", info.mfaMethods());

    mfaService.processEmailMethod(data, info.mfaMethods(), AuthUtils.getUser().getCode());

    return Response.ok(data).build();
  }

  @POST
  @Hidden
  public Response verifyIdentity(Map<String, Object> data) {
    if (data == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(Map.of("message", I18n.get("Invalid request")))
          .build();
    }

    var response = new com.axelor.rpc.Response();

    try {
      verificationService.verifyIdentity(data);
      response.setStatus(com.axelor.rpc.Response.STATUS_SUCCESS);
    } catch (IllegalArgumentException e) {
      response.setStatus(com.axelor.rpc.Response.STATUS_FAILURE);
      response.setData(Map.of("message", e.getMessage()));
    }

    return Response.ok(response).build();
  }
}
