import { dialogs } from "@/components/dialogs";

import { alerts } from "@/components/alerts";
import { Box, Button } from "@axelor/ui";
import { i18n } from "./i18n";
import { session } from "./session";

export type ErrorReport = {
  title?: string;
  message?: string;
  causeClass?: string;
  causeMessage?: string;
  causeStack?: string;
  entityId?: number;
  entityName?: string;
  constraints?: Record<string, string>;
};

export type RejectType = number | string | ErrorReport | null;

export function reject(data: RejectType) {
  return handleReject(data);
}

export function rejectAsAlert(data: RejectType) {
  return handleReject(data, showAlert);
}

function handleReject(data: RejectType, showError = showDialog) {
  if (typeof data === "number" || data == null) {
    return Promise.reject(data);
  }

  const report = typeof data === "string" ? { message: data } : data;

  const title = report.title || i18n.get("Internal Server Error");
  const message = (report.message ?? report.causeMessage)?.replace(
    "\n",
    "<br>",
  );
  const stacktrace =
    report.causeStack ??
    (typeof data === "string" ? data.replace(/.*<body>|<\/body>.*/g, "") : "");

  const canShowStack = !!session.info?.user?.technical;

  showError({
    title,
    message,
    stacktrace: canShowStack ? stacktrace : undefined,
  });

  return Promise.reject(500);
}

const showDialog = ({
  title,
  message,
  stacktrace,
}: {
  title: string;
  message?: string;
  stacktrace?: string;
}) => {
  dialogs.box({
    title,
    content: message,
    yesNo: false,
    footer: () => (
      <Box flex={1}>
        {stacktrace && (
          <Button
            variant="secondary"
            onClick={() =>
              dialogs.box({
                title,
                content: (
                  <Box as="pre" p={2} m={0}>
                    {stacktrace}
                  </Box>
                ),
                size: "xl",
                yesNo: false,
                padding: "0",
              })
            }
          >
            {i18n.get("Details...")}
          </Button>
        )}
      </Box>
    ),
  });
};

const showAlert = ({ title, message }: { title: string; message?: string }) => {
  alerts.error({ title, message });
};
