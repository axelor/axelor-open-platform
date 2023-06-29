import clsx from "clsx";
import { useAtomValue } from "jotai";
import { selectAtom } from "jotai/utils";
import { useCallback, useMemo, useState } from "react";

import { Box, Button } from "@axelor/ui";

import { dialogs } from "@/components/dialogs";
import { Field } from "@/services/client/meta.types";
import { Formatters } from "@/utils/format";

import { WidgetControl, WidgetProps } from "../../builder";
import { useFormScope } from "../../builder/scope";
import { useReadonly } from "../button/hooks";

import { Icon } from "@/components/icon";
import styles from "./info-button.module.scss";

export function InfoButton(props: WidgetProps) {
  const { schema, widgetAtom, formAtom } = props;
  const { showTitle = true, icon, iconHover, widgetAttrs } = schema;
  const { field } = widgetAttrs || {};

  const {
    attrs: { title },
  } = useAtomValue(widgetAtom);
  const { actionExecutor } = useFormScope();
  const value = useAtomValue(
    useMemo(
      () => selectAtom(formAtom, (form) => form.record[field!]),
      [formAtom, field]
    )
  );
  const fieldSchema = useAtomValue(
    useMemo(
      () => selectAtom(formAtom, (form) => form.fields[field!]),
      [formAtom, field]
    )
  );

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
        <Icon
          icon={icon}
          className={clsx(styles.icon, {
            [styles.hideOnHover]: iconHover,
          })}
        />
        {iconHover && (
          <Icon
            icon={iconHover}
            className={clsx(styles.iconHover, styles.showOnHover)}
          />
        )}
        <Box className={styles.data}>
          {value && (
            <div className={styles.value}>
              {Formatters.decimal(value, {
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
