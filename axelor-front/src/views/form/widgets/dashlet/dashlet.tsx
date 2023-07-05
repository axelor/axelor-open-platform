import { Box } from "@axelor/ui";
import clsx from "clsx";
import { useAtomCallback } from "jotai/utils";
import uniqueId from "lodash/uniqueId";
import { useCallback, useEffect } from "react";

import { useAsync } from "@/hooks/use-async";
import { Tab, initTab } from "@/hooks/use-tabs";
import { DataContext } from "@/services/client/data.types";
import { findActionView } from "@/services/client/meta-cache";
import { Schema } from "@/services/client/meta.types";
import { DashletView } from "@/view-containers/view-dashlet";
import { useDashletHandlerAtom } from "@/view-containers/view-dashlet/handler";
import { Views } from "@/view-containers/views";
import { useViewTab } from "@/view-containers/views/scope";
import { useAtomValue } from "jotai";
import { WidgetProps } from "../../builder";
import { DashletActions } from "./dashlet-actions";
import classes from "./dashlet.module.scss";

interface DashletProps {
  schema: Schema;
  className?: string;
  readonly?: boolean;
  dashboard?: boolean;
  getContext?: () => DataContext;
  viewId?: number;
  onViewLoad?: (schema: Schema, viewId?: number, viewType?: string) => void;
}

function DashletTitle({ title }: { title?: string }) {
  const dashlet = useAtomValue(useDashletHandlerAtom());
  return <Box className={classes.title}>{dashlet.title || title}</Box>;
}

export function DashletComponent({
  schema,
  className,
  readonly,
  viewId,
  dashboard,
  onViewLoad,
  getContext,
}: DashletProps): any {
  const { title, action, widgetAttrs } = schema;
  const height = schema.height ?? widgetAttrs?.height;
  const { data: tab, state } = useAsync<Tab | null>(async () => {
    const actionView = await findActionView(action);
    const ctx = {
      ...actionView.context,
      ...getContext?.(),
    };
    return await initTab({
      ...actionView,
      name: uniqueId("$dashlet"),
      params: {
        dashlet: true,
        "show-toolbar": false,
        ...actionView.params,
      },
      context: {
        ...ctx,
        _id: (ctx?._id ?? ctx?.id)!,
        _model: ctx.model ?? ctx._model,
        _domainAction: action,
      },
    });
  }, [action, getContext]);

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
          style={{ height }}
        >
          <Box className={classes.header}>
            <DashletTitle title={title || tab?.title} />
            <DashletActions
              dashboard={dashboard}
              viewType={viewType}
              showBars={widgetAttrs?.showBars}
            />
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
  const { schema, readonly, formAtom } = props;
  const tab = useViewTab();

  const getContext = useAtomCallback(
    useCallback(
      (get) => {
        const ctx = formAtom ? get(formAtom).record : {};
        return {
          ...tab.action.context,
          ...ctx,
        } as DataContext;
      },
      [formAtom, tab.action.context]
    )
  );

  return (
    <DashletComponent
      schema={schema}
      readonly={readonly}
      getContext={getContext}
    />
  );
}
