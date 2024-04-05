import { useMemo } from "react";
import { Box, clsx } from "@axelor/ui";

import { dialogs } from "@/components/dialogs";
import { Icon } from "@/components/icon";
import { createScriptContext } from "@/hooks/use-parser/context";
import { parseExpression } from "@/hooks/use-parser/utils";
import { Button as ButtonField, Field } from "@/services/client/meta.types";
import { legacyClassNames } from "@/styles/legacy";
import { useViewAction } from "@/view-containers/views/scope";
import { useCanDirty, useFormEditableScope } from "@/views/form/builder/scope";
import { processContextValues } from "@/views/form/builder/utils";
import { GridCellProps } from "../../builder/types";

import styles from "./button.module.scss";

export function Button(props: GridCellProps) {
  const { record, data: field, actionExecutor, onUpdate } = props;
  const { prompt, onClick } = field as ButtonField;
  const { title, name, icon, css, help: _help } = field as Field;
  const help = _help || title;
  const { context } = useViewAction();

  const canDirty = useCanDirty();

  const { hidden, readonly } = useMemo(() => {
    const { showIf, hideIf, readonlyIf } = field as Field;
    const ctx = createScriptContext({ ...context, ...record });
    let hidden: boolean | undefined;
    let readonly = (field as Field).readonly;
    if (showIf) {
      hidden = !parseExpression(showIf)(ctx);
    } else if (hideIf) {
      hidden = !!parseExpression(hideIf)(ctx);
    }
    if (readonlyIf) {
      readonly = !!parseExpression(readonlyIf)(ctx);
    }
    return { hidden, readonly };
  }, [field, record, context]);

  const { commit: commitEditableWidgets } = useFormEditableScope();

  if (!icon || hidden) return null;

  const className = clsx({
    [styles.readonly]: readonly,
  });

  async function handleClick() {
    if (!readonly && actionExecutor) {
      if (prompt) {
        const confirmed = await dialogs.confirm({
          content: prompt,
        });
        if (!confirmed) return;
      }
      if (!onClick) return;
      await commitEditableWidgets();
      await actionExecutor.waitFor();
      const res = await actionExecutor.execute(onClick, {
        context: {
          ...processContextValues(record),
          selected: true,
          _signal: name,
          _ids: undefined,
          ...(record.$$id && {
            id: record.$$id,
            $$id: undefined,
          }),
        },
      });
      const values = res?.reduce?.(
        (obj, { values }) => ({
          ...obj,
          ...values,
        }),
        {},
      );

      const fieldNames = Object.keys(values || {});

      if (fieldNames.length) {
        const _dirty =
          record._dirty || fieldNames.some((name) => canDirty(name));
        onUpdate?.({ ...record, ...values, _dirty });
      }
    }
  }

  function renderIcon() {
    return (
      <Box
        d="inline-flex"
        className={legacyClassNames(className, css)}
        onClick={handleClick}
        title={help}
      >
        {icon && <Icon icon={icon} />}
      </Box>
    );
  }

  function renderImage() {
    return (
      <Box className={className}>
        <img
          title={help}
          height={24}
          className={legacyClassNames(css)}
          src={icon}
          alt={name}
          onClick={handleClick}
        />
      </Box>
    );
  }

  return !icon.includes(".") ? renderIcon() : renderImage();
}
