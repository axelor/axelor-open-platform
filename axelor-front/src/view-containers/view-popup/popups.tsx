import { Provider, atom, createStore, useAtomValue } from "jotai";
import uniqueId from "lodash/uniqueId";
import { Fragment } from "react";

import { Portal } from "@axelor/ui";

import { DataRecord } from "@/services/client/data.types";
import { PopupDialog, PopupProps } from "./view-popup";

type PopupsState = {
  id: string;
  tabId: string;
  popup: React.ReactNode;
};

const popupStore = createStore();
const popupAtom = atom<PopupsState[]>([]);

export async function showPopup(props: PopupProps) {
  const { tab, open = true, onClose, ...rest } = props;

  const handleClosePopup = (result: boolean, record?: DataRecord) => {
    onClose?.(result, record);
    removePopup();
  };

  const id = uniqueId("$p");
  const popup = tab && (
    <PopupDialog tab={tab} open={open} onClose={handleClosePopup} {...rest} />
  );

  popupStore.set(popupAtom, (prev) => [...prev, { id, popup, tabId: tab.id }]);

  const removePopup = () => {
    popupStore.set(popupAtom, (prev) => prev.filter((x) => x.id !== id));
  };

  return removePopup;
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
