import { useSession } from "../use-session";
import { i18n } from "@/services/client/i18n";

const NAME = "Axelor";
const DESCRIPTION = "Axelor Entreprise Application";
export const COPYRIGHT = `© 2005–${new Date().getFullYear()} Axelor. ${i18n.get(
  "All Rights Reserved",
)}.`;

export function useAppSettings() {
  const { data: info, state } = useSession();

  const isReady = state === "hasData";
  const name = info?.application.name ?? NAME;
  const description = info?.application.description ?? DESCRIPTION;
  const icon = info?.application.icon;
  const copyright =
    info?.application.copyright?.replace("&copy;", "©") || COPYRIGHT;

  return {
    isReady,
    name,
    description,
    copyright,
    icon,
  };
}
