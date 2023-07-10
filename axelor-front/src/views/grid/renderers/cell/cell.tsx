import { ReactElement } from "react";
import { Box } from "@axelor/ui";

import { Field, Tooltip as TooltipType } from "@/services/client/meta.types";
import { legacyClassNames } from "@/styles/legacy";
import { sanitize } from "@/utils/sanitize";
import { Tooltip } from "@/components/tooltip";
import { DataRecord } from "@/services/client/data.types";
import { DataStore } from "@/services/client/data-store";
import { useHilites, useTemplate } from "@/hooks/use-parser";
import { useAsync } from "@/hooks/use-async";

import { GridCellProps } from "../../builder/types";
import { useWidgetComp } from "../../hooks";

function CellRenderer(props: GridCellProps) {
  const { type, widget } = props.data as Field;
  const { state, data: Comp } = useWidgetComp((widget || type)!);
  if (state === "loading") return null;
  return (Comp ? <Comp {...props} /> : props.children) as React.ReactElement;
}

function TooltipContent({
  model,
  record,
  data,
}: {
  model?: string;
  record?: DataRecord;
  data: TooltipType;
}) {
  const { depends, template } = data;
  const Template = useTemplate(template!);
  const { data: context } = useAsync(async () => {
    let values = { ...record };
    if (model && record?.id) {
      const ds = new DataStore(model);
      const newValues = await ds.read(+record.id, {
        fields: (depends || "")?.split?.(",").map((f) => f.trim()),
      });
      values = { ...values, ...newValues };
    }
    return { ...values, record: values };
  }, [record]);

  return (
    context && (
      <Box>
        <Template context={context} />
      </Box>
    )
  );
}

export function Cell(props: GridCellProps) {
  const { view, data, value, record } = props;
  const { type, tooltip, widget, serverType, hilites } = data as Field;
  const { children, style, className, onClick } =
    props as React.HTMLAttributes<HTMLDivElement>;
  const $className = useHilites(hilites ?? [])(record)?.[0]?.css;

  function render() {
    function renderContent() {
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
        {renderContent()}
      </Box>
    );
  }

  return tooltip ? (
    <Tooltip
      content={() => (
        <TooltipContent model={view?.model} data={tooltip} record={record} />
      )}
    >
      {render() as ReactElement}
    </Tooltip>
  ) : (
    render()
  );
}
