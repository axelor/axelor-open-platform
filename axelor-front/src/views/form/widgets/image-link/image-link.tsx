import { useEffect, useState } from "react";
import { useAtomValue } from "jotai";
import { Box } from "@axelor/ui";

import { parseAngularExp } from "@/hooks/use-parser/utils";
import { useFormScope } from "../../builder/scope";
import { FieldControl, FieldProps } from "../../builder";
import { String } from "../string";
import { makeImageURL } from "../image/utils";
import styles from "./image-link.module.scss";

function Image({ valueAtom, schema }: FieldProps<string>) {
  const [src, setSrc] = useState("");
  const { recordHandler } = useFormScope();
  const value = useAtomValue(valueAtom);

  const { height = "auto", width = 140, noframe } = schema;

  useEffect(() => {
    if (value?.includes("{{")) {
      recordHandler.subscribe((record) => {
        const src = parseAngularExp(value)(record);
        setSrc(src);
      });
    } else {
      setSrc(value || "");
    }
  }, [value, recordHandler]);

  return (
    <Box className={styles.imageContainer} style={{ width }}>
      <Box
        as="img"
        {...(!noframe && {
          p: 1,
          border: true,
          shadow: "sm",
        })}
        style={{ height, width }}
        src={src || makeImageURL(null)}
      />
    </Box>
  );
}

export function ImageLink(props: FieldProps<string>) {
  const { readonly } = props;

  if (readonly) {
    return (
      <FieldControl {...props}>
        <Image {...props} />
      </FieldControl>
    );
  }

  return <String {...props} />;
}
