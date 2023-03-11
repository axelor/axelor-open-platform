import { atom } from "jotai";
import { uniqueId } from "lodash";
import { createRef } from "react";

export type AlertType =
  | "primary"
  | "secondary"
  | "info"
  | "success"
  | "warning"
  | "danger";

export type AlertProps = {
  id: string;
  type: AlertType;
  title?: string;
  message: string;
};

export const alertsAtom = atom<AlertProps[]>([]);

export const closeAlertAtom = atom(null, (get, set, id: string) => {
  const alerts = get(alertsAtom);
  const found = alerts.find((x) => x.id === id);
  if (found) {
    set(alertsAtom, (prev) => prev.filter((x) => x !== found));
  }
});

export const showAlertAtom = atom(
  null,
  (get, set, alert: { type: AlertType; title?: string; message: string }) => {
    const id = uniqueId("n");
    const nodeRef = createRef();
    set(alertsAtom, (prev) => [{ id, nodeRef, ...alert }, ...prev]);
  }
);
