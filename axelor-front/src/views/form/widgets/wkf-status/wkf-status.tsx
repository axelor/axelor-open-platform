import { Schema } from "@/services/client/meta.types";
import { legacyClassNames } from "@/styles/legacy";
import { useAtomValue } from "jotai";
import { FieldProps } from "../../builder";

import styles from "./wkf-status.module.css";

const getTagProps = ({ color = "blue" }: Schema) => {
  return color.startsWith("#")
    ? { style: { backgroundColor: color } }
    : { className: legacyClassNames("badge", `bg-${color}`) };
};

export function WkfStatus({ valueAtom }: FieldProps<any>) {
  const fields = useAtomValue(valueAtom);
  return (
    fields && (
      <ul className={styles.wkfStatus}>
        {fields.map((field: Schema) => (
          <li key={field.name}>
            <span {...getTagProps(field)}>{field.title}</span>
          </li>
        ))}
      </ul>
    )
  );
}
