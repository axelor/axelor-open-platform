import { l10n } from "@/services/client/l10n";
import { ReactElement, forwardRef, useEffect, useState } from "react";
import ReactDatePicker, {
  ReactDatePickerProps,
  registerLocale,
  setDefaultLocale,
} from "react-datepicker";

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

export const Picker = forwardRef<any, ReactDatePickerProps>((props, ref) => {
  const locale = l10n.getLocale() || DEFAULT_LOCALE;
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    let cancelled = false;
    const loader = LOCALE_LOADERS[locale];
    loader().then((module: any) => {
      if (!cancelled) {
        if (module) {
          registerLocale(locale, module.default);
        }
        setDefaultLocale(locale);
        setLoaded(true);
      }
    });
    return () => {
      cancelled = true;
    };
  }, [locale]);

  return (loaded && (
    <ReactDatePicker {...props} ref={ref} locale={locale} />
  )) as ReactElement;
});
