import { memo, useEffect, useState } from "react";
import { Box } from "@axelor/ui";

import { useWidgetComp } from "./hooks";
import { useResizeDetector } from "@/hooks/use-resize-detector";
import { ChartProps } from "./types";
import classes from "./chart.module.scss";

export const Chart = memo(function Chart(props: ChartProps) {
  const { type } = props;
  const { data: ChartComponent } = useWidgetComp(type!);
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
      {ChartComponent && height && width ? (
        <ChartComponent width={width} height={height} {...props} />
      ) : null}
    </Box>
  );
});
