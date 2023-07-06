import { memo, useMemo, useRef } from "react";
import { Box } from "@axelor/ui";

import { useWidgetComp } from "./hooks";
import { useResizeDetector } from "@/hooks/use-resize-detector";
import { ChartProps } from "./types";
import classes from "./chart.module.scss";

export const Chart = memo(function Chart(props: ChartProps) {
  const { type } = props;
  const { data: ChartComponent } = useWidgetComp(type!);
  const { ref, height, width } = useResizeDetector();
  const values = useRef({ height, width });

  const [$height, $width] = useMemo(() => {
    height && (values.current.height = height);
    width && (values.current.width = width);
    return [height || values.current.height, width || values.current.width];
  }, [height, width]);

  return (
    <Box
      ref={ref}
      d="flex"
      position="relative"
      justifyContent="center"
      flex={1}
      className={classes.chart}
    >
      {ChartComponent && $height && $width ? (
        <ChartComponent width={$width} height={$height} {...props} />
      ) : null}
    </Box>
  );
});
