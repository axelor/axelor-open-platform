import { useAtomValue } from "jotai";
import { useMemo } from "react";

import { Box, LinearProgress } from "@axelor/ui";

import { Schema } from "@/services/client/meta.types";

import { FieldControl, FieldProps } from "../../builder";
import { Integer as IntegerWidget } from "../integer";

import styles from "./progress.module.scss";

// colors "r:24,y:49,b:74,g:100" -> [{code:'r', max:24}...]
type TransformColor = {
  code: string;
  max: number;
};

const transformColors = (colors: string): TransformColor[] =>
  colors
    .split(/,/)
    .map((c) => c.split(/:/))
    .map((c) => ({
      code: c[0],
      max: Number(c[1]),
    }))
    .sort((a, b) => a.max - b.max);

export function ProgressComponent({
  value,
  schema,
}: {
  value: number | string;
  schema: Schema;
}) {
  const props = useMemo(() => {
    const { min = 0, max = 100, colors = "r:24,y:49,b:74,g:100" } = schema;
    const num = Math.min(Math.round((+value * 100) / (max - min)), 100);
    const code = transformColors(colors).find((c) => num <= c.max)?.code ?? "";
    return { value: num, className: styles[code] };
  }, [value, schema]);
  return <LinearProgress flex={1} {...props} striped animated />;
}

export function Progress(props: FieldProps<number | string>) {
  const { schema, readonly, valueAtom } = props;
  const value = useAtomValue(valueAtom);
  if (readonly) {
    return (
      <FieldControl {...props}>
        <Box className={styles.progress}>
          <ProgressComponent value={value || 0} schema={schema} />
        </Box>
      </FieldControl>
    );
  }
  return <IntegerWidget {...props} />;
}
