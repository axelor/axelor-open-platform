import { createElement } from "react";

import { dialogs } from "@/components/dialogs";
import { i18n } from "@/services/client/i18n";

import { IdentityCheckDialog } from "./identity-check-dialog";

/**
 * Opens an identity verification dialog.
 *
 * @param pendingAction optional action name stored on the server-side
 *   identity-checked session flag, useful when the verification is gating a
 *   specific subsequent action.
 * @returns true if the user successfully verified their identity, false otherwise.
 */
export async function showIdentityCheck(): Promise<boolean> {
  return new Promise<boolean>((resolve) => {
    dialogs.modal({
      open: true,
      title: i18n.get("Identity Verification"),
      size: "md",
      showFooter: false,
      closeable: false,
      padding: "var(--bs-modal-padding)",
      content: createElement(IdentityCheckDialog),
      onClose: (result) => {
        resolve(result);
      },
    });
  });
}
