import { useMemo } from "react";
import { useSession } from "../use-session/use-session";
import { getPreferredLocale } from "../../utils/locale";
import { SessionInfo } from "@/services/client/session";

export function useLocale() {
  const {
    user: { lang },
  } = useSession().info || ({} as SessionInfo);
  const preferredLocale = useMemo(() => getPreferredLocale(lang), [lang]);

  return preferredLocale;
}
