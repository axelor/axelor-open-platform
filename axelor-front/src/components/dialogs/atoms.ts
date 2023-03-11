import { atom } from "jotai";
import { uniqueId } from "lodash";

export type DialogType = "yes" | "yes-no";

export type DialogProps = {
  id: string;
  type: DialogType;
  title?: string;
  content: React.ReactNode;
  header?: React.ReactNode;
  footer?: React.ReactNode;
  onClose: (confirmed: boolean) => void | Promise<void>;
};

type OpenOptions = Omit<DialogProps, "id">;

export const dialogsAtom = atom<DialogProps[]>([]);

export const showDialogAtom = atom(null, (get, set, options: OpenOptions) => {
  const id = uniqueId("d");
  set(dialogsAtom, (prev) => [{ id, ...options }, ...prev]);
});

export const closeDialogAtom = atom(null, (get, set, id: string) => {
  const dialogs = get(dialogsAtom);
  const found = dialogs.find((x) => x.id === id);
  if (found) {
    set(dialogsAtom, (prev) => prev.filter((x) => x.id !== id));
  }
});
