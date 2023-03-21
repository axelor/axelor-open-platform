import { useMemo } from "react";
import { useSession } from "../use-session/use-session";
import { getPreferredLocale } from "../../utils/locale";

export function useLocale() {
  const lang = useSession().data?.user.lang;
  const locale = useMemo(() => getPreferredLocale(lang), [lang]);
  return locale;
}
