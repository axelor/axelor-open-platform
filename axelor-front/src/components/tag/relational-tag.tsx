import { useCallback } from "react";

import { Box, clsx } from "@axelor/ui";
import { DataRecord } from "@/services/client/data.types";
import { Schema } from "@/services/client/meta.types";
import { RelationalValue } from "@/views/form/widgets";

import { Tag } from "./tag";

import styles from "./relational-tag.module.scss";

type RelationalTagProps = {
  /**
   * The value
   */
  value: DataRecord;
  /**
   * The field schema
   */
  schema: Schema;
  /**
   * A callback function that gets triggered when a record is removed.
   */
  onRemove?: (record: DataRecord) => void;
  /**
   * An optional callback function triggered when a click event occurs.
   */
  onClick?: (record: DataRecord) => void;
};

/**
 * A React component that renders a relational field as a tag element
 */
export function RelationalTag(props: RelationalTagProps) {
  const { value, schema, onClick, onRemove } = props;
  const { colorField, imageField } = schema;
  const handleClick = useCallback(
    (event: React.MouseEvent<HTMLDivElement>) => {
      event.preventDefault();
      onClick?.(value);
    },
    [onClick, value],
  );

  const handleRemove = useCallback(() => {
    onRemove?.(value);
  }, [onRemove, value]);

  const canOpen = Boolean(onClick);
  const canRemove = Boolean(onRemove);

  return (
    <Tag
      {...(imageField && {
        className: clsx(styles.imageTag, {
          [styles.colorField]: value[colorField],
        }),
      })}
      title={
        canOpen ? (
          <Box
            d="flex"
            alignItems="center"
            className={styles.tagLink}
            onClick={handleClick}
          >
            <RelationalValue schema={schema} value={value} />
          </Box>
        ) : (
          <RelationalValue schema={schema} value={value} />
        )
      }
      color={(colorField ? value[colorField] : "") || "primary"}
      onRemove={canRemove ? handleRemove : undefined}
    />
  );
}
