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
import { i18n } from "@/services/client/i18n";
import classes from "./card.module.scss";

export const Card = memo(function Card({
  record,
  fields,
  getContext,
  onEdit,
  onView,
  onDelete,
  Template,
  width,
  minWidth,
}: {
  record: DataRecord;
  fields?: any;
  getContext?: () => DataContext;
  onEdit?: (record: DataRecord) => void;
  onView?: (record: DataRecord) => void;
  onDelete?: (record: DataRecord) => void;
  Template: FunctionComponent<{
    context: DataContext;
    options?: EvalContextOptions;
  }>;
  width?: string;
  minWidth?: string;
}) {
  // state to store updated action values
  const [values, setValues] = useState<DataRecord>({});

  const { context, actionExecutor } = useMemo(() => {
    const $record = { ...record, ...values };
    const context = { ...getContext?.(), ...$record };
    const actionExecutor = new DefaultActionExecutor(
      new FormActionHandler((options?: DataContext) => context)
    );
    return { context, actionExecutor };
  }, [getContext, record, values]);

  function handleClick() {
    onView?.(record);
  }

  const execute = useCallback(
    async (action: string, options?: ActionOptions) => {
      const res = await actionExecutor.execute(action, options);
      const values = res?.reduce?.(
        (obj, { values }) => ({
          ...obj,
          ...values,
        }),
        {}
      );
      values && setValues(values);
    },
    [actionExecutor]
  );

  const commandItems: CommandItemProps[] = [
    {
      key: "menu",
      iconProps: { icon: "arrow_drop_down" },
      items: [
        {
          key: "edit",
          text: i18n.get("Edit"),
          hidden: !Boolean(onEdit),
          onClick: () => onEdit?.(record),
        },
        {
          key: "delete",
          text: i18n.get("Delete"),
          hidden: !Boolean(onDelete),
          onClick: () => onDelete?.(record),
        },
      ],
    },
  ];

  // reset values on record update(fetch)
  useEffect(() => {
    setValues({});
  }, [record]);

  const showActions = onEdit || onDelete;
  return (
    <>
      <Box
        d="flex"
        px={{base: 1, md: 2}}
        mb={{base: 2, md: 3}}
        className={classes.card}
        style={{
          width,
          minWidth,
        }}
      >
        <Box
          p={{base: 2, md: 3}}
          bgColor="body"
          w={100}
          border
          rounded
          shadow="sm"
          onClick={handleClick}
        >
          <Template
            context={context}
            options={{
              execute,
              fields,
            }}
          />
        </Box>
        {showActions && (
          <CommandBar className={classes.menuIcon} items={commandItems} />
        )}
      </Box>
    </>
  );
});
