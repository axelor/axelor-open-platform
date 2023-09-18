import { i18n } from "@/services/client/i18n";
import { axelor } from "@/utils/globals";
import { Box, Button } from "@axelor/ui";
import { useCallback } from "react";

export type ErrorProxProps = {
  status: number;
  statusText?: string;
  error?: Error;
  detailsTitle?: string;
  resetTitle?: string;
  onReset?: () => void;
};

export function ErrorBox({
  status,
  statusText,
  error,
  detailsTitle = i18n.get("Details"),
  resetTitle = i18n.get("Reset"),
  onReset,
}: ErrorProxProps) {
  const message =
    statusText ?? status === 404
      ? i18n.get("Page not found")
      : i18n.get("Unexpected error occurred");

  const handleDetails = useCallback(() => {
    axelor.dialogs.error({
      content: (
        <Box as="pre" flex={1} p={2} style={{ margin: "-1rem" }}>
          {error?.stack}
        </Box>
      ),
      size: "lg",
    });
  }, [error?.stack]);

  return (
    <Box d="flex" flex={1} alignItems="center" justifyContent="center">
      <Box textAlign="center">
        <Box as="h1" fontWeight="bold">
          {status}
        </Box>
        <Box as="p" fontSize={5}>
          <Box as="span" color="danger">
            {i18n.get("Opps!")}
          </Box>{" "}
          {message}
        </Box>
        <Box
          d="flex"
          g={2}
          flex={1}
          alignItems="center"
          justifyContent="center"
        >
          {error && (
            <Button variant="secondary" onClick={handleDetails}>
              {detailsTitle}
            </Button>
          )}
          {onReset && (
            <Button variant="primary" onClick={onReset}>
              {resetTitle}
            </Button>
          )}
        </Box>
      </Box>
    </Box>
  );
}
