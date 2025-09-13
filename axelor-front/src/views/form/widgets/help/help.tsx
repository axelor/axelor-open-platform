import { useAtomValue } from "jotai";
import { ReactNode, useMemo } from "react";

import { TemplateRenderer } from "@/hooks/use-parser";
import { session } from "@/services/client/session";
import { Alert } from "@axelor/ui";

import { WidgetProps } from "../../builder";

import styles from "./help.module.scss";

export function HelpComponent({
  css,
  text,
  variant,
}: {
  text?: ReactNode;
  /** @deprecated in favor of `variant` */
  css?: string;
  variant?: "success" | "danger" | "warning" | "info";
}) {
  const variantValue = useMemo(() => {
    if (variant && ["success", "danger", "warning", "info"].includes(variant)) {
      return variant;
    }

    if (css) {
      if (
        session?.info?.application?.mode != "prod" &&
        css.includes("alert-")
      ) {
        console.warn(
          'Help widget `css` property is deprecated, use `variant` ("success" | "danger" | "warning" | "info") instead',
        );
      }

      const cssClass = css || "";
      if (cssClass.includes("alert-warning")) return "warning";
      if (cssClass.includes("alert-danger")) return "danger";
      if (cssClass.includes("alert-success")) return "success";
    }

    return "info";
  }, [css, variant]);

  return (
    <Alert className={styles.alert} variant={variantValue}>
      {text}
    </Alert>
  );
}

export function Help(props: WidgetProps) {
  const { schema, formAtom } = props;
  const { text, css, variant } = schema;
  const { record } = useAtomValue(formAtom);

  return (
    <HelpComponent
      text={<TemplateRenderer context={record} template={text} />}
      css={css}
      variant={variant as any}
    />
  );
}
