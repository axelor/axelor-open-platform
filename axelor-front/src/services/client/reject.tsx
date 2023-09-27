import { alerts } from "@/components/alerts";
import { dialogs } from "@/components/dialogs";

import { i18n } from "./i18n";
import { Box } from "@axelor/ui";

export function reject(report: any) {
  let message;
  let exception;
  let stacktrace;

  if (report.popup) {
    message =
      report.message ||
      i18n.get("A server error occurred. Please contact the administrator.");
    dialogs.error({
      title: report.title,
      content: message,
    });
    return Promise.reject(500);
  }

  if (report.stacktrace) {
    message = report.message || report.string;
    exception = report["class"] || "";

    if (
      exception.match(/(OptimisticLockException|StaleObjectStateException)/)
    ) {
      message =
        "<b>" + i18n.get("Concurrent updates error") + "</b><br>" + message;
    }

    stacktrace = report.stacktrace;
  } else if (report.message) {
    message = "<p>" + report.message.replace("\n", "<br>") + "</p>";
    alerts.error({ message });
    return Promise.reject(500);
  } else if (typeof report === "string") {
    stacktrace = report.replace(/.*<body>|<\/body>.*/g, "");
  } else {
    return Promise.reject(500); // no error report, so ignore
  }

  // TODO: show error dialog with stacktrace
  dialogs.error({
    title: report.title || i18n.get("Error"),
    size: "lg",
    content: (
      <Box d="flex" flexDirection="column" style={{ minWidth: 0 }}>
        <p>{message}</p>
        {stacktrace && (
          <Box flex={1} overflow="auto">
            <pre>{stacktrace}</pre>
          </Box>
        )}
      </Box>
    ),
  });

  return Promise.reject(500);
}
