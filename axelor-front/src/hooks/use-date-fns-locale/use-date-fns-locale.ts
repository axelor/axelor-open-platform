import { atom, useAtom } from "jotai";
import { registerLocale, setDefaultLocale } from "react-datepicker";

import { findSupportedLocale } from "@/utils/locale";

import { useLocale } from "../use-locale/use-locale";
import { useAsyncEffect } from "../use-async-effect";

interface LocaleImporters {
  [locale: string]: () => Promise<{ default: any } | void>;
}

const DEFAULT_LOCALE = "en";

// prettier-ignore
const LOCALE_IMPORTERS: LocaleImporters = {
  [DEFAULT_LOCALE]: () => Promise.resolve(),
  "af": () => import("date-fns/locale/af"),
  "ar-DZ": () => import("date-fns/locale/ar-DZ"),
  "ar-MA": () => import("date-fns/locale/ar-MA"),
  "ar-SA": () => import("date-fns/locale/ar-SA"),
  "ar-TN": () => import("date-fns/locale/ar-TN"),
  "az": () => import("date-fns/locale/az"),
  "be": () => import("date-fns/locale/be"),
  "bg": () => import("date-fns/locale/bg"),
  "bn": () => import("date-fns/locale/bn"),
  "bs": () => import("date-fns/locale/bs"),
  "ca": () => import("date-fns/locale/ca"),
  "cs": () => import("date-fns/locale/cs"),
  "cy": () => import("date-fns/locale/cy"),
  "da": () => import("date-fns/locale/da"),
  "de": () => import("date-fns/locale/de"),
  "de-AT": () => import("date-fns/locale/de-AT"),
  "el": () => import("date-fns/locale/el"),
  "en-AU": () => import("date-fns/locale/en-AU"),
  "en-CA": () => import("date-fns/locale/en-CA"),
  "en-GB": () => import("date-fns/locale/en-GB"),
  "en-IN": () => import("date-fns/locale/en-IN"),
  "en-NZ": () => import("date-fns/locale/en-NZ"),
  "en-US": () => import("date-fns/locale/en-US"),
  "en-ZA": () => import("date-fns/locale/en-ZA"),
  "eo": () => import("date-fns/locale/eo"),
  "es": () => import("date-fns/locale/es"),
  "et": () => import("date-fns/locale/et"),
  "eu": () => import("date-fns/locale/eu"),
  "fa-IR": () => import("date-fns/locale/fa-IR"),
  "fi": () => import("date-fns/locale/fi"),
  "fr": () => import("date-fns/locale/fr"),
  "fr-CA": () => import("date-fns/locale/fr-CA"),
  "fr-CH": () => import("date-fns/locale/fr-CH"),
  "gd": () => import("date-fns/locale/gd"),
  "gl": () => import("date-fns/locale/gl"),
  "gu": () => import("date-fns/locale/gu"),
  "he": () => import("date-fns/locale/he"),
  "hi": () => import("date-fns/locale/hi"),
  "hr": () => import("date-fns/locale/hr"),
  "ht": () => import("date-fns/locale/ht"),
  "hu": () => import("date-fns/locale/hu"),
  "hy": () => import("date-fns/locale/hy"),
  "id": () => import("date-fns/locale/id"),
  "is": () => import("date-fns/locale/is"),
  "it": () => import("date-fns/locale/it"),
  "ja": () => import("date-fns/locale/ja"),
  "ja-Hira": () => import("date-fns/locale/ja-Hira"),
  "ka": () => import("date-fns/locale/ka"),
  "kk": () => import("date-fns/locale/kk"),
  "kn": () => import("date-fns/locale/kn"),
  "ko": () => import("date-fns/locale/ko"),
  "lb": () => import("date-fns/locale/lb"),
  "lt": () => import("date-fns/locale/lt"),
  "lv": () => import("date-fns/locale/lv"),
  "mk": () => import("date-fns/locale/mk"),
  "mn": () => import("date-fns/locale/mn"),
  "ms": () => import("date-fns/locale/ms"),
  "mt": () => import("date-fns/locale/mt"),
  "nb": () => import("date-fns/locale/nb"),
  "nl": () => import("date-fns/locale/nl"),
  "nl-BE": () => import("date-fns/locale/nl-BE"),
  "nn": () => import("date-fns/locale/nn"),
  "pl": () => import("date-fns/locale/pl"),
  "pt": () => import("date-fns/locale/pt"),
  "pt-BR": () => import("date-fns/locale/pt-BR"),
  "ro": () => import("date-fns/locale/ro"),
  "ru": () => import("date-fns/locale/ru"),
  "sk": () => import("date-fns/locale/sk"),
  "sl": () => import("date-fns/locale/sl"),
  "sq": () => import("date-fns/locale/sq"),
  "sr": () => import("date-fns/locale/sr"),
  "sr-Latn": () => import("date-fns/locale/sr-Latn"),
  "sv": () => import("date-fns/locale/sv"),
  "ta": () => import("date-fns/locale/ta"),
  "te": () => import("date-fns/locale/te"),
  "th": () => import("date-fns/locale/th"),
  "tr": () => import("date-fns/locale/tr"),
  "ug": () => import("date-fns/locale/ug"),
  "uk": () => import("date-fns/locale/uk"),
  "uz": () => import("date-fns/locale/uz"),
  "vi": () => import("date-fns/locale/vi"),
  "zh-CN": () => import("date-fns/locale/zh-CN"),
  "zh-TW": () => import("date-fns/locale/zh-TW"),
};

type DateFnsLocale = {
  name: string;
  data?: any;
};

const dateFnsLocaleAtom = atom<DateFnsLocale | undefined>(undefined);

export function useDateFnsLocale() {
  const locale = useLocale();
  const [dateFnsLocale, setDateFnsLocale] = useAtom(dateFnsLocaleAtom);

  useAsyncEffect(async () => {
    const supportedLocale =
      findSupportedLocale(Object.keys(LOCALE_IMPORTERS), locale) ||
      DEFAULT_LOCALE;

    const result = { name: supportedLocale } as DateFnsLocale;
    const importer = LOCALE_IMPORTERS[supportedLocale];
    const module = await importer();
    if (module) {
      const localeData = module.default;
      result.data = localeData;
      registerLocale(supportedLocale, localeData);
    }
    setDefaultLocale(supportedLocale);
    setDateFnsLocale(result);
  }, [locale, setDateFnsLocale]);

  return dateFnsLocale;
}
