import { useEffect } from "react";

import { AppInfo } from "@/services/client/session";
import { useSession } from "../use-session";

const NAME = "Axelor";
const DESC = "Axelor Entreprise Application";

export function useAppHead() {
  const { appData: info } = useSession();
  useEffect(() => {
    if (info) {
      setTitle(info);
      setIcon(info);
    }
  }, [info]);
}

function setTitle(info: AppInfo) {
  const { name = NAME, description = DESC } = info.app ?? {};
  document.title = `${name} – ${description}`;
}

function setIcon(info: AppInfo) {
  const icon = info.app.icon;
  const elem = document.querySelector("head > link[rel='shortcut icon']") as HTMLLinkElement;

  if (icon && elem) {
    elem.href = icon;
  }
}
