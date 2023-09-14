import { useAtomValue } from "jotai";

import { Box } from "@axelor/ui";

import { FieldControl, FieldProps } from "../../builder";
import { String } from "../string";

import styles from "./phone.module.scss";

export function Phone(props: FieldProps<string>) {
  const { readonly, valueAtom } = props;
  const value = useAtomValue(valueAtom);
  if (readonly) {
    return (
      <FieldControl {...props}>
        {value && (
          <Box
            as="a"
            target="_blank"
            href={`tel:${value}`}
            className={styles.link}
          >
            {value}
          </Box>
        )}
      </FieldControl>
    );
  }
  return <String {...props} inputProps={{ type: "tel" }} />;
}
