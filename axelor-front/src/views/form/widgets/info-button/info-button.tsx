import clsx from "clsx";
import { useAtomValue } from "jotai";
import { focusAtom } from "jotai-optics";
import { useCallback, useMemo } from "react";

import { Box, Button } from "@axelor/ui";

import { dialogs } from "@/components/dialogs";
import { legacyClassNames } from "@/styles/legacy";

import { FieldContainer, WidgetProps } from "../../builder";
import { useFormScope } from "../../builder/scope";

import styles from "./info-button.module.scss";

export function InfoButton({ schema, readonly, formAtom }: WidgetProps) {
  const { title, icon, iconHover, widgetAttrs } = schema;
  const { field } = widgetAttrs || {};

  const { actionExecutor } = useFormScope();
  const value = useAtomValue(
    useMemo(
      () => focusAtom(formAtom, (o) => o.prop("record").prop(field!)),
      [formAtom, field]
    )
  );

  const handleClick = useCallback(async () => {
    const { prompt, onClick } = schema;
    if (prompt) {
      const confirmed = await dialogs.confirm({
        content: prompt,
      });
      if (!confirmed) return;
    }
    actionExecutor.execute(onClick);
  }, [actionExecutor, schema]);

  return (
    <FieldContainer readonly={readonly}>
      <Button
        title={title}
        variant="primary"
        className={clsx(styles.button)}
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
          {value && <div className={styles.value}>{value}</div>}
          <div className={styles.title}>{title}</div>
        </Box>
      </Button>
    </FieldContainer>
  );
}
