import { useMemo } from "react";

import { Alert } from "@axelor/ui";

import { WidgetProps } from "../../builder";

export function HelpComponent({ css, text }: { text?: string; css?: string }) {
  const variant = useMemo(() => {
    const cssClass = css || "";
    if (cssClass.includes("alert-warning")) return "warning";
    if (cssClass.includes("alert-danger")) return "danger";
    if (cssClass.includes("alert-success")) return "success";
    return "info";
  }, [css]);
  return <Alert variant={variant}>{text}</Alert>;
}

export function Help(props: WidgetProps) {
  const { schema } = props;
  const { text } = schema;
  return <HelpComponent text={text} css={schema.css} />;
}
