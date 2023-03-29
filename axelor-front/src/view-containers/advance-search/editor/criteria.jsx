import React from "react";
import { Box } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";

import { Select, Widget } from "./components";
import { useField } from "./utils";
import styles from "./criteria.module.css";

export function Criteria({ t, value, fields, index, onRemove, onChange }) {
  const { fieldName, operator } = value || {};
  const { type, field, options } = useField(fields, fieldName, t);

  function handleRemove() {
    onRemove(index);
  }

  function handleChange(data) {
    onChange(data, index);
  }

  function renderSelect(name, options) {
    return (
      <Select
        name={name}
        onChange={(value) => handleChange({ name, value })}
        value={value[name]}
        options={options}
      />
    );
  }

  return (
    <Box d="flex" alignItems="center" mb={1}>
      <Box d="flex" me={1} onClick={handleRemove} >
        <MaterialIcon icon="close" />
      </Box>

      {renderSelect("fieldName", fields)}

      {renderSelect("operator", options)}

      {operator && (
        <Widget
          {...{
            t,
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
  );
}

Criteria.defaultProps = {
  t: (e) => e,
};

export default React.memo(Criteria);
