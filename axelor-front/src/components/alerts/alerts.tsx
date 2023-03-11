import { Alert, AlertHeader, Portal, useTheme } from "@axelor/ui";
import clsx from "clsx";
import { useAtomValue, useSetAtom } from "jotai";
import { useEffect } from "react";

import { CSSTransition, TransitionGroup } from "react-transition-group";

import styles from "./alerts.module.css";
import { AlertProps, alertsAtom, closeAlertAtom } from "./atoms";

export function Alerts() {
  const alerts = useAtomValue(alertsAtom);
  const { dir } = useTheme();
  return (
    <Portal>
      <TransitionGroup
        className={clsx(styles.alerts, { [styles.rtl]: dir === "rtl" })}
      >
        {alerts.map((item: AlertProps & { nodeRef?: any }) => (
          <CSSTransition
            key={item.id}
            timeout={500}
            nodeRef={item.nodeRef}
            classNames={{
              enter: styles["item-enter"],
              enterActive: styles["item-enter-active"],
              exit: styles["item-exit"],
              exitActive: styles["item-exit-active"],
            }}
          >
            <div ref={item.nodeRef}>
              <AlertContainer {...item} />
            </div>
          </CSSTransition>
        ))}
      </TransitionGroup>
    </Portal>
  );
}

function AlertContainer({ id, type = "info", title, message }: AlertProps) {
  const closeAlert = useSetAtom(closeAlertAtom);
  useEffect(() => {
    const timer = setTimeout(() => closeAlert(id), 3000);
    return () => {
      clearTimeout(timer);
    };
  }, [id, closeAlert]);
  return (
    <Alert variant={type} shadow className={styles.alert}>
      <AlertHeader>{title}</AlertHeader>
      {message}
    </Alert>
  );
}
