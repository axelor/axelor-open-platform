import clsx from "clsx";
import { useAtomValue } from "jotai";
import { selectAtom } from "jotai/utils";
import { useCallback, useMemo, useState } from "react";

import { Box, Button } from "@axelor/ui";

import format from "@/utils/format";
import { dialogs } from "@/components/dialogs";
import { Icon } from "@/components/icon";
import { Field } from "@/services/client/meta.types";

import { WidgetControl, WidgetProps } from "../../builder";
import { useFormScope, useWidgetState } from "../../builder/scope";
import { useReadonly } from "../button/hooks";

import styles from "./info-button.module.scss";

export function InfoButton(props: WidgetProps) {
  const { schema, widgetAtom, formAtom } = props;
  const { showTitle = true, icon, iconHover, widgetAttrs } = schema;
  const { field = schema.name } = widgetAttrs || {};

  const {
    attrs: { title },
  } = useAtomValue(widgetAtom);
  const { actionExecutor } = useFormScope();
  const record = useAtomValue(
    useMemo(() => selectAtom(formAtom, (form) => form.record), [formAtom])
  );
  const value = record[field];

  const { attrs: fieldSchema = {} } = useWidgetState(formAtom, field ?? "");

  const [wait, setWait] = useState(false);

  const handleClick = useCallback(async () => {
    const { prompt, onClick } = schema;
    if (prompt) {
      const confirmed = await dialogs.confirm({
        content: prompt,
      });
      if (!confirmed) return;
    }
    try {
      setWait(true);
      await actionExecutor.waitFor();
      await actionExecutor.execute(onClick, {
        context: {
          _signal: schema.name,
          _source: schema.name,
        },
      });
    } finally {
      setWait(false);
    }
  }, [actionExecutor, schema]);

  const readonly = useReadonly(widgetAtom);
  const disabled = wait || readonly;

  return (
    <WidgetControl {...props}>
      <Button
        title={title}
        variant="primary"
        className={clsx(styles.button)}
        disabled={disabled}
        onClick={handleClick}
      >
        {icon && (
          <Icon
            icon={icon}
            className={clsx(styles.icon, {
              [styles.hideOnHover]: iconHover,
            })}
          />
        )}
        {iconHover && (
          <Icon
            icon={iconHover}
            className={clsx(styles.iconHover, styles.showOnHover)}
          />
        )}
        <Box className={styles.data}>
          {value && (
            <div className={styles.value}>
              {format(value, {
                context: record,
                props: fieldSchema as unknown as Field,
              })}
            </div>
          )}
          {showTitle && <div className={styles.title}>{title}</div>}
        </Box>
      </Button>
    </WidgetControl>
  );
}
