package com.axelor.data;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAliasType;
import com.thoughtworks.xstream.annotations.XStreamInclude;

public class XStreamUtils {

  public static XStream createXStream() {
    return setupSecurity(new XStream());
  }

  public static XStream setupSecurity(XStream xStream) {
    XStream.setupDefaultSecurity(xStream);
    // Permission for any type which is annotated with an XStream annotation.
    xStream.addPermission(
        type -> {
          if (type == null) {
            return false;
          }
          return ((Class<?>) type).isAnnotationPresent(XStreamAlias.class)
              || ((Class<?>) type).isAnnotationPresent(XStreamAliasType.class)
              || ((Class<?>) type).isAnnotationPresent(XStreamInclude.class);
        });
    return xStream;
  }
}
