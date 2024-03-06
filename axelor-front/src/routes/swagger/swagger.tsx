import React from 'react';

import SwaggerUI from 'swagger-ui-react';
import 'swagger-ui-react/swagger-ui.css';

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
    <SwaggerUI url={openapiUrl} plugins={[DisableTryItOutPlugin]}/>
  );
}