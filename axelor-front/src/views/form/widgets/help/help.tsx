import { ReactNode, useMemo } from "react";
import { useAtomValue } from "jotai";

import { Alert } from "@axelor/ui";

import { WidgetProps } from "../../builder";
import { useTemplate } from "@/hooks/use-parser";

import styles from "./help.module.scss";

export function HelpComponent({
  css,
  text,
}: {
  text?: ReactNode;
  css?: string;
}) {
  const variant = useMemo(() => {
    const cssClass = css || "";
    if (cssClass.includes("alert-warning")) return "warning";
    if (cssClass.includes("alert-danger")) return "danger";
    if (cssClass.includes("alert-success")) return "success";
    return "info";
  }, [css]);

  return (
    <Alert className={styles.alert} variant={variant}>
      {text}
    </Alert>
  );
}

export function Help(props: WidgetProps) {
  const { schema, formAtom } = props;
  const { text } = schema;
  const { record } = useAtomValue(formAtom);
  const Template = useTemplate(text);
  return (
    <HelpComponent text={<Template context={record} />} css={schema.css} />
  );
}
