import { Icon } from "@/components/icon";
import { Box, Link } from "@axelor/ui";
import { WidgetProps } from "../../types";
import styles from "./button.module.scss";
import { useCallback } from "react";

export function Button({ field, node, record, actionExecutor }: WidgetProps) {
  const { name, icon, onClick, title, help } = field;

  const handleClick = useCallback(
    async (event: React.MouseEvent<HTMLElement>) => {
      event.preventDefault();
      event.stopPropagation();
      if (onClick && actionExecutor) {
        await actionExecutor.waitFor();
        await actionExecutor.execute(onClick, {
          context: {
            ...record,
            _signal: name,
            _model: node?.model,
          },
        });
      }
    },
    [onClick, actionExecutor, record, name, node],
  );

  return (
    <Link
      d="inline-flex"
      onClick={handleClick}
      title={help}
      className={styles.action}
    >
      {icon && !icon.includes(".") ? (
        <Icon icon={icon} />
      ) : (
        <img style={{ maxHeight: 24, width: "100%"}} alt={title} title={help} src={icon} />
      )}
    </Link>
  );
}
