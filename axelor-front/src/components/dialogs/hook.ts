import { useSetAtom } from "jotai";
import { showDialogAtom } from "./atoms";

export function useDialogs() {
  return useSetAtom(showDialogAtom);
}
