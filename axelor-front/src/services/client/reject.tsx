import { useCallback, useMemo, useState } from "react";
import { Box, NavTabItem, NavTabs } from "@axelor/ui";

import { alerts } from "@/components/alerts";
import { dialogs } from "@/components/dialogs";

import { i18n } from "./i18n";

export function reject(report: any) {
  let message;
  let exception;
  let stacktrace;
  let cause;

  const isDev = process.env.NODE_ENV !== "production";

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
    cause = report.cause;
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
        <p dangerouslySetInnerHTML={{ __html: message }} />
        {stacktrace &&
          (isDev ? (
            <ErrorTabs cause={cause} stacktrace={stacktrace} />
          ) : (
            <Box flex={1} overflow="auto">
              <ErrorText error={stacktrace} />
            </Box>
          ))}
      </Box>
    ),
  });

  return Promise.reject(500);
}

function ErrorText({ error }: { error?: string }) {
  return (
    error && (
      <Box
        as="pre"
        style={{ whiteSpace: "normal" }}
        dangerouslySetInnerHTML={{
          __html: error,
        }}
      />
    )
  );
}

function ErrorTabs({
  stacktrace,
  cause,
}: {
  stacktrace?: string;
  cause?: string;
}) {
  const items = useMemo(
    () => [
      {
        id: "1",
        title: i18n.get("Stacktrace"),
      },
      { id: "2", title: i18n.get("Cause") },
    ],
    [],
  );
  const [active, setActive] = useState("1");
  const handleChange = useCallback(
    (item: NavTabItem) => setActive(item.id),
    [],
  );

  const error = active === "1" ? stacktrace : cause;
  return (
    <>
      <NavTabs items={items} active={active} onItemClick={handleChange} />
      <Box flex={1} p={3} overflow="auto">
        <ErrorText error={error} />
      </Box>
    </>
  );
}