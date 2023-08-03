import { useEffect, useMemo, useReducer } from "react";
import { useSetAtom } from "jotai";

import { Box } from "@axelor/ui";

import { HtmlView } from "@/services/client/meta.types";
import { useExpression } from "@/hooks/use-parser";
import {
  useViewContext,
  useViewTab,
  useViewTabRefresh,
} from "@/view-containers/views/scope";
import { useDashletHandlerAtom } from "@/view-containers/view-dashlet/handler";

import { ViewProps } from "../types";
import styles from "./html.module.scss";

export function Html(props: ViewProps<HtmlView>) {
  const {
    meta: { view },
  } = props;
  const name = view.name || view.resource;
  const parseURL = useExpression(name!);
  const getContext = useViewContext();
  const [updateCount, onRefresh] = useReducer((x) => x + 1, 0);

  const { dashlet } = useViewTab();
  const setDashletHandlers = useSetAtom(useDashletHandlerAtom());

  const url = useMemo(() => {
    let url = `${name}`;

    if (!url) return "";

    if (url && url.indexOf("{{") > -1) {
      url = parseURL(getContext() ?? {});
    }

    return url;
  }, [name, getContext, parseURL]);

  useEffect(() => {
    if (dashlet) {
      setDashletHandlers({
        view,
        onRefresh: onRefresh as any,
      });
    }
  }, [dashlet, view, onRefresh, setDashletHandlers]);

  // register tab:refresh
  useViewTabRefresh("html", onRefresh);

  const key = `${url}_${updateCount}`;

  return (
    <Box flexGrow={1} position="relative" className={styles.container}>
      <Box d="flex" position="absolute" className={styles.frame}>
        {url && (
          <Box
            key={key}
            as="iframe"
            title="HTML View"
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
