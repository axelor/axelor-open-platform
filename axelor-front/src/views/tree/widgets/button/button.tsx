import { Icon } from "@/components/icon";
import { Link } from "@axelor/ui";
import { WidgetProps } from "../../types";
import styles from "./button.module.scss";

export function Button({ field, node, record, actionExecutor }: WidgetProps) {
  const { name, icon, onClick } = field;
  return (
    <Link
      className={styles.link}
      onClick={(e) => {
        e.preventDefault();
        e.stopPropagation();
        onClick &&
          actionExecutor?.execute(onClick, {
            context: {
              ...record,
              _signal: name,
              _model: node?.model,
            },
          });
      }}
    >
      {icon && icon.includes?.("fa") ? (
        <Icon icon={icon} />
      ) : (
        <img width={16} alt="Tree Icon" src={icon} />
      )}
    </Link>
  );
}
