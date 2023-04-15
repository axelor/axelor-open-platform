import uniqueId from "lodash/uniqueId";

import { Tab, initTab } from "@/hooks/use-tabs";
import { Views } from "@/view-containers/views";
import { useAsync } from "@/hooks/use-async";
import { findActionView } from "@/services/client/meta-cache";
import { Box } from "@axelor/ui";
import { DashletView } from "@/view-containers/view-dashlet";
import { WidgetProps } from "../../builder";
import { useFormScope } from "../../builder/scope";
import { DashletActions } from "./dashlet-actions";
import classes from "./dashlet.module.scss";
import { useCallback, useEffect } from "react";
import { useAtomCallback } from "jotai/utils";

export function Dashlet(props: WidgetProps) {
  const { schema, readonly } = props;
  const { action } = schema;
  const { actionHandler } = useFormScope();

  const { data: tab, state } = useAsync<Tab | null>(async () => {
    const actionView = await findActionView(action);
    const context = actionHandler.getContext();

    return await initTab({
      ...actionView,
      name: uniqueId("$dashlet"),
      params: {
        dashlet: true,
        "show-toolbar": false,
      },
      context: {
        ...actionView.context,
        ...context,
        _id: context.id!,
        _domainAction: action,
      },
    });
  }, [action, actionHandler]);

  const setGridViewProps = useAtomCallback(
    useCallback((get, set, tab: Tab, readonly: boolean = false) => {
      const props = get(tab.state).props;
      const gridProps = props?.grid;
      if (gridProps?.readonly !== readonly) {
        set(tab.state, {
          props: {
            ...props,
            grid: {
              ...gridProps,
              readonly,
            },
          },
        });
      }
    }, [])
  );

  useEffect(() => {
    // for grid view to update readonly to show edit icon
    if (tab && tab?.action?.viewType === 'grid') {
      setGridViewProps(tab, readonly);
    }
  }, [tab, readonly, setGridViewProps]);

  if (state === "loading") return null;

  const { viewType = "" } = tab?.action ?? {};

  return (
    tab && (
      <DashletView>
        <Box
          d="flex"
          flexDirection="column"
          className={classes.container}
          border
          roundedTop
        >
          <Box className={classes.header}>
            <Box className={classes.title}>{tab?.title}</Box>
            <DashletActions viewType={viewType} />
          </Box>
          <Box className={classes.content}>{tab && <Views tab={tab} />}</Box>
        </Box>
      </DashletView>
    )
  );
}
