import { Provider, atom, createStore, useAtomValue } from "jotai";
import { uniqueId } from "lodash";
import { useCallback, useState } from "react";

import {
  Box,
  Button,
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  Fade,
  Portal,
} from "@axelor/ui";

import { i18n } from "@/services/client/i18n";

import styles from "./dialogs.module.css";

export type DialogButton = {
  name: string;
  title: string;
  variant: "primary" | "secondary" | "danger";
  onClick: (fn: (result: boolean) => void) => void;
};

export type DialogOptions = {
  title?: string;
  content: React.ReactNode;
  header?: React.ReactNode;
  footer?: React.ReactNode;
  buttons?: DialogButton[];
  onClose?: (result: boolean) => void;
};

type DialogProps = DialogOptions & {
  id: string;
};

const dialogsAtom = atom<DialogProps[]>([]);
const dialogsStore = createStore();

function showDialog(options: DialogOptions) {
  const id = uniqueId("d");
  dialogsStore.set(dialogsAtom, (prev) => [...prev, { id, ...options }]);
}

function closeDialog(id: string) {
  const dialogs = dialogsStore.get(dialogsAtom);
  const found = dialogs.find((x) => x.id === id);
  if (found) {
    dialogsStore.set(dialogsAtom, (prev) => prev.filter((x) => x.id !== id));
  }
}

export module dialogs {
  export async function box({
    title = i18n.get("Information"),
    content,
    yesNo = true,
    yesTitle,
    noTitle,
  }: {
    title?: string;
    content: React.ReactNode;
    yesNo?: boolean;
    yesTitle?: string;
    noTitle?: string;
  }) {
    const [cancelButton, confirmButton] = defaultButtons;
    const buttons = yesNo
      ? [
          { ...cancelButton, title: noTitle ?? cancelButton.title },
          { ...confirmButton, title: yesTitle ?? confirmButton.title },
        ]
      : [{ ...confirmButton, title: yesTitle ?? confirmButton.title }];

    return new Promise<boolean>((resolve) => {
      showDialog({
        title,
        content,
        buttons,
        onClose: resolve,
      });
    });
  }

  export async function info(options: {
    title?: string;
    content: React.ReactNode;
  }) {
    const { title, content } = options;
    return box({ title, content, yesNo: false });
  }

  export async function confirm(options: {
    title?: string;
    content: React.ReactNode;
  }) {
    const { title = i18n.get("Question"), content } = options;
    return box({ title, content });
  }

  export async function error(options: {
    title?: string;
    content: React.ReactNode;
  }) {
    const { title = i18n.get("Error"), content } = options;
    return box({ title, content });
  }
}

export function DialogsProvider() {
  return (
    <Provider store={dialogsStore}>
      <Dialogs />
    </Provider>
  );
}

function Dialogs() {
  const dialogs = useAtomValue(dialogsAtom);
  return (
    <Portal>
      {dialogs.map((dialog) => (
        <DialogContainer key={dialog.id} {...dialog} />
      ))}
      {dialogs.length > 0 && (
        <Fade in={true}>
          <Box className={styles.backdrop}></Box>
        </Fade>
      )}
    </Portal>
  );
}

const defaultButtons: DialogButton[] = [
  {
    name: "cancel",
    title: "Cancel",
    variant: "secondary",
    onClick(fn) {
      fn(false);
    },
  },
  {
    name: "confirm",
    title: "OK",
    variant: "primary",
    onClick(fn) {
      fn(true);
    },
  },
];

const defaultOnClose = () => {};

function DialogContainer(props: DialogProps) {
  const {
    id,
    title,
    content,
    header,
    footer,
    buttons = defaultButtons,
    onClose = defaultOnClose,
  } = props;
  const [open, setOpen] = useState<boolean>(true);

  const close = useCallback(
    (result: boolean) => {
      setOpen(false);
      onClose(result);
      setTimeout(() => closeDialog(id), 300);
    },
    [id, onClose]
  );

  return (
    <Dialog open={open}>
      <DialogHeader onCloseClick={(e) => close(false)}>
        <DialogTitle>{title}</DialogTitle>
        {header}
      </DialogHeader>
      <DialogContent>{content}</DialogContent>
      <DialogFooter>
        {footer}
        {buttons.map((button) => (
          <Button
            key={button.name}
            type="button"
            variant={button.variant}
            onClick={() => button.onClick(close)}
          >
            {button.title}
          </Button>
        ))}
      </DialogFooter>
    </Dialog>
  );
}
