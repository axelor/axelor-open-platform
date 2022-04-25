package com.axelor.auth.pac4j;

import java.lang.invoke.MethodHandles;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.http.adapter.HttpActionAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorHandler {

  private static final Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public void handleException(
      Exception e, HttpActionAdapter httpActionAdapter, WebContext context) {
    logger.error(e.getMessage());
    logger.debug(e.getMessage(), e);
  }
}
