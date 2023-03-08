import { memoize } from "lodash";

const getNormalizedLocale = memoize((locale: string) => {
  const [language = "", country = ""] = locale.replaceAll("_", "-").split("-");
  const lowerLanguage = language.toLowerCase();
  return country ? `${lowerLanguage}-${country.toUpperCase()}` : lowerLanguage;
});

export function getPreferredLocale(locale: string | null | undefined) {
  const userLocale = locale ? getNormalizedLocale(locale) : navigator.language;

  if (userLocale.includes("-")) {
    return userLocale;
  }

  const foundLocale = navigator.languages.find((locale) => {
    const [language, country] = locale.split("-");
    return country && language === userLocale;
  });

  return foundLocale || userLocale;
}

export function findSupportedLocale(
  locales: string[],
  locale: string | null | undefined
) {
  const toLanguage = (locale: string) => locale.split("-")[0];
  const findLocale = (locale: string, tr = (x: string) => x) =>
    locales.find((item) => tr(item).toLowerCase() === locale);

  const fullLocale = (locale || navigator.language).toLowerCase();
  const language = toLanguage(fullLocale);

  return (
    findLocale(fullLocale) ||
    findLocale(language) ||
    findLocale(language, toLanguage)
  );
}
