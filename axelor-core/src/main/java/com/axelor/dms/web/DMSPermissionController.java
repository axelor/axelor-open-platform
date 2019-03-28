package com.axelor.dms.web;

import com.axelor.db.Model;
import com.axelor.dms.db.DMSFile;
import com.axelor.dms.db.DMSPermission;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DMSPermissionController {

  public void setUserDomain(ActionRequest request, ActionResponse response) {
    response.setAttr("user", "domain", getDomain(request, DMSPermission::getUser));
  }

  public void setGroupDomain(ActionRequest request, ActionResponse response) {
    response.setAttr("group", "domain", getDomain(request, DMSPermission::getGroup));
  }

  /**
   * Gets domain string in order to suggest unique users/groups in a DMS file's permissions.
   *
   * @param request
   * @param getter
   * @return
   */
  private String getDomain(ActionRequest request, Function<DMSPermission, Model> getter) {
    final DMSPermission permission = request.getContext().asType(DMSPermission.class);
    final DMSFile file = request.getContext().getParent().asType(DMSFile.class);
    final Model current = getter.apply(permission);
    final String idListString =
        Optional.ofNullable(file.getPermissions())
            .orElse(Collections.emptyList())
            .stream()
            .map(getter)
            .filter(Objects::nonNull)
            .filter(model -> current == null || !current.equals(model))
            .map(Model::getId)
            .map(Number::toString)
            .collect(Collectors.joining(","));
    return String.format("self.id NOT IN (%s)", idListString.isEmpty() ? "0" : idListString);
  }
}
