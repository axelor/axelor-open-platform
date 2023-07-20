import { legacyClassNames } from "@/styles/legacy";
import { Box } from "@axelor/ui";
import clsx from "clsx";
import { useMemo } from "react";

import { Icon } from "@/components/icon";
import { parseExpression } from "@/hooks/use-parser/utils";
import { Button as ButtonField, Field } from "@/services/client/meta.types";
import { useViewAction } from "@/view-containers/views/scope";
import { GridCellProps } from "../../builder/types";

import styles from "./button.module.scss";

export function Button(props: GridCellProps) {
  const { record, data: field, onAction } = props;
  const { onClick } = field as ButtonField;
  const { name, icon, css, help } = field as Field;
  const { context } = useViewAction();

  const { hidden, readonly } = useMemo(() => {
    const { showIf, hideIf, readonlyIf } = field as Field;
    const ctx = { ...context, ...record };
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

  function handleClick(e: React.MouseEvent<HTMLElement>) {
    e.preventDefault();
    if (!readonly && onClick && onAction) {
      onAction(onClick, {
        ...record,
        id: record.$$id ?? record.id,
        selected: true,
        _signal: name,
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

  return icon.includes("fa-") ? renderIcon() : renderImage();
}
