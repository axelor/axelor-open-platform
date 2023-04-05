import clsx from "clsx";
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
import { SenitizedContent } from "@/utils/sanitize";

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
  size?: "sm" | "md" | "lg" | "xl";
  classes?: {
    root?: string;
    content?: string;
    header?: string;
    footer?: string;
  };
  onClose?: (result: boolean) => void;
};

type DialogProps = {
  id: string;
  options: DialogOptions;
};

const dialogsAtom = atom<DialogProps[]>([]);
const dialogsStore = createStore();

function showDialog(options: DialogOptions) {
  const id = uniqueId("d");
  dialogsStore.set(dialogsAtom, (prev) => [...prev, { id, options }]);
  return () => {
    dialogsStore.set(dialogsAtom, (prev) => prev.filter((x) => x.id !== id));
  };
}

export module dialogs {
  export async function modal(options: DialogOptions) {
    const { onClose, classes, ...rest } = options;
    const handleClose = async (result: boolean) => {
      return new Promise((resolve) => {
        setTimeout(() => {
          resolve(result);
          onClose?.(result);
          close();
        }, 100);
      });
    };
    const close = showDialog({
      ...rest,
      classes: {
        ...classes,
        root: clsx(styles.root, classes?.root),
        content: clsx(styles.content, classes?.content),
      },
      onClose: handleClose,
    });
    return handleClose;
  }

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

    if (typeof content === "string") {
      content = <SenitizedContent content={content} />;
    }

    return new Promise<boolean>(async (resolve) => {
      const close = await modal({
        title,
        content,
        buttons,
        classes: { content: styles.box },
        onClose: async (result) => {
          await close(result);
          resolve(result);
        },
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
      {dialogs.map(({ id, options }) => (
        <ModalDialog key={id} {...options} />
      ))}
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

export function ModalDialog(props: DialogOptions) {
  const {
    size,
    title,
    content,
    header,
    footer,
    buttons = defaultButtons,
    classes = {},
    onClose = defaultOnClose,
  } = props;
  const [open, setOpen] = useState<boolean>(true);

  const close = useCallback(
    (result: boolean) => {
      setOpen(false);
      onClose(result);
    },
    [onClose]
  );

  return (
    <>
      <Dialog open={open} scrollable size={size} className={classes.root}>
        <DialogHeader
          onCloseClick={(e) => close(false)}
          className={classes.header}
        >
          <DialogTitle className={styles.title}>{title}</DialogTitle>
          {header}
        </DialogHeader>
        <DialogContent className={classes.content}>{content}</DialogContent>
        <DialogFooter className={classes.footer}>
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
      <Fade in={open}>
        <Box className={styles.backdrop}></Box>
      </Fade>
    </>
  );
}
