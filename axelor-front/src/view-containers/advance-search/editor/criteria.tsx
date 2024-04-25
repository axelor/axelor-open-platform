import { Box } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";
import { memo } from "react";

import { Select } from "@/components/select";
import { Filter } from "@/services/client/data.types";
import { Field } from "@/services/client/meta.types";

import { Widget } from "./components";
import { useField } from "./utils";

import styles from "./criteria.module.scss";

export const Criteria = memo(function Criteria({
  filter,
  fields,
  index,
  onRemove,
  onChange,
}: {
  index: number;
  filter?: Filter;
  fields?: Field[];
  onChange?: (e: { name: string; value: any }, index: number) => void;
  onRemove?: (index: number) => void;
}) {
  const { fieldName, operator } = filter || {};
  const { field, options } = useField(fields, fieldName);

  function handleRemove() {
    onRemove?.(index);
  }

  function handleChange(e: { name: string; value: any }) {
    onChange?.(e, index);
  }

  return (
    <Box d="flex" alignItems="center" g={2} w={100}>
      <Box d="flex" onClick={handleRemove}>
        <MaterialIcon icon="close" className={styles.icon} />
      </Box>

      <Box d="flex" g={2} className={styles.inputs}>
        {fields && (
          <Select
            className={styles.select}
            multiple={false}
            options={fields}
            optionKey={(x) => x.name}
            optionLabel={(x) => x.title ?? x.autoTitle ?? x.name}
            optionEqual={(x, y) => x.name === y.name}
            onChange={(value) =>
              handleChange({ name: "fieldName", value: value?.name })
            }
            value={field}
          />
        )}
        {Boolean(options?.length) && (
          <Select
            className={styles.select}
            multiple={false}
            options={options}
            optionKey={(x) => x.name}
            optionLabel={(x) => x.title}
            optionEqual={(x, y) => x.name === y.name}
            onChange={(value) =>
              handleChange({ name: "operator", value: value?.name })
            }
            value={options.find((x) => x.name === operator) ?? null}
          />
        )}
        {operator && field && filter && (
          <Widget
            {...{
              operator,
              field,
              filter,
              onChange: handleChange,
              inputProps: {
                className: styles.criteria,
              },
            }}
          />
        )}
      </Box>
    </Box>
  );
});
