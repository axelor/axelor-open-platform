import SwaggerUI from "swagger-ui-react";

import { session } from "@/services/client/session";

import {
  CSRF_COOKIE_NAME,
  CSRF_HEADER_NAME,
  readCookie,
} from "@/services/client/client";

import "swagger-ui-react/swagger-ui.css";
import styles from "./swagger.module.scss";

export function Swagger() {
  const openapiUrl = "ws/openapi";

  const { info } = session;
  const { allowTryItOut } = info?.application?.swaggerUI ?? {};

  const DisableTryItOutPlugin = function () {
    return {
      statePlugins: {
        spec: {
          wrapSelectors: {
            allowTryItOutFor: () => () => false,
          },
        },
      },
    };
  };

  const RequestInterceptor = function (req: {
    headers?: Record<string, string>;
  }) {
    const token = readCookie(CSRF_COOKIE_NAME);
    if (req.headers && token) {
      req.headers[CSRF_HEADER_NAME] = token;
    }
    return req;
  };

  return (
    <div className={styles.page}>
      <SwaggerUI
        url={openapiUrl}
        plugins={allowTryItOut ? undefined : [DisableTryItOutPlugin]}
        requestInterceptor={RequestInterceptor}
      />
    </div>
  );
}
