import { useEffect } from "react";

import { useAppSettings } from "@/hooks/use-app-settings";

export function useAppHead() {
  const { name, description, isReady, themeMode } = useAppSettings();
  useEffect(() => {
    if (isReady) {
      setTitle(name, description);
      setIcon(`ws/public/app/icon?mode=${themeMode}`);
    }
  }, [name, description, isReady, themeMode]);
}

function setTitle(name: string, description: string) {
  document.title = `${name} – ${description}`;
}

function setIcon(icon: string | undefined) {
  const elem = document.querySelector(
    "head > link[rel='shortcut icon']",
  ) as HTMLLinkElement;

  if (icon && elem) {
    elem.href = icon;
  }
}
