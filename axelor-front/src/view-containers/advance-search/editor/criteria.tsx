import React, { memo } from "react";
import { Box } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { Select, Widget } from "./components";
import { useField } from "./utils";
import { Filter } from "@/services/client/data.types";
import { Field, JsonField } from "@/services/client/meta.types";
import styles from "./criteria.module.scss";

export const Criteria = memo(function Criteria({
  value,
  fields,
  index,
  onRemove,
  onChange,
}: {
  index: number;
  value?: Filter;
  fields?: Field[];
  onChange?: (e: { name: string; value: any }, index: number) => void;
  onRemove?: (index: number) => void;
}) {
  const { fieldName, operator } = value || {};
  const { type, field, options } = useField(fields, fieldName);

  function handleRemove() {
    onRemove?.(index);
  }

  function handleChange(e: { name: string; value: any }) {
    onChange?.(e, index);
  }

  function renderSelect(
    name: keyof Filter,
    options: { name: string; title: string }[]
  ) {
    const $value = value?.[name];
    const selectValue =
      !(field as JsonField)?.jsonField && name === "fieldName" && $value
        ? $value.split(".")[0]
        : $value;
    return (
      <Select
        name={name}
        onChange={(value: string) => handleChange({ name, value })}
        value={selectValue}
        options={options}
      />
    );
  }

  return (
    <Box d="flex" alignItems="center" g={2} w={100}>
      <Box d="flex" onClick={handleRemove}>
        <MaterialIcon icon="close" className={styles.icon} />
      </Box>

      <Box d="flex" g={2} className={styles.inputs}>
        {fields &&
          renderSelect(
            "fieldName",
            fields as { name: string; title: string }[]
          )}

        {renderSelect("operator", options)}

        {operator && (
          <Widget
            {...{
              operator,
              type,
              field,
              value,
              onChange: handleChange,
              className: styles.criteria,
            }}
          />
        )}
      </Box>
    </Box>
  );
});
