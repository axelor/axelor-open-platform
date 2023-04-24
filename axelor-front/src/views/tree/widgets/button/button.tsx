import { Link } from "@axelor/ui";
import { legacyClassNames } from "@/styles/legacy";
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
        <i className={legacyClassNames("fa", icon)} />
      ) : (
        <img width={16} alt="Tree Icon" src={icon} />
      )}
    </Link>
  );
}
