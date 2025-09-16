/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web.service;

import com.axelor.app.AppSettings;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.common.MimeTypesUtils;
import com.axelor.common.ObjectUtils;
import com.axelor.common.StringUtils;
import com.axelor.common.UriBuilder;
import com.axelor.file.store.FileStoreFactory;
import com.axelor.file.store.Store;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaFile;
import com.axelor.meta.db.MetaTheme;
import com.axelor.meta.theme.MetaThemeService;
import com.google.inject.servlet.RequestScoped;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/public/app")
public class InfoResource {

  @Context private HttpServletRequest request;
  @Context private HttpServletResponse response;

  private final InfoService infoService;

  private static Logger log = LoggerFactory.getLogger(InfoResource.class);

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
          """
          Retrieve metadata information for `application` and `authentication`. \
          If the user is logged in, also retrieve `user`, `view`, `api`, `data`, and `features` information.""")
  public Map<String, Object> info() {
    return infoService.info(request, response);
  }

  @GET
  @Path("theme")
  @Hidden
  public Response getTheme(@QueryParam("name") String name) {
    MetaTheme theme = null;
    final User user = AuthUtils.getUser();
    try {
      theme = Beans.get(MetaThemeService.class).getTheme(name, user);
    } catch (Exception e) {
      // ignore
    }
    String content = Optional.ofNullable(theme).map(MetaTheme::getContent).orElse(null);
    if (StringUtils.notBlank(content)) {
      return Response.ok().entity(content).build();
    }
    return Response.noContent().build();
  }

  @GET
  @Path("logo")
  @Hidden
  public Response getLogoContent(@QueryParam("mode") String mode) {
    return getImageContent(infoService.getLogo(mode));
  }

  @GET
  @Path("sign-in/logo")
  @Hidden
  public Response getSignInLogoContent(@QueryParam("mode") String mode) {
    return getImageContent(infoService.getSignInLogo(mode));
  }

  @GET
  @Path("icon")
  @Hidden
  public Response getIconContent(@QueryParam("mode") String mode) {
    return getImageContent(infoService.getIcon(mode));
  }

  private Response getImageContent(Object image) {
    try {
      if (image instanceof MetaFile metaFile) {
        final Store store = FileStoreFactory.getStore();
        final String filePath = metaFile.getFilePath();

        if (store.hasFile(filePath)) {
          final InputStream inputStream = store.getStream(filePath);
          return Response.ok(inputStream)
              .type(MimeTypesUtils.getContentType(metaFile.getFileName()))
              .build();
        }
      } else if (ObjectUtils.notEmpty(image)) {
        final String path = image.toString();
        final InputStream inputStream = request.getServletContext().getResourceAsStream(path);

        if (inputStream != null) {
          return Response.ok(inputStream).type(MimeTypesUtils.getContentType(path)).build();
        }

        URI uri = new URI(path);
        if (!uri.isAbsolute()) {
          uri =
              UriBuilder.from(AppSettings.get().getBaseURL()).merge(UriBuilder.from(path)).toUri();
        }

        return Response.seeOther(uri).build();
      }
    } catch (Exception e) {
      log.error("Unable to get image content for {}: {}", image, e.getMessage());
    }

    return Response.status(Response.Status.NOT_FOUND).build();
  }
}
