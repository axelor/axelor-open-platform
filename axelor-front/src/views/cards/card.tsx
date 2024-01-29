import {
  FunctionComponent,
  useCallback,
  useEffect,
  useMemo,
  useState,
  memo,
} from "react";
import { Box, CommandItemProps, CommandBar } from "@axelor/ui";

import { DataContext, DataRecord } from "@/services/client/data.types";
import { FormActionHandler } from "../form/builder/scope";
import { ActionOptions, DefaultActionExecutor } from "@/view-containers/action";
import { EvalContextOptions } from "@/hooks/use-parser/context";
import { MetaData } from "@/services/client/meta";
import { i18n } from "@/services/client/i18n";
import { useViewAction } from "@/view-containers/views/scope";
import classes from "./card.module.scss";

export function CardTemplate({
  component: TemplateComponent,
  record,
  fields,
  onRefresh,
}: {
  component: FunctionComponent<{
    context: DataContext;
    options?: EvalContextOptions;
  }>;
  record: DataRecord;
  fields?: MetaData["fields"];
  onRefresh?: () => Promise<any>;
}) {
  // state to store updated action values
  const [values, setValues] = useState<DataRecord>({});
  const action = useViewAction();

  const getContext = useCallback(
    () => ({
      ...action.context,
      _model: action.model,
      _viewName: action.name,
      _viewType: action.viewType,
      _views: action.views,
    }),
    [action.model, action.context],
  );

  const { context, actionExecutor } = useMemo(() => {
    const $record = { ...record, ...values };
    const context = { ...getContext?.(), ...$record };
    const actionHandler = new FormActionHandler(() => context);

    onRefresh && actionHandler.setRefreshHandler(onRefresh);

    const actionExecutor = new DefaultActionExecutor(actionHandler);
    return { context, actionExecutor };
  }, [getContext, onRefresh, record, values]);

  const execute = useCallback(
    async (action: string, options?: ActionOptions) => {
      const res = await actionExecutor.execute(action, options);
      const values = res?.reduce?.(
        (obj, { values }) => ({
          ...obj,
          ...values,
        }),
        {},
      );
      values && setValues(values);
    },
    [actionExecutor],
  );

  // reset values on record update(fetch)
  useEffect(() => {
    setValues({});
  }, [record]);

  return (
    <TemplateComponent
      context={context}
      options={{
        execute,
        fields,
      }}
    />
  );
}

export const Card = memo(function Card({
  record,
  fields,
  onEdit,
  onView,
  onDelete,
  onRefresh,
  Template,
  width,
  minWidth,
}: {
  record: DataRecord;
  fields?: any;
  onEdit?: (record: DataRecord) => void;
  onView?: (record: DataRecord) => void;
  onDelete?: (record: DataRecord) => void;
  onRefresh?: () => Promise<any>;
  Template: FunctionComponent<{
    context: DataContext;
    options?: EvalContextOptions;
  }>;
  width?: string;
  minWidth?: string;
}) {
  function handleClick() {
    onView?.(record);
  }

  const commandItems: CommandItemProps[] = [
    {
      key: "menu",
      iconProps: { icon: "arrow_drop_down" },
      items: [
        {
          key: "edit",
          text: i18n.get("Edit"),
          hidden: !onEdit,
          onClick: () => onEdit?.(record),
        },
        {
          key: "delete",
          text: i18n.get("Delete"),
          hidden: !onDelete,
          onClick: () => onDelete?.(record),
        },
      ],
    },
  ];

  const showActions = onEdit || onDelete;
  return (
    <>
      <Box
        d="flex"
        px={{ base: 1, md: 2 }}
        mb={{ base: 2, md: 3 }}
        className={classes.card}
        style={{
          width,
          minWidth,
        }}
      >
        <Box
          p={{ base: 2, md: 3 }}
          bgColor="body"
          w={100}
          border
          rounded
          shadow="sm"
          onClick={handleClick}
        >
          <CardTemplate
            component={Template}
            record={record}
            fields={fields}
            onRefresh={onRefresh}
          />
        </Box>

        {showActions && (
          <CommandBar className={classes.menuIcon} items={commandItems} />
        )}
      </Box>
    </>
  );
});
