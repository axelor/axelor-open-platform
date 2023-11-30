import clsx from "clsx";
import { Provider, atom, createStore, useAtomValue } from "jotai";
import { uniqueId } from "lodash";
import { useCallback, useRef, useState } from "react";

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

import { DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { SanitizedContent } from "@/utils/sanitize";
import { usePopupHandlerAtom } from "@/view-containers/view-popup/handler";

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
  header?:
    | ((close: (result: boolean) => void) => React.ReactNode)
    | React.ReactNode;
  footer?: (close: (result: boolean) => void) => React.ReactNode;
  buttons?: DialogButton[];
  size?: "sm" | "md" | "lg" | "xl";
  padding?: string;
  classes?: {
    root?: string;
    content?: string;
    header?: string;
    footer?: string;
  };
  open: boolean;
  maximize?: boolean;
  closeable?: boolean;
  setOpen?: React.Dispatch<React.SetStateAction<boolean>>;
  onClose: (result: boolean, record?: DataRecord) => void;
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

export function dialogsActive() {
  return dialogsStore.get(dialogsAtom).length > 0;
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace dialogs {
  export async function modal(options: DialogOptions) {
    const { onClose, open = true, ...rest } = options;
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
      open,
      onClose: handleClose,
      ...rest,
    });
  }

  export async function box({
    size,
    title = i18n.get("Information"),
    content,
    yesNo = true,
    yesTitle,
    noTitle,
    padding,
    footer,
  }: {
    title?: string;
    content: React.ReactNode;
    yesNo?: boolean;
    yesTitle?: string;
    noTitle?: string;
  } & Pick<DialogOptions, "size" | "padding" | "footer">) {
    const [cancelButton, confirmButton] = getDefaultButtons();
    const buttons = yesNo
      ? [
          { ...cancelButton, title: noTitle ?? cancelButton.title },
          { ...confirmButton, title: yesTitle ?? confirmButton.title },
        ]
      : [{ ...confirmButton, title: yesTitle ?? confirmButton.title }];

    if (typeof content === "string") {
      content = <SanitizedContent content={content} />;
    }

    return new Promise<boolean>((resolve) => {
      void (async () => {
        await modal({
          open: true,
          size,
          title,
          content,
          buttons,
          padding,
          classes: { content: styles.box },
          onClose: (result) => resolve(result),
          footer,
        });
      })();
    });
  }

  export async function info(options: {
    size?: DialogOptions["size"];
    title?: string;
    content: React.ReactNode;
  }) {
    const { title, size, content } = options;
    return box({ size, title, content, yesNo: false });
  }

  export async function confirm(options: {
    size?: DialogOptions["size"];
    title?: string;
    content: React.ReactNode;
    yesTitle?: string;
    noTitle?: string;
  }) {
    const {
      size,
      title = i18n.get("Question"),
      content,
      yesTitle,
      noTitle,
    } = options;
    return box({ size, title, content, yesTitle, noTitle });
  }

  export async function error(options: {
    size?: DialogOptions["size"];
    title?: string;
    content: React.ReactNode;
  }) {
    const { size, title = i18n.get("Error"), content } = options;
    return box({ size, title, content, yesNo: false });
  }

  export async function confirmDirty(
    check: () => Promise<boolean>,
    callback: () => Promise<any>,
    options?: {
      title?: string;
      content?: React.ReactNode;
    },
  ) {
    const {
      title,
      content = i18n.get(
        "Current changes will be lost. Do you really want to proceed?",
      ),
    } = options ?? {};
    const dirty = await check();
    const confirmed = !dirty || (await confirm({ title, content }));
    if (confirmed) {
      await callback();
    }
    return confirmed;
  }

  export async function confirmSave(
    check: () => Promise<boolean>,
    callback: () => Promise<any>,
    options?: {
      title?: string;
      content?: React.ReactNode;
    },
  ) {
    const {
      content = i18n.get(
        "Current changes will be saved. Do you want to proceed?",
      ),
      ...rest
    } = options ?? {};
    return await confirmDirty(check, callback, { content, ...rest });
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

const getDefaultButtons: () => DialogButton[] = () => [
  {
    name: "cancel",
    title: i18n.get("Cancel"),
    variant: "secondary",
    onClick(fn) {
      fn(false);
    },
  },
  {
    name: "confirm",
    title: i18n.get("OK"),
    variant: "primary",
    onClick(fn) {
      fn(true);
    },
  },
];

export function ModalDialog(props: DialogOptions) {
  const {
    open,
    setOpen,
    size,
    title,
    content,
    header,
    footer,
    padding,
    buttons = getDefaultButtons(),
    classes = {},
    closeable = true,
    onClose,
    maximize,
  } = props;

  const [show, setShow] = useState(open);
  const [result, setResult] = useState(false);

  const close = useCallback(
    (result: boolean) => {
      setOpen?.(false);
      setShow(false);
      setResult(result);
    },
    [setOpen],
  );

  const handlerAtom = usePopupHandlerAtom();
  const handler = useAtomValue(handlerAtom);

  const contentRef = useRef<HTMLDivElement | null>(null);

  const initialFocus = useCallback(() => contentRef.current!, []);

  const onHide = useCallback(() => {
    const record = handler.getState?.().record;
    onClose?.(result, record);
  }, [onClose, result, handler]);

  const canShow = setOpen ? open : show;

  return (
    <Portal>
      <Fade in={canShow} unmountOnExit mountOnEnter>
        <Box className={styles.backdrop}></Box>
      </Fade>
      <Dialog
        open={canShow}
        onHide={onHide}
        onShow={initialFocus}
        scrollable
        fullscreen={maximize}
        size={size}
        className={clsx(classes.root, styles.root)}
        initialFocus={initialFocus}
        data-dialog="true"
      >
        <DialogHeader
          {...(closeable && {
            onCloseClick: () => close(false),
          })}
          className={classes.header}
        >
          <DialogTitle className={styles.title}>{title}</DialogTitle>
          {typeof header === "function" ? header(close) : header}
        </DialogHeader>
        <DialogContent
          className={clsx(classes.content, styles.content)}
          style={{ padding }}
          tabIndex={-1}
          ref={contentRef}
        >
          {content}
        </DialogContent>
        <DialogFooter className={classes.footer}>
          {footer && footer(close)}
          {buttons.map((button) => (
            <Button
              autoFocus={button.variant === "primary"}
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
    </Portal>
  );
}
