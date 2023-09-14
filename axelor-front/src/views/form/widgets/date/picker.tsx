import { forwardRef, useState } from "react";
import ReactDatePicker, {
  ReactDatePickerProps,
  getDefaultLocale,
  registerLocale,
  setDefaultLocale,
} from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";

import { useAsyncEffect } from "@/hooks/use-async-effect";
import { l10n } from "@/services/client/l10n";

import { ViewerInput } from "../string/viewer";

import "./picker.scss";

const DEFAULT_LOCALE = "en";

// prettier-ignore
const LOCALE_LOADERS: Record<string, () => Promise<any>> = {
  [DEFAULT_LOCALE]: () => Promise.resolve(),
  "ar-DZ": () => import("date-fns/locale/ar-DZ"),
  "ar-MA": () => import("date-fns/locale/ar-MA"),
  "ar-SA": () => import("date-fns/locale/ar-SA"),
  "ar-TN": () => import("date-fns/locale/ar-TN"),
  "de-AT": () => import("date-fns/locale/de-AT"),
  "de": () => import("date-fns/locale/de"),
  "en-AU": () => import("date-fns/locale/en-AU"),
  "en-CA": () => import("date-fns/locale/en-CA"),
  "en-GB": () => import("date-fns/locale/en-GB"),
  "en-IN": () => import("date-fns/locale/en-IN"),
  "en-NZ": () => import("date-fns/locale/en-NZ"),
  "en-US": () => import("date-fns/locale/en-US"),
  "en-ZA": () => import("date-fns/locale/en-ZA"),
  "es": () => import("date-fns/locale/es"),
  "fr-CA": () => import("date-fns/locale/fr-CA"),
  "fr-CH": () => import("date-fns/locale/fr-CH"),
  "fr": () => import("date-fns/locale/fr"),
  "hi": () => import("date-fns/locale/hi"),
  "it": () => import("date-fns/locale/it"),
  "ja": () => import("date-fns/locale/ja"),
  "ko": () => import("date-fns/locale/ko"),
  "nl-BE": () => import("date-fns/locale/nl-BE"),
  "nl": () => import("date-fns/locale/nl"),
  "pt-BR": () => import("date-fns/locale/pt-BR"),
  "pt": () => import("date-fns/locale/pt"),
  "ru": () => import("date-fns/locale/ru"),
  "zh-CN": () => import("date-fns/locale/zh-CN"),
  "zh-TW": () => import("date-fns/locale/zh-TW"),
};

const load = async (locale: string) => {
  const short = locale.split(/[-_]/)[0];
  const loader =
    LOCALE_LOADERS[locale] ??
    LOCALE_LOADERS[short] ??
    LOCALE_LOADERS[DEFAULT_LOCALE];
  return await loader();
};

export const Picker = forwardRef<
  any,
  ReactDatePickerProps & {
    textValue?: string;
  }
>(({ textValue, ...props }, ref) => {
  const locale = l10n.getLocale() || DEFAULT_LOCALE;
  const defaultLocale = getDefaultLocale();
  const [loaded, setLoaded] = useState(locale === defaultLocale);

  useAsyncEffect(
    async (signal) => {
      if (loaded) return;
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

  if (loaded) {
    return <ReactDatePicker {...props} ref={ref} locale={locale} />;
  }

  return <ViewerInput value={textValue || ""} />;
});
