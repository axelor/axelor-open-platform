import SwaggerUI from "swagger-ui-react";
import { clsx } from "@axelor/ui/core";

import { useAppSettings } from "@/hooks/use-app-settings";
import {
  CSRF_COOKIE_NAME,
  CSRF_HEADER_NAME,
  readCookie,
} from "@/services/client/client";

import { session } from "@/services/client/session";
import "swagger-ui-react/swagger-ui.css";
import styles from "./swagger.module.scss";

const OPENAPI_URL = "ws/openapi";

const DisableTryItOutPlugin = () => ({
  statePlugins: {
    spec: {
      wrapSelectors: {
        allowTryItOutFor: () => () => false,
      },
    },
  },
});

const RequestInterceptor = (req: { headers?: Record<string, string> }) => ({
  ...req,
  headers: {
    ...req.headers,
    [CSRF_HEADER_NAME]: readCookie(CSRF_COOKIE_NAME),
  },
});

export function Swagger() {
  const { info } = session;
  const { enabled, allowTryItOut } = info?.application?.swaggerUI ?? {};
  const { themeMode } = useAppSettings();

  return (
    enabled && (
      <div className={clsx(styles.page, `swagger-theme-${themeMode}`)}>
        <SwaggerUI
          url={OPENAPI_URL}
          plugins={allowTryItOut ? undefined : [DisableTryItOutPlugin]}
          requestInterceptor={RequestInterceptor}
        />
      </div>
    )
  );
}
