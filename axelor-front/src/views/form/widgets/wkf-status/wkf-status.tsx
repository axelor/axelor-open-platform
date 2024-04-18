import { Box } from "@axelor/ui";
import { useAtomValue } from "jotai";

import { Tooltip } from "@/components/tooltip";
import { Schema } from "@/services/client/meta.types";
import { legacyClassNames } from "@/styles/legacy";
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
          <Tooltip
            key={field.name}
            title={field.title}
            content={() => <span>{field.help}</span>}
          >
            <Box d="flex" as="li" {...(field.help && { className: styles.help })}>
              <span {...getTagProps(field)}>{field.title}</span>
            </Box>
          </Tooltip>
        ))}
      </ul>
    )
  );
}
