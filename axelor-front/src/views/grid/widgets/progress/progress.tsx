import React, { useMemo } from "react";
import { Box, LinearProgress } from "@axelor/ui";
import { GridColumnProps } from "@axelor/ui/src/grid/grid-column";
import classes from "./progress.module.scss";

// colors "r:24,y:49,b:74,g:100" -> [{code:'r', max:24}...]
type TransformColor = {
  code: string;
  max: number;
};

const transformColors = (colors: string) =>
  colors
    .split(/,/)
    .map((c) => c.split(/:/))
    .map(
      (c) =>
        ({
          code: c[0],
          max: Number(c[1]),
        } as TransformColor)
    )
    .sort((a, b) => a.max - b.max);

export function Progress(props: GridColumnProps) {
  const { data: field, value: _value } = props;

  const progressProps = useMemo(() => {
    const {
      min = 0,
      max = 100,
      colors = "r:24,y:49,b:74,g:100",
    } = field as {
      min?: number;
      max?: number;
      colors?: string;
    };

    const value = Math.min(Math.round((_value * 100) / (max - min)), 100);
    const { code } = transformColors(colors).find((c) => value <= c.max) || {
      code: "",
    };

    return { value, className: classes[code] };
  }, [_value, field]);

  return (
    <Box d="flex" h={100} alignItems="center">
      <LinearProgress flex={1} {...progressProps} striped animated />
    </Box>
  );
}
