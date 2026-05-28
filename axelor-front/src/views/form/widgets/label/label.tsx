import { useMemo } from "react";
import { useAtomValue } from "jotai";

import { legacyClassNames } from "@/styles/legacy";
import { sanitize } from "@/utils/sanitize";
import { WidgetProps } from "../../builder";

export function Label({ schema, widgetAtom }: WidgetProps) {
  const { attrs } = useAtomValue(widgetAtom);
  const { title = "", css } = attrs;

  const text = useMemo(() => title && sanitize(title), [title]);

  return (
    <label
      className={legacyClassNames(css)}
      dangerouslySetInnerHTML={{ __html: text }}
      data-testid="label"
    />
  );
}
