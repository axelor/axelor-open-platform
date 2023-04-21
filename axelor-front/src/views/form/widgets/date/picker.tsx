import { l10n } from "@/services/client/l10n";
import { ReactElement, forwardRef, useState } from "react";
import ReactDatePicker, {
  ReactDatePickerProps,
  registerLocale,
  setDefaultLocale,
} from "react-datepicker";

import { useAsyncEffect } from "@/hooks/use-async-effect";
import "react-datepicker/dist/react-datepicker.css";
import "./picker.scss";

const DEFAULT_LOCALE = "en";

const LOCALE_LOADERS: Record<string, any> = {
  [DEFAULT_LOCALE]: () => Promise.resolve(),
  "en-AU": () => import("date-fns/locale/en-AU"),
  "en-CA": () => import("date-fns/locale/en-CA"),
  "en-GB": () => import("date-fns/locale/en-GB"),
  "en-IN": () => import("date-fns/locale/en-IN"),
  "en-US": () => import("date-fns/locale/en-US"),
  fr: () => import("date-fns/locale/fr"),
  "fr-CA": () => import("date-fns/locale/fr-CA"),
  ja: () => import("date-fns/locale/ja"),
  ru: () => import("date-fns/locale/ru"),
  "zh-CN": () => import("date-fns/locale/zh-CN"),
};

const load = async (locale: string) => {
  const short = locale.split(/-_/)[0];
  const loader =
    LOCALE_LOADERS[locale] ??
    LOCALE_LOADERS[short] ??
    LOCALE_LOADERS[DEFAULT_LOCALE];
  return await loader();
};

export const Picker = forwardRef<any, ReactDatePickerProps>((props, ref) => {
  const locale = l10n.getLocale() || DEFAULT_LOCALE;
  const [loaded, setLoaded] = useState(false);

  useAsyncEffect(
    async (signal) => {
      const mod = await load(locale);
      if (signal.aborted) return;
      if (mod) {
        registerLocale(locale, mod.default);
      }
      setDefaultLocale(locale);
      setLoaded(true);
    },
    [locale]
  );

  return (loaded && (
    <ReactDatePicker {...props} ref={ref} locale={locale} />
  )) as ReactElement;
});
