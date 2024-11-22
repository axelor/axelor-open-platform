import { useCallback, useRef, useState } from "react";
import { Box, Button, Popper } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";
import { i18n } from "@/services/client/i18n";
import { axelor } from "@/utils/globals";

import styles from "./errorbox.module.scss";

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
    (statusText ?? status === 404)
      ? i18n.get("Page not found")
      : i18n.get("Unexpected error occurred");

  const handleDetails = useCallback(() => {
    axelor.dialogs.box({
      title: i18n.get("Error"),
      yesNo: false,
      content: (
        <Box as="pre" flex={1} p={2} style={{ margin: "-1rem" }}>
          {error?.stack}
        </Box>
      ),
      size: "lg",
      footer: () => {
        return error?.stack && <ErrorBoxFooter error={error} />;
      },
    });
  }, [error]);

  return (
    <Box d="flex" flex={1} className={styles.content}>
      <Box textAlign="center">
        <Box as="p" fontSize={5}>
          <Box as="span" color="danger">
            {i18n.get("Opps!")}
          </Box>{" "}
          {message}
        </Box>
        <Box as="p" fontSize={6}>
          <Box as="span">
            {i18n.get(
              "Try to refresh this page or contact your support service if the problem persists.",
            )}
          </Box>
        </Box>
        {status && (
          <Box as="p" fontSize={6}>
            <Box as="span">
              <b>{i18n.get("Error code :")}</b> {status}
            </Box>
          </Box>
        )}
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

function ErrorBoxFooter({ error }: { error: Error }) {
  const divRef = useRef<HTMLInputElement>(null);
  const [copied, setCopied] = useState(false);

  const copyStackTrace = useCallback(async () => {
    if (!error?.stack) {
      return;
    }
    try {
      await navigator.clipboard.writeText(error?.stack);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (e) {
      // ignore
    }
  }, [error]);

  return (
    <Box d="flex" flex={1}>
      <Button outline variant="primary" onClick={copyStackTrace}>
        <Box d="flex" as="span" ref={divRef}>
          <MaterialIcon icon="content_copy" />
          {i18n.get("Copy to clipboard")}
        </Box>
      </Button>
      <Popper
        placement="bottom"
        open={copied}
        target={divRef.current}
        offset={[0, 4]}
        shadow
        arrow
        rounded
      >
        <Box className={styles.copyToClipboard}>{i18n.get("Copied")}</Box>
      </Popper>
    </Box>
  );
}
