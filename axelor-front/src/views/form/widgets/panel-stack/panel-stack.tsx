import { StackLayout, WidgetProps } from "../../builder";
import styles from "./panel-stack.module.css";

export function PanelStack(props: WidgetProps) {
    return <StackLayout {...props} className={styles.panelStack} />
}