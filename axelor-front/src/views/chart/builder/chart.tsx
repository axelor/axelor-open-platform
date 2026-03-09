import { useEffect, useState } from "react";
import { Box } from "@axelor/ui";

import * as CHARTS from "../widgets";
import { useResizeDetector } from "@/hooks/use-resize-detector";
import { ChartProps } from "./types";
import { toCamelCase } from "@/utils/names";
import classes from "./chart.module.scss";

export function Chart(props: ChartProps) {
  const { type } = props;
  const ChartComponent = CHARTS[toCamelCase(type!) as keyof typeof CHARTS];
  const { ref, height: _height, width: _width } = useResizeDetector();
  const [height, setHeight] = useState(_height);
  const [width, setWidth] = useState(_width);

  useEffect(() => {
    if (_height) {
      setHeight(_height);
    }
    if (_width) {
      setWidth(_width);
    }
  }, [_height, _width]);

  return (
    <Box
      ref={ref}
      d="flex"
      position="relative"
      justifyContent="center"
      flex={1}
      className={classes.chart}
    >
      {height && width ? (
        <ChartComponent width={width} height={height} {...props} />
      ) : null}
    </Box>
  );
}
