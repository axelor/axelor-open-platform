/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
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

import com.axelor.common.MimeTypesUtils;
import com.axelor.common.StringUtils;
import com.google.inject.servlet.RequestScoped;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/public/app")
public class InfoResource {

  @Context private HttpServletRequest request;
  @Context private HttpServletResponse response;

  private final InfoService infoService;

  @Inject
  public InfoResource(InfoService infoService) {
    this.infoService = infoService;
  }

  /**
   * Retrieves either application login information or session information if the user is logged in.
   */
  @GET
  @Path("info")
  @Tag(name = "Metadata")
  @Operation(
      summary = "Retrieve metadata information for the application",
      description =
          "Retrieve metadata information for `application` and `authentication`. "
              + "If the user is logged in, also retrieve `user`, `view`, `api`, `data`, and `features` information.")
  public Map<String, Object> info() {
    return infoService.info(request, response);
  }

  @GET
  @Path("logo")
  @Hidden
  public Response getLogoContent() {
    return getImageContent(infoService.getLogo());
  }

  @GET
  @Path("icon")
  @Hidden
  public Response getIconContent() {
    return getImageContent(infoService.getIcon());
  }

  private Response getImageContent(String pathString) {
    if (StringUtils.notEmpty(pathString)) {
      final ServletContext context = request.getServletContext();
      try (final InputStream inputStream = context.getResourceAsStream(pathString)) {
        if (inputStream != null) {
          final byte[] imageData = inputStream.readAllBytes();
          final String mediaType = MimeTypesUtils.getContentType(pathString);
          return Response.ok(imageData).type(mediaType).build();
        }
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    return Response.status(Response.Status.NOT_FOUND).build();
  }
}
