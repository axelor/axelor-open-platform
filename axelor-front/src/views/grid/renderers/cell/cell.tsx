import { Box } from "@axelor/ui";

import { useHilites } from "@/hooks/use-parser";
import { Field } from "@/services/client/meta.types";
import { legacyClassNames } from "@/styles/legacy";
import { sanitize } from "@/utils/sanitize";

import { GridCellProps } from "../../builder/types";
import { useWidgetComp } from "../../hooks";

function CellRenderer(props: GridCellProps) {
  const { type, widget } = props.data as Field;
  const { state, data: Comp } = useWidgetComp((widget || type)!);
  if (state === "loading") return null;
  return (Comp ? <Comp {...props} /> : props.children) as React.ReactElement;
}

export function Cell(props: GridCellProps) {
  const { data, value, record } = props;
  const { type, widget, serverType, hilites } = data as Field;
  const { children, style, className, onClick } =
    props as React.HTMLAttributes<HTMLDivElement>;
  const $className = useHilites(hilites ?? [])(record)?.[0]?.css;

  function render() {
    if (widget || type !== "field") {
      return <CellRenderer {...props} />;
    }
    if (
      typeof value === "string" &&
      (serverType === "STRING" ||
        serverType === "TEXT" ||
        serverType === "ONE_TO_ONE" ||
        serverType === "MANY_TO_ONE")
    ) {
      return <span dangerouslySetInnerHTML={{ __html: sanitize(value) }} />;
    }
    return children;
  }

  return (
    <Box
      {...{
        style,
        className: legacyClassNames(className, $className),
        onClick,
      }}
    >
      {render()}
    </Box>
  );
}
