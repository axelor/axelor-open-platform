import { Box, clsx } from "@axelor/ui";

import { dialogs } from "@/components/dialogs";
import { Icon } from "@/components/icon";
import {
  Button as ButtonField,
  Field,
  JsonField,
} from "@/services/client/meta.types";
import { ActionResult } from "@/services/client/meta";
import { DataRecord } from "@/services/client/data.types";
import { useCanDirty, useFormEditableScope } from "@/views/form/builder/scope";
import { legacyClassNames } from "@/styles/legacy";
import { processContextValues } from "@/views/form/builder/utils";
import { executeWithoutQueue } from "@/view-containers/action";

import { GridCellProps } from "../../builder/types";
import { useButtonProps } from "./utils";

import styles from "./button.module.scss";

export function Button(props: GridCellProps) {
  const { record, data: field, actionExecutor, onUpdate } = props;
  const { prompt, onClick } = field as ButtonField;
  const { title, name, icon, css, help: _help, jsonPath } = field as JsonField;
  const help = _help || title;

  const canDirty = useCanDirty();

  const { hidden, readonly } = useButtonProps(field as Field, record);

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

      let values: Partial<DataRecord> = {};

      async function handleSave() {
        const fieldNames = Object.keys(values || {});

        if (fieldNames.length) {
          const _dirty =
            record._dirty || fieldNames.some((key) => canDirty(key));

          const res = await onUpdate?.({ ...record, ...values, _dirty });

          values = {};

          return res;
        }
      }

      await commitEditableWidgets();
      await actionExecutor.waitFor();
      await actionExecutor.execute(onClick, {
        context: {
          ...processContextValues(record),
          selected: true,
          _signal: jsonPath ?? name,
          _ids: undefined,
          ...(record.$$id && {
            id: record.$$id,
            $$id: undefined,
          }),
        },
        handle: async (result: ActionResult) => {
          if (result?.values) {
            values = { ...values, ...result.values };
          }
          if (result?.save) {
            await executeWithoutQueue(handleSave);
            values = {};
          }
          return Promise.resolve();
        },
      });

      // if any values updates are pending then save to record
      await handleSave();
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
