import { useHilites } from "@/hooks/use-parser";
import { Hilite } from "@/services/client/meta.types";
import { legacyClassNames } from "@/styles/legacy";
import { Box } from "@axelor/ui";
import { GridRowProps } from "@axelor/ui/grid";
import { memo } from "react";

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
  const $className = useHilites(hilites ?? [])(record)?.[0]?.css;
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
