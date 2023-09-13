import { legacyClassNames } from "@/styles/legacy";
import { Box } from "@axelor/ui";
import clsx from "clsx";
import { useMemo } from "react";

import { Icon } from "@/components/icon";
import { parseExpression } from "@/hooks/use-parser/utils";
import { Button as ButtonField, Field } from "@/services/client/meta.types";
import { useViewAction } from "@/view-containers/views/scope";
import { GridCellProps } from "../../builder/types";
import { processContextValues } from "@/views/form/builder/utils";
import { createScriptContext } from "@/hooks/use-parser/context";

import styles from "./button.module.scss";

export function Button(props: GridCellProps) {
  const { record, data: field, actionExecutor } = props;
  const { onClick } = field as ButtonField;
  const { name, icon, css, help } = field as Field;
  const { context } = useViewAction();

  const { hidden, readonly } = useMemo(() => {
    const { showIf, hideIf, readonlyIf } = field as Field;
    const ctx = createScriptContext({ ...context, ...record });
    let hidden: boolean | undefined;
    let readonly: boolean | undefined;
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

  if (!icon || hidden) return null;

  const className = clsx({
    [styles.readonly]: readonly,
  });

  async function handleClick() {
    if (!readonly && onClick && actionExecutor) {
      await actionExecutor.waitFor();
      await actionExecutor.execute(onClick, {
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
