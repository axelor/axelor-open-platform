import { parseExpression } from "@/hooks/use-parser/utils";
import { Hilite } from "@/services/client/meta.types";
import { legacyClassNames } from "@/styles/legacy";
import { Box } from "@axelor/ui";
import { GridRowProps } from "@axelor/ui/src/grid";
import { memo, useMemo } from "react";

export const Row = memo(function Row(
  props: GridRowProps & {
    hilites?: Hilite[];
  }
) {
  const {
    hilites,
    data: { record },
  } = props;
  const { children, style, className, onDoubleClick } =
    props as React.HTMLAttributes<HTMLDivElement>;

  const $className = useMemo(
    () =>
      (hilites || [])
        .filter(({ condition }) => parseExpression(condition)(record))
        .slice(0, 1)
        .map((x) => x.css)
        .join(" "),
    [hilites, record]
  );
  return (
    <Box
      {...{
        style,
        className: legacyClassNames(className, $className),
        onDoubleClick,
        children,
      }}
    />
  );
});
