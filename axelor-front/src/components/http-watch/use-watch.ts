import { i18n } from "@/services/client/i18n";
import { session } from "@/services/client/session";
import { $request, $use } from "@/services/http";
import { useCallback, useEffect, useState } from "react";
import { alerts } from "../alerts";

type Pending = () => Promise<any>;

function isLogin(input: RequestInfo | URL) {
  const url = input instanceof Request ? input.url : input.toString();
  return url.startsWith(`./callback`) || url.startsWith("callback");
}

export function useHttpWatch() {
  const [count, setCount] = useState(0);
  const [pending, setPending] = useState<Pending[]>([]);

  const resume = useCallback(() => {
    const all = [...pending];
    setPending([]);
    all.forEach((fn) => fn());
  }, [pending]);

  useEffect(() => {
    return $use(async (args, next) => {
      // Don't intercept:
      // - Request with silent option
      // - Login request
      if (args.options?.silent || isLogin(args.input)) return next();

      setCount((count) => count + 1);
      try {
        const res = await next();
        if (res instanceof Response) {
          if (res.status === 401) {
            if (res.headers.get("Content-Type")?.includes("application/json")) {
              const content = await res.json();
              // Reload page on any non-zero status.
              if (content.status) {
                window.location.reload();
                return;
              }
            }
            setCount((count) => count - 1);
            return new Promise((resolve, reject) => {
              setPending((pending) => [
                ...pending,
                () => $request(args.input, args.init).then(resolve, reject),
              ]);
            });
          } else if (res.status === 403) {
            alerts.error({
              title: i18n.get("Access Error"),
              message: i18n.get(
                "You are not authorized to access this resource.",
              ),
            });
          } else if (res.status >= 500) {
            const contentType = res.headers.get("Content-Type");

            // Response has HTML content, so we show it as full page.
            if (contentType?.includes("text/html")) {
              const html = await res.text();
              if (html.trim()) {
                document.open();
                try {
                  html.split("\n").forEach((line) => document.writeln(line));
                } finally {
                  document.close();
                }
                return;
              }
            }

            // Show alert when response has no HTML content.
            const technical = session.info?.user?.technical;
            const message = i18n.get(
              "An error has occurred{0}. Please contact your administrator.",
              technical ? ` (${res.status})` : "",
            );

            alerts.error({ message });
          }
        }
        return res;
      } finally {
        setCount((count) => count - 1);
      }
    });
  }, []);

  return pending.length ? { count, resume } : { count };
}
