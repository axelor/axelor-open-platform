import { dialogs } from "@/components/dialogs";

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

export function reject(data: number | string | ErrorReport) {
  if (typeof data === "number") {
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

  dialogs.box({
    title,
    content: message,
    yesNo: false,
    footer: () => (
      <Box flex={1}>
        {stacktrace && canShowStack && (
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

  return Promise.reject(500);
}
