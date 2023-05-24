import {
  MouseEvent,
  FunctionComponent,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  memo,
} from "react";
import { Button, Box, Menu, MenuItem } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";
import clsx from "clsx";

import { DataContext, DataRecord } from "@/services/client/data.types";
import { FormActionHandler } from "../form/builder/scope";
import { ActionOptions, DefaultActionExecutor } from "@/view-containers/action";
import { EvalContextOptions } from "@/hooks/use-parser/eval-context";
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
  const [showMenu, setShowMenu] = useState(false);
  const menuIconRef = useRef<HTMLButtonElement | null>(null);

  const { context, actionExecutor } = useMemo(() => {
    const $record = { ...record, ...values };
    const context = { ...getContext?.(), record: $record, ...$record };
    const actionExecutor = new DefaultActionExecutor(
      new FormActionHandler((options?: DataContext) => context)
    );
    return { context, actionExecutor };
  }, [getContext, record, values]);

  function handleMenuOpen() {
    setShowMenu(true);
  }

  function handleMenuClose() {
    setShowMenu(false);
  }

  function handleClick(e: MouseEvent<HTMLDivElement>) {
    const iconEl = menuIconRef.current;
    // check menu icon click
    if (e.target === iconEl || iconEl?.contains?.(e.target as Node)) {
      handleMenuOpen();
    } else {
      onView?.(record);
    }
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

  // reset values on record update(fetch)
  useEffect(() => {
    setValues({});
  }, [record]);

  const hasMenu = onEdit || onDelete;
  return (
    <>
      <Box
        d="flex"
        px={2}
        mb={3}
        className={classes.card}
        onClick={handleClick}
        style={{
          width,
          minWidth,
        }}
      >
        <Box p={3} bgColor="light" w={100} rounded shadow>
          <Template
            context={context}
            options={{
              execute,
              fields,
            }}
          />
          {hasMenu && (
            <Box
              className={clsx(classes.menuIcon, {
                [classes.show]: showMenu,
              })}
            >
              <Button ref={menuIconRef} variant="link" p={0} d="inline-flex">
                <MaterialIcon icon="arrow_drop_down" />
              </Button>
            </Box>
          )}
        </Box>
      </Box>
      {hasMenu && (
        <Menu
          placement="bottom-end"
          show={showMenu}
          target={menuIconRef.current}
          onHide={handleMenuClose}
          offset={[0, -5]}
        >
          {onEdit && (
            <MenuItem
              onClick={() => {
                handleMenuClose();
                onEdit(record);
              }}
            >
              {i18n.get("Edit")}
            </MenuItem>
          )}
          {onDelete && (
            <MenuItem
              onClick={() => {
                handleMenuClose();
                onDelete(record);
              }}
            >
              {i18n.get("Delete")}
            </MenuItem>
          )}
        </Menu>
      )}
    </>
  );
});
