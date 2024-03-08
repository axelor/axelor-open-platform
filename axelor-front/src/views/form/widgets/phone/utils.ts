import parsePhoneNumber from "libphonenumber-js/max";
import { defaultCountries } from "react-international-phone";

import flags from "@/assets/flags.svg";
import { i18n } from "@/services/client/i18n";
import { _findLocale, l10n } from "@/services/client/l10n";

// Fallback country codes when country is not found in language code
export const FALLBACK_COUNTRIES: Record<string, string> = {
  af: "za", // Afrikaans -> South Africa
  ar: "sa", // Arabic -> Saudi Arabia
  be: "by", // Belarusian -> Belarus
  bn: "bd", // Bengali -> Bangladesh
  bs: "ba", // Bosnian -> Bosnia and Herzegovina
  cs: "cz", // Czech -> Czech Republic
  da: "dk", // Danish -> Denmark
  el: "gr", // Greek -> Greece
  en: "us", // English -> United States
  et: "ee", // Estonian -> Estonia
  fa: "ir", // Persian -> Iran
  gu: "in", // Gujarati -> India
  he: "il", // Hebrew -> Israel
  hi: "in", // Hindi -> India
  ja: "jp", // Japanese -> Japan
  ko: "kr", // Korean -> South Korea
  ms: "my", // Malay -> Malaysia
  sv: "se", // Swedish -> Sweden
  uk: "ua", // Ukrainian -> Ukraine
  vi: "vn", // Vietnamese -> Vietnam
  zh: "cn", // Chinese -> China
};

export const FLAGS = defaultCountries.map((country) => {
  const iso2 = country[1];
  const src = flags;
  return { iso2, src };
});

const NUMBER_TYPES: Record<string, () => string> = {
  FIXED_LINE: () => i18n.get("Fixed line"),
  MOBILE: () => i18n.get("Mobile"),
  FIXED_LINE_OR_MOBILE: () => i18n.get("Fixed line or mobile"),
  TOLL_FREE: () => i18n.get("Toll free"),
  PREMIUM_RATE: () => i18n.get("Premium rate"),
  SHARED_COST: () => i18n.get("Shared cost"),
  VOIP: () => i18n.get("VoIP"),
  PERSONAL_NUMBER: () => i18n.get("Personal number"),
  PAGER: () => i18n.get("Pager"),
  UAN: () => i18n.get("UAN"),
  VOICEMAIL: () => i18n.get("Voicemail"),
  UNKNOWN: () => i18n.get("Unknown"),
};

export function getPhoneInfo(phone?: string) {
  const phoneNumber = parsePhoneNumber(phone ?? "");
  return {
    isPossible: () => phoneNumber?.isPossible(),
    isValid: () => phoneNumber?.isValid(),
    getDisplayType: () => {
      const type = phoneNumber?.getType();
      return (type && NUMBER_TYPES[type]?.()) ?? NUMBER_TYPES.UNKNOWN();
    },
  };
}

export function useDefaultCountry(
  initialCountry?: string,
  onlyCountries?: string[],
) {
  let defaultCountry = initialCountry;

  if (!defaultCountry) {
    const locale = l10n.getLocale();

    // If user locale has no country code, look for a match in `navigator.languages`.
    const [
      language,
      country = _findLocale(
        navigator.languages.filter((language) => language.split("-")[1]),
        locale,
        (language) => language.split("-")[0],
      )
        ?.split("-")[1]
        ?.toLowerCase(),
    ] = locale.split("-").map((value) => value.toLowerCase());
    defaultCountry = country ?? FALLBACK_COUNTRIES[language] ?? language;
  }

  if (onlyCountries?.length && !onlyCountries.includes(defaultCountry)) {
    defaultCountry = onlyCountries[0];
  }

  return defaultCountry;
}
