import { useEffect } from "react";

import { SessionInfo } from "@/services/client/session";
import { useSession } from "../use-session";

const NAME = "Axelor";
const DESC = "Axelor Entreprise Application";

export function useAppHead() {
  const { data: info } = useSession();
  useEffect(() => {
    if (info) {
      setTitle(info);
      setIcon(info);
    }
  }, [info]);
}

function setTitle(info: SessionInfo) {
  const { name = NAME, description = DESC } = info.app ?? {};
  document.title = `${name} – ${description}`;
}

function setIcon(info: SessionInfo) {
  const icon = info.app.icon;
  const elem = document.querySelector("head > link[rel='shortcut icon']") as HTMLLinkElement;

  if (icon && elem) {
    elem.href = icon;
  }
}
