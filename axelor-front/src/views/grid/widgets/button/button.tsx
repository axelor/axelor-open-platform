import { legacyClassNames } from "@/styles/legacy";
import { Box } from "@axelor/ui";
import { useMemo } from "react";
import clsx from "clsx";

import { Field } from "@/services/client/meta.types";
import { Button as ButtonField } from "@/services/client/meta.types";
import { GridCellProps } from "../../builder/types";
import { parseExpression } from "@/hooks/use-parser/utils";
import { useViewAction } from "@/view-containers/views/scope";
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
      <a className={className} href=" " onClick={handleClick} title={help}>
        <Box as="i" me={2} className={legacyClassNames("fa", css, icon)} />
      </a>
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
