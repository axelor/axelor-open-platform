import { useSetAtom } from "jotai";
import { showAlertAtom } from "./atoms";

export function useAlerts() {
  return useSetAtom(showAlertAtom);
}
