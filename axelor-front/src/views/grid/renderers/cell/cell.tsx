import { ReactElement, useCallback, useMemo } from "react";

import { Box } from "@axelor/ui";

import { Field, Property, Schema } from "@/services/client/meta.types";
import { legacyClassNames } from "@/styles/legacy";
import { sanitize } from "@/utils/sanitize";
import { Tooltip } from "@/components/tooltip";
import { useHilites } from "@/hooks/use-parser";
import { FieldDetails } from "@/views/form/builder";
import { toCamelCase } from "@/utils/names";

import * as WIDGETS from "../../widgets";
import { GridCellProps } from "../../builder/types";
import { Image } from "../../widgets/image";

const getWidget = (name?: string) =>
  WIDGETS[toCamelCase(name ?? "") as keyof typeof WIDGETS];

export function Cell(props: GridCellProps) {
  const { view, data, value, record } = props;
  const { items: viewItems = [] } = view ?? {};
  const { name, type, tooltip, widget, serverType, hilites } = data as Field;
  const { children, style, className, onClick } =
    props as React.HTMLAttributes<HTMLDivElement>;
  const $className = useHilites(hilites ?? [])(record)?.[0]?.css;

  function render() {
    function renderContent() {
      const Comp = getWidget(widget) || getWidget(type) || getWidget(serverType);
      if (Comp) {
        return <Comp {...props} />;
      }
      if (serverType === "BINARY" && name === "image") {
        return <Image {...props} />;
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

  const fields = useMemo(
    () =>
      viewItems.reduce(
        (acc, item) => ({
          ...acc,
          [item.name ?? ""]: {
            ...item,
            ...item.widgetAttrs,
          } as unknown as Property,
        }),
        {} as Record<string, Property>,
      ),
    [viewItems],
  );

  const $getField = useCallback(
    (fieldName: string) => fields[fieldName] as Schema,
    [fields],
  );

  return tooltip ? (
    <Tooltip
      content={() => (
        <FieldDetails
          model={view?.model}
          data={tooltip}
          record={record}
          $getField={$getField}
        />
      )}
    >
      {render() as ReactElement}
    </Tooltip>
  ) : (
    render()
  );
}
