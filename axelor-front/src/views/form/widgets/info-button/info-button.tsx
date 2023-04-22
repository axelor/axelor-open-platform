import clsx from "clsx";
import { useAtomValue } from "jotai";
import { selectAtom } from "jotai/utils";
import { useCallback, useMemo, useState } from "react";

import { Box, Button } from "@axelor/ui";

import { dialogs } from "@/components/dialogs";
import { Field } from "@/services/client/meta.types";
import { legacyClassNames } from "@/styles/legacy";
import { Formatters } from "@/utils/format";

import { FieldContainer, WidgetProps } from "../../builder";
import { useFormScope } from "../../builder/scope";

import styles from "./info-button.module.scss";

export function InfoButton({ schema, widgetAtom, formAtom }: WidgetProps) {
  const { showTitle = true, icon, iconHover, widgetAttrs } = schema;
  const { field } = widgetAttrs || {};

  const {
    attrs: { title, readonly },
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
      actionExecutor.execute(onClick);
    } finally {
      setWait(false);
    }
  }, [actionExecutor, schema]);

  const disabled = wait || readonly;

  return (
    <FieldContainer>
      <Button
        title={title}
        variant="primary"
        className={clsx(styles.button)}
        disabled={disabled}
        onClick={handleClick}
      >
        <Box
          as="i"
          className={clsx(styles.icon, legacyClassNames("fa", icon), {
            [styles.hideOnHover]: iconHover,
          })}
        />
        {iconHover && (
          <Box
            as="i"
            className={clsx(
              styles.iconHover,
              styles.showOnHover,
              legacyClassNames("fa", iconHover)
            )}
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
    </FieldContainer>
  );
}
