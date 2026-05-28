import { memo } from "react";

import { Box, findAriaProp, findDataProp } from "@axelor/ui";
import { GridRowProps } from "@axelor/ui/grid";

import { Hilite } from "@/services/client/meta.types";
import { useHilites } from "@/hooks/use-parser";
import { legacyClassNames } from "@/styles/legacy";
import { useViewAction } from "@/view-containers/views/scope.ts";

import styles from "./row.module.css";

export const Row = memo(function Row(
  props: GridRowProps & {
    hilites?: Hilite[];
  },
) {
  const {
    selected,
    hilites,
    data: { record },
  } = props;
  const { children, style, className, onDoubleClick } =
    props as React.HTMLAttributes<HTMLDivElement>;
  const { context } = useViewAction();
  const $className = useHilites(hilites)({ ...context, ...record })?.[0]?.css;

  const { role } = props as React.HTMLAttributes<HTMLDivElement>;
  const testId = findDataProp(props, "data-testid");
  const ariaRowIndex = findAriaProp(props, "aria-rowindex");
  const ariaSelected = findAriaProp(props, "aria-selected");

  return (
    <Box
      {...{
        style,
        className: legacyClassNames(className, $className, styles.row, {
          [styles.selected]: selected,
        }),
        onDoubleClick,
        children,
      }}
      role={role}
      aria-rowindex={ariaRowIndex}
      aria-selected={ariaSelected}
      data-testid={testId}
    />
  );
});
