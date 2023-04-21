import { useCallback, useEffect } from "react";
import { useAtomCallback } from "jotai/utils";
import { Box } from "@axelor/ui";
import uniqueId from "lodash/uniqueId";
import clsx from "clsx";

import { Tab, initTab } from "@/hooks/use-tabs";
import { Views } from "@/view-containers/views";
import { useAsync } from "@/hooks/use-async";
import { findActionView } from "@/services/client/meta-cache";
import { DashletView } from "@/view-containers/view-dashlet";
import { WidgetProps } from "../../builder";
import { useFormScope } from "../../builder/scope";
import { DashletActions } from "./dashlet-actions";
import { CustomView, Schema } from "@/services/client/meta.types";
import { ActionHandler } from "@/view-containers/action";
import classes from "./dashlet.module.scss";
import { useAtomValue } from "jotai";
import { useDashletHandlerAtom } from "@/view-containers/view-dashlet/handler";

interface DashletProps {
  schema: Schema;
  className?: string;
  readonly?: boolean;
  actionHandler?: ActionHandler;
  viewId?: number;
  onViewLoad?: (schema: Schema, viewId?: number, viewType?: string) => void;
}

export function DashletComponent({
  schema,
  className,
  readonly,
  actionHandler,
  viewId,
  onViewLoad,
}: DashletProps): any {
  const { action } = schema;

  const { data: tab, state } = useAsync<Tab | null>(async () => {
    const actionView = await findActionView(action);
    const context = actionHandler?.getContext();

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
        _id: context?.id!,
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
    if (tab && tab?.action?.viewType === "grid") {
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
          className={clsx(classes.container, className)}
          border
          roundedTop
        >
          <Box className={classes.header}>
            <Box className={classes.title}>{tab?.title}</Box>
            <DashletActions viewType={viewType} />
            {viewType && onViewLoad && (
              <DashletViewLoad
                schema={schema}
                viewId={viewId}
                viewType={viewType}
                onViewLoad={onViewLoad}
              />
            )}
          </Box>
          <Box className={classes.content}>{tab && <Views tab={tab} />}</Box>
        </Box>
      </DashletView>
    )
  );
}

function DashletViewLoad({
  schema,
  viewId,
  viewType,
  onViewLoad,
}: Pick<DashletProps, "schema" | "viewId" | "onViewLoad"> & {
  viewType: string;
}) {
  const { view } = useAtomValue(useDashletHandlerAtom());
  const $viewType = view?.type || viewType;

  useEffect(() => {
    if ($viewType && onViewLoad) {
      onViewLoad(schema, viewId, $viewType);
    }
  }, [schema, viewId, onViewLoad, $viewType]);

  return null;
}

export function Dashlet(props: WidgetProps) {
  const { schema, readonly } = props;
  const { actionHandler } = useFormScope();
  return (
    <DashletComponent
      schema={schema}
      readonly={readonly}
      actionHandler={actionHandler}
    />
  );
}
