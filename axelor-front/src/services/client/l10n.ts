import dayjs from "dayjs";
import dayjsLocale from "dayjs/locale.json";
import customParseFormat from "dayjs/plugin/customParseFormat";
import relativeTime from "dayjs/plugin/relativeTime";

import { toKebabCase } from "@/utils/names";
import { session } from "./session";

dayjs.extend(customParseFormat);
dayjs.extend(relativeTime);

const DATE_FORMAT = "DD/MM/YYYY";
const SUPPORTED_LOCALE = dayjsLocale.reduce((prev, item) => ({
  ...prev,
  [item.key]: true,
}));

const DEFAULT_CURRENCY_CODE = "EUR";
const SUPPORTED_CURRENCY_CODES: Record<string, string> = {
  en: "USD",
  en_us: "USD",
  en_uk: "GBP",
  en_in: "INR",
  en_ca: "CAD",
  en_au: "AUD",
  fr_ca: "CAD",
  fr_ma: "MAD",
  zh: "CNY",
  ja: "JPY",
  ru: "RUB",
};

const normalizeLocale = (locale: string) => toKebabCase(locale);
const shortLocale = (locale: string) => toKebabCase(locale).split("-")[0];

let locale = "";
let dateFormat = "";
let currency = "";

async function init() {
  locale = findLocale();
  currency = findCurrencyCode();
  dateFormat = await findDateFormat();
}

await init();

// listen for session change
session.subscribe(init);

function findCurrencyCode() {
  return (
    SUPPORTED_CURRENCY_CODES[normalizeLocale(locale)] ??
    SUPPORTED_CURRENCY_CODES[shortLocale(locale)] ??
    DEFAULT_CURRENCY_CODE
  );
}

async function findDateFormat() {
  const langs = [normalizeLocale(locale), shortLocale(locale)];
  const found = langs.find((x) => x in SUPPORTED_LOCALE);
  if (found) {
    const { default: data } = await import(
      `../../../node_modules/dayjs/esm/locale/${found}.js`
    );
    const format = data?.formats?.L;
    if (format) {
      return format
        .replace(/\u200f/g, "") // ar
        .replace(/YYYY年MMMD日/g, "YYYY-MM-DD") // zh-tw
        .replace(/MMM/g, "MM") // Don't support MMM
        .replace(/\bD\b/g, "DD") // D -> DD
        .replace(/\bM\b/g, "MM"); // M -> MM
    }
  }
  return dateFormat || DATE_FORMAT;
}

function findLocale() {
  const userLang = session.info?.user.lang;
  if (userLang?.includes("-")) {
    return userLang;
  }
  const lang =
    navigator.languages
      .filter((x) => x && x.length > 0)
      .find((x) => x === userLang || shortLocale(x) === userLang) ??
    navigator.language;
  return lang ?? "en";
}

export const moment = dayjs;

export module l10n {
  export function getLocale() {
    return locale;
  }

  export function getDateFormat() {
    return dateFormat;
  }

  export function formatNumber(
    value: number,
    options?: Intl.NumberFormatOptions
  ) {
    return new Intl.NumberFormat(getLocale(), options).format(value);
  }

  export function formatDecimal(
    value: number,
    options?: Intl.NumberFormatOptions
  ) {
    return new Intl.NumberFormat(getLocale(), {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
      ...options,
    }).format(value);
  }

  export function formatCurrency(
    value: number,
    options?: Intl.NumberFormatOptions
  ) {
    return new Intl.NumberFormat(getLocale(), {
      style: "currency",
      currency,
      ...options,
    }).format(value);
  }

  export function formatDateTime(
    date: number | Date,
    options?: Intl.DateTimeFormatOptions
  ) {
    return new Intl.DateTimeFormat(getLocale(), {
      day: "numeric",
      month: "numeric",
      year: "numeric",
      hour: "numeric",
      minute: "numeric",
      second: "numeric",
      hour12: false,
      ...options,
    }).format(date);
  }
}
