import {
  Box,
  Button,
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  Fade,
  Portal
} from "@axelor/ui";
import { useAtomValue, useSetAtom } from "jotai";
import { useCallback, useState } from "react";
import { closeDialogAtom, DialogProps, dialogsAtom } from "./atoms";

import styles from "./dialogs.module.css";

export function Dialogs() {
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

function DialogContainer(props: DialogProps) {
  const { id, title, content, header, footer, onClose } = props;
  const [open, setOpen] = useState<boolean>(true);

  const closeDialog = useSetAtom(closeDialogAtom);
  const close = useCallback(
    async (confirmed: boolean) => {
      await onClose(confirmed);
      setOpen(false);
      setTimeout(() => closeDialog(id), 300);
    },
    [onClose, closeDialog, id]
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
        <Button variant="secondary" onClick={(e) => close(false)}>
          Close
        </Button>
        <Button variant="primary" onClick={(e) => close(true)}>
          OK
        </Button>
      </DialogFooter>
    </Dialog>
  );
}
