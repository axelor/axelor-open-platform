import { atom, useAtom } from "jotai";

import moment from "moment";

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
  "af": () => import("moment/dist/locale/af"),
  "ar-dz": () => import("moment/dist/locale/ar-dz"),
  "ar-kw": () => import("moment/dist/locale/ar-kw"),
  "ar-ly": () => import("moment/dist/locale/ar-ly"),
  "ar-ma": () => import("moment/dist/locale/ar-ma"),
  "ar-sa": () => import("moment/dist/locale/ar-sa"),
  "ar-tn": () => import("moment/dist/locale/ar-tn"),
  "ar": () => import("moment/dist/locale/ar"),
  "az": () => import("moment/dist/locale/az"),
  "be": () => import("moment/dist/locale/be"),
  "bg": () => import("moment/dist/locale/bg"),
  "bm": () => import("moment/dist/locale/bm"),
  "bn-bd": () => import("moment/dist/locale/bn-bd"),
  "bn": () => import("moment/dist/locale/bn"),
  "bo": () => import("moment/dist/locale/bo"),
  "br": () => import("moment/dist/locale/br"),
  "bs": () => import("moment/dist/locale/bs"),
  "ca": () => import("moment/dist/locale/ca"),
  "cs": () => import("moment/dist/locale/cs"),
  "cv": () => import("moment/dist/locale/cv"),
  "cy": () => import("moment/dist/locale/cy"),
  "da": () => import("moment/dist/locale/da"),
  "de-at": () => import("moment/dist/locale/de-at"),
  "de-ch": () => import("moment/dist/locale/de-ch"),
  "de": () => import("moment/dist/locale/de"),
  "dv": () => import("moment/dist/locale/dv"),
  "el": () => import("moment/dist/locale/el"),
  "en-au": () => import("moment/dist/locale/en-au"),
  "en-ca": () => import("moment/dist/locale/en-ca"),
  "en-gb": () => import("moment/dist/locale/en-gb"),
  "en-ie": () => import("moment/dist/locale/en-ie"),
  "en-il": () => import("moment/dist/locale/en-il"),
  "en-in": () => import("moment/dist/locale/en-in"),
  "en-nz": () => import("moment/dist/locale/en-nz"),
  "en-sg": () => import("moment/dist/locale/en-sg"),
  "eo": () => import("moment/dist/locale/eo"),
  "es-do": () => import("moment/dist/locale/es-do"),
  "es-mx": () => import("moment/dist/locale/es-mx"),
  "es-us": () => import("moment/dist/locale/es-us"),
  "es": () => import("moment/dist/locale/es"),
  "et": () => import("moment/dist/locale/et"),
  "eu": () => import("moment/dist/locale/eu"),
  "fa": () => import("moment/dist/locale/fa"),
  "fi": () => import("moment/dist/locale/fi"),
  "fil": () => import("moment/dist/locale/fil"),
  "fo": () => import("moment/dist/locale/fo"),
  "fr-ca": () => import("moment/dist/locale/fr-ca"),
  "fr-ch": () => import("moment/dist/locale/fr-ch"),
  "fr": () => import("moment/dist/locale/fr"),
  "fy": () => import("moment/dist/locale/fy"),
  "ga": () => import("moment/dist/locale/ga"),
  "gd": () => import("moment/dist/locale/gd"),
  "gl": () => import("moment/dist/locale/gl"),
  "gom-deva": () => import("moment/dist/locale/gom-deva"),
  "gom-latn": () => import("moment/dist/locale/gom-latn"),
  "gu": () => import("moment/dist/locale/gu"),
  "he": () => import("moment/dist/locale/he"),
  "hi": () => import("moment/dist/locale/hi"),
  "hr": () => import("moment/dist/locale/hr"),
  "hu": () => import("moment/dist/locale/hu"),
  "hy-am": () => import("moment/dist/locale/hy-am"),
  "id": () => import("moment/dist/locale/id"),
  "is": () => import("moment/dist/locale/is"),
  "it-ch": () => import("moment/dist/locale/it-ch"),
  "it": () => import("moment/dist/locale/it"),
  "ja": () => import("moment/dist/locale/ja"),
  "jv": () => import("moment/dist/locale/jv"),
  "ka": () => import("moment/dist/locale/ka"),
  "kk": () => import("moment/dist/locale/kk"),
  "km": () => import("moment/dist/locale/km"),
  "kn": () => import("moment/dist/locale/kn"),
  "ko": () => import("moment/dist/locale/ko"),
  "ku": () => import("moment/dist/locale/ku"),
  "ky": () => import("moment/dist/locale/ky"),
  "lb": () => import("moment/dist/locale/lb"),
  "lo": () => import("moment/dist/locale/lo"),
  "lt": () => import("moment/dist/locale/lt"),
  "lv": () => import("moment/dist/locale/lv"),
  "me": () => import("moment/dist/locale/me"),
  "mi": () => import("moment/dist/locale/mi"),
  "mk": () => import("moment/dist/locale/mk"),
  "ml": () => import("moment/dist/locale/ml"),
  "mn": () => import("moment/dist/locale/mn"),
  "mr": () => import("moment/dist/locale/mr"),
  "ms-my": () => import("moment/dist/locale/ms-my"),
  "ms": () => import("moment/dist/locale/ms"),
  "mt": () => import("moment/dist/locale/mt"),
  "my": () => import("moment/dist/locale/my"),
  "nb": () => import("moment/dist/locale/nb"),
  "ne": () => import("moment/dist/locale/ne"),
  "nl-be": () => import("moment/dist/locale/nl-be"),
  "nl": () => import("moment/dist/locale/nl"),
  "nn": () => import("moment/dist/locale/nn"),
  "oc-lnc": () => import("moment/dist/locale/oc-lnc"),
  "pa-in": () => import("moment/dist/locale/pa-in"),
  "pl": () => import("moment/dist/locale/pl"),
  "pt-br": () => import("moment/dist/locale/pt-br"),
  "pt": () => import("moment/dist/locale/pt"),
  "ro": () => import("moment/dist/locale/ro"),
  "ru": () => import("moment/dist/locale/ru"),
  "sd": () => import("moment/dist/locale/sd"),
  "se": () => import("moment/dist/locale/se"),
  "si": () => import("moment/dist/locale/si"),
  "sk": () => import("moment/dist/locale/sk"),
  "sl": () => import("moment/dist/locale/sl"),
  "sq": () => import("moment/dist/locale/sq"),
  "sr-cyrl": () => import("moment/dist/locale/sr-cyrl"),
  "sr": () => import("moment/dist/locale/sr"),
  "ss": () => import("moment/dist/locale/ss"),
  "sv": () => import("moment/dist/locale/sv"),
  "sw": () => import("moment/dist/locale/sw"),
  "ta": () => import("moment/dist/locale/ta"),
  "te": () => import("moment/dist/locale/te"),
  "tet": () => import("moment/dist/locale/tet"),
  "tg": () => import("moment/dist/locale/tg"),
  "th": () => import("moment/dist/locale/th"),
  "tk": () => import("moment/dist/locale/tk"),
  "tl-ph": () => import("moment/dist/locale/tl-ph"),
  "tlh": () => import("moment/dist/locale/tlh"),
  "tr": () => import("moment/dist/locale/tr"),
  "tzl": () => import("moment/dist/locale/tzl"),
  "tzm-latn": () => import("moment/dist/locale/tzm-latn"),
  "tzm": () => import("moment/dist/locale/tzm"),
  "ug-cn": () => import("moment/dist/locale/ug-cn"),
  "uk": () => import("moment/dist/locale/uk"),
  "ur": () => import("moment/dist/locale/ur"),
  "uz-latn": () => import("moment/dist/locale/uz-latn"),
  "uz": () => import("moment/dist/locale/uz"),
  "vi": () => import("moment/dist/locale/vi"),
  "x-pseudo": () => import("moment/dist/locale/x-pseudo"),
  "yo": () => import("moment/dist/locale/yo"),
  "zh-cn": () => import("moment/dist/locale/zh-cn"),
  "zh-hk": () => import("moment/dist/locale/zh-hk"),
  "zh-mo": () => import("moment/dist/locale/zh-mo"),
  "zh-tw": () => import("moment/dist/locale/zh-tw"),
};

const momentLocaleAtom = atom<string | undefined>(undefined);

export function useMomentLocale() {
  const locale = useLocale();
  const [momentLocale, setMomentLocale] = useAtom(momentLocaleAtom);

  useAsyncEffect(async () => {
    const supportedLocale =
      findSupportedLocale(Object.keys(LOCALE_IMPORTERS), locale) ||
      DEFAULT_LOCALE;

    const importer = LOCALE_IMPORTERS[supportedLocale];
    await importer();
    setMomentLocale(moment.locale(supportedLocale));
  }, [locale, setMomentLocale]);

  return momentLocale;
}
