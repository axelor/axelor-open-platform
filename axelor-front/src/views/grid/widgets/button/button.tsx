import { legacyClassNames } from "@/styles/legacy";
import { Box } from "@axelor/ui";

import { Field } from "@/services/client/meta.types";
import { Button as ButtonField } from "@/services/client/meta.types";
import { GridCellProps } from "../../builder/types";

export function Button(props: GridCellProps) {
  const { record, data: field, onAction } = props;
  const { onClick } = field as ButtonField;
  const { name, icon, css, help } = field as Field;

  if (!icon) return null;

  function handleClick(e: React.MouseEvent<HTMLElement>) {
    e.preventDefault();
    if (onClick && onAction) {
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
      <a href=" " onClick={handleClick} title={help}>
        <Box as="i" me={2} className={legacyClassNames("fa", css, icon)} />
      </a>
    );
  }

  function renderImage() {
    return (
      <Box>
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
