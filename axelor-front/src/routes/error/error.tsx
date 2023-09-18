import { useCallback } from "react";
import { isRouteErrorResponse, useRouteError } from "react-router-dom";

import { ErrorBox } from "@/components/error-box";
import { useRoute } from "@/hooks/use-route";
import { i18n } from "@/services/client/i18n";
import { Box } from "@axelor/ui";

export function ErrorPage() {
  const error = useRouteError();
  const { redirect } = useRoute();

  const status = isRouteErrorResponse(error) ? error.status : 500;
  const resetTitle = status === 404 ? i18n.get("Home page") : i18n.get("Reload");

  const handleReset = useCallback(() => {
    if (status === 404) {
      redirect("/");
    } else {
      window.location.reload();
    }
  }, [redirect, status]);

  return (
    <Box d="flex" vh={100}>
      <ErrorBox status={status} resetTitle={resetTitle} onReset={handleReset} />
    </Box>
  );
}
