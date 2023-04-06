import { Portal } from "@axelor/ui";
import { Provider, atom, createStore, useAtomValue } from "jotai";
import { uniqueId } from "lodash";
import { Fragment } from "react";
import { PopupDialog, PopupProps } from "./view-popup";

type PopupsState = {
  id: string;
  popup: React.ReactNode;
};

const popupStore = createStore();
const popupAtom = atom<PopupsState[]>([]);

export async function showPopup(props: PopupProps) {
  const { tab, open = true, onClose, ...rest } = props;

  const handleClose = (result: boolean) => {
    onClose?.(result);
    close();
  };

  const id = uniqueId("$p");
  const popup = tab && (
    <PopupDialog tab={tab} open={open} onClose={handleClose} {...rest} />
  );

  popupStore.set(popupAtom, (prev) => [...prev, { id, popup }]);

  const close = () => {
    popupStore.set(popupAtom, (prev) => prev.filter((x) => x.id !== id));
  };

  return close;
}

export function PopupsProvider() {
  return (
    <Provider store={popupStore}>
      <Popups />
    </Provider>
  );
}

function Popups() {
  const popups = useAtomValue(popupAtom);
  return (
    <Portal>
      {popups.map(({ id, popup }) => (
        <Fragment key={id}>{popup}</Fragment>
      ))}
    </Portal>
  );
}
