import SwaggerUI from "swagger-ui-react";

import "swagger-ui-react/swagger-ui.css";
import styles from "./swagger.module.scss";

export function Swagger() {
  const openapiUrl = "ws/openapi";

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

  return (
    <div className={styles.page}>
      <SwaggerUI url={openapiUrl} plugins={[DisableTryItOutPlugin]} />
    </div>
  );
}
