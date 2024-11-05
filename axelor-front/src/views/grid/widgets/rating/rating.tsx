import { useMemo } from "react";

import { Box, Rating as AxRating } from "@axelor/ui";
import { GridColumnProps } from "@axelor/ui/grid";
import { Schema } from "@/services/client/meta.types.ts";

export function Rating(props: GridColumnProps) {
  const { record, data, rawValue } = props;
  const { maxSize = 5, widgetAttrs } = data as Schema;
  const {
    ratingIcon = "star",
    ratingColor,
    ratingFill = true,
    ratingHighlightSelected = false,
  } = widgetAttrs || {};

  const text = useMemo(
    () => (rawValue != null ? data.formatter?.(data, rawValue, record) : ""),
    [data, record, rawValue],
  );

  return (
    <Box d="inline-flex">
      <AxRating
        value={Number(rawValue)}
        text={text}
        icon={ratingIcon}
        color={ratingColor}
        fill={ratingFill}
        highlightSelected={ratingHighlightSelected}
        max={maxSize}
        readonly={true}
      />
    </Box>
  );
}
