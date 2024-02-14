import { PhoneNumberUtil } from "google-libphonenumber";
import { defaultCountries } from "react-international-phone";

import flags from "@/assets/flags.svg";
import { i18n } from "@/services/client/i18n";

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

const UNKNOWN_NUMBER_TYPE = () => i18n.get("Unknown");

const NUMBER_TYPES = [
  () => i18n.get("Fixed line"),
  () => i18n.get("Mobile"),
  () => i18n.get("Fixed line or mobile"),
  () => i18n.get("Toll free"),
  () => i18n.get("Premium rate"),
  () => i18n.get("Shared cost"),
  () => i18n.get("VoIP"),
  () => i18n.get("Personal number"),
  () => i18n.get("Pager"),
  () => i18n.get("UAN"),
  () => i18n.get("Voicemail"),
];

const phoneUtil = PhoneNumberUtil.getInstance();

export function getPhoneInfo(phone: string) {
  try {
    const number = phoneUtil.parseAndKeepRawInput(phone);
    const isValidNumber = phoneUtil.isValidNumber(number);
    const numberType = isValidNumber ? phoneUtil.getNumberType(number) : -1;
    return {
      isValidNumber,
      numberType: NUMBER_TYPES[numberType as number]() ?? UNKNOWN_NUMBER_TYPE(),
    };
  } catch (error) {
    return {
      isValidNumber: false,
      numberType: UNKNOWN_NUMBER_TYPE(),
    };
  }
}
