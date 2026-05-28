import { memo } from "react";
import { Box, CircularProgress } from "@axelor/ui";

import { i18n } from "@/services/client/i18n";
import * as CHARTS from "../widgets";
import { useResizeDetector } from "@/hooks/use-resize-detector";
import { ChartProps } from "./types";
import { toCamelCase } from "@/utils/names";
import classes from "./chart.module.scss";

export const Chart = memo(function Chart(props: ChartProps) {
  const { type, loading } = props;
  const ChartComponent = CHARTS[toCamelCase(type!) as keyof typeof CHARTS];
  const { ref, height, width } = useResizeDetector({
    keepLastSize: true,
  });

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
      {loading && (
        <Box
          position="absolute"
          style={{ inset: 0 }}
          d="flex"
          alignItems="center"
          justifyContent="center"
          className={classes.loader}
        >
          <Box d="flex" flexDirection="column" alignItems="center" g={2}>
            <CircularProgress size={25} indeterminate />
            <Box>{i18n.get("Please wait...")}</Box>
          </Box>
        </Box>
      )}
    </Box>
  );
});
