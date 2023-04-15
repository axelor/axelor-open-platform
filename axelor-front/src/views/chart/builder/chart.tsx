import { memo } from "react";
import { Box } from "@axelor/ui";

import { useWidgetComp } from "./hooks";
import { useResizeDetector } from "@/hooks/use-resize-detector";
import { ChartProps } from "./types";
import classes from "./chart.module.scss";

export const Chart = memo(function Chart(props: ChartProps) {
  const { type } = props;
  const { data: ChartComponent } = useWidgetComp(type!);
  const { ref, height, width } = useResizeDetector();

  return (
    <Box
      ref={ref}
      d="flex"
      position="relative"
      justifyContent="center"
      flex={1}
      className={classes.chart}
    >
      {ChartComponent && (
        <ChartComponent width={width} height={height} {...props} />
      )}
    </Box>
  );
});
