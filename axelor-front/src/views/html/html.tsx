import { useMemo } from "react";

import { Box } from "@axelor/ui";

import { useExpression } from "@/hooks/use-parser";
import { useViewContext } from "@/view-containers/views/scope";
import { HtmlView } from "@/services/client/meta.types";
import { ViewProps } from "../types";

import styles from "./html.module.scss";

export function Html(props: ViewProps<HtmlView>) {
  const {
    meta: { view },
  } = props;
  const name = view.name || view.resource;
  const parseURL = useExpression(name!);
  const getContext = useViewContext();

  const url = useMemo(() => {
    let url = `${name}`;

    if (!url) return "";

    if (url && url.indexOf("{{") > -1) {
      url = parseURL(getContext() ?? {});
    }

    const stamp = new Date().getTime();

    return `${url}${url.includes("?") ? "&" : "?"}${stamp}`;
  }, [name, getContext, parseURL]);

  return (
    <Box flexGrow={1} position="relative" className={styles.container}>
      <Box d="flex" position="absolute" className={styles.frame}>
        {url && (
          <Box
            as="iframe"
            title="HTML View"
            frameBorder="0"
            scrolling="auto"
            src={url}
            w={100}
            h={100}
            flex={1}
          />
        )}
      </Box>
    </Box>
  );
}
