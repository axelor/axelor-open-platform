import { useMemo } from "react";

import { Alert } from "@axelor/ui";

import { WidgetProps } from "../../builder";

export function Help(props: WidgetProps) {
  const { schema } = props;
  const { text } = schema;
  const variant = useMemo(() => {
    const css = schema.css || "";
    if (css.includes("alert-warning")) return "warning";
    if (css.includes("alert-danger")) return "danger";
    if (css.includes("alert-success")) return "success";
    return "info";
  }, [schema]);
  return <Alert variant={variant}>{text}</Alert>;
}
