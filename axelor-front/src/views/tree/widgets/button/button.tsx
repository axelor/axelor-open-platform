import { Link, clsx } from "@axelor/ui";
import { useCallback } from "react";

import { Icon } from "@/components/icon";
import { Field } from "@/services/client/meta.types";
import { dialogs } from "@/components/dialogs";

import { WidgetProps } from "../../types";
import { useButtonProps } from "@/views/grid/widgets/button/utils";
import styles from "./button.module.scss";

export function Button({ field, node, record, actionExecutor }: WidgetProps) {
  const { name, icon, onClick, title, prompt, help: _help } = field;
  const help = _help || title;

  const { hidden, readonly } = useButtonProps(field as Field, record);

  const handleClick = useCallback(
    async (event: React.MouseEvent<HTMLElement>) => {
      event.preventDefault();
      event.stopPropagation();
      if (!readonly && onClick && actionExecutor) {
        if (prompt) {
          const confirmed = await dialogs.confirm({
            content: prompt,
          });
          if (!confirmed) return;
        }
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
    [readonly, prompt, onClick, actionExecutor, record, name, node],
  );

  return (
    !hidden && (
      <Link
        d="inline-flex"
        onClick={handleClick}
        title={help}
        className={clsx(styles.action, {
          [styles.readonly]: readonly,
        })}
      >
        {icon && !icon.includes(".") ? (
          <Icon icon={icon} />
        ) : (
          <img
            style={{ maxHeight: 17, width: "100%" }}
            alt={title}
            title={help}
            src={icon}
          />
        )}
      </Link>
    )
  );
}
