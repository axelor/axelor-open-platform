import { useAtomValue } from "jotai";
import { ReactNode, useMemo } from "react";

import { Alert } from "@axelor/ui";
import { TemplateRenderer } from "@/hooks/use-parser";
import { session } from "@/services/client/session";

import { WidgetProps } from "../../builder";

import styles from "./help.module.scss";

export function HelpComponent({
  text,
  variant,
}: {
  text?: ReactNode;
  variant?: "success" | "danger" | "warning" | "info";
}) {
  const variantValue = useMemo(() => {
    if (variant && ["success", "danger", "warning", "info"].includes(variant)) {
      return variant;
    }

    return "info";
  }, [variant]);

  return (
    <Alert className={styles.alert} variant={variantValue}>
      {text}
    </Alert>
  );
}

export function Help(props: WidgetProps) {
  const { schema, formAtom } = props;
  const { text, variant } = schema;
  const { record } = useAtomValue(formAtom);

  return (
    <HelpComponent
      text={<TemplateRenderer context={record} template={text} />}
      variant={variant as any}
    />
  );
}
