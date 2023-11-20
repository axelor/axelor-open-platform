import dayjs, { Dayjs } from "dayjs";
import dayjsLocale from "dayjs/locale.json";
import customParseFormat from "dayjs/plugin/customParseFormat";
import isBetween from "dayjs/plugin/isBetween";
import isSameOrAfter from "dayjs/plugin/isSameOrAfter";
import isSameOrBefore from "dayjs/plugin/isSameOrBefore";
import localizedFormat from "dayjs/plugin/localizedFormat";
import relativeTime from "dayjs/plugin/relativeTime";
import weekOfYear from "dayjs/plugin/weekOfYear";

import { limitScale } from "@/utils/format";
import { toKebabCase } from "@/utils/names";
import { session } from "./session";

dayjs.extend(localizedFormat);
dayjs.extend(customParseFormat);
dayjs.extend(relativeTime);
dayjs.extend(weekOfYear);
dayjs.extend(isBetween);
dayjs.extend(isSameOrAfter);
dayjs.extend(isSameOrBefore);

const DEFAULT_LANGUAGE = "en";
const DEFAULT_DATE_FORMAT = "DD/MM/YYYY";
const DEFAULT_CURRENCY_CODE = "EUR";

// prettier-ignore
const SUPPORTED_CURRENCY_CODES: Record<string, string> = {
  "en-au": "AUD",
  "en-ca": "CAD",
  "en-gb": "GBP",
  "en-in": "INR",
  "en-us": "USD",
  "fr-ca": "CAD",
  "fr-ch": "CHF",
  "fr-ma": "MAD",
  "hi": "INR",
  "ja": "JPY",
  "ru": "RUB",
  "zh": "CNY",
};

const getNormalizedLocale = (locale: string) => toKebabCase(locale);
const getShortLocale = (locale: string) => toKebabCase(locale).split("-")[0];
const getCountry = (locale: string) => toKebabCase(locale).split("-")[1];

let locale = "";
let dateFormat = "";
let currency = "";

/*
  en -> EUR, DD/MM/YYYY (defaults)
  en-US -> USD, MM/DD/YYYY
  en-GB -> GBP, DD/MM/YYYY
  fr -> EUR, DD/MM/YYYY
  fr-FR -> EUR, DD/MM/YYYY
*/
async function init() {
  locale = findLocale();
  currency = findCurrencyCode();

  const dayjsLocale = await initDayjs();
  dateFormat = findDateFormat(dayjsLocale);
}

async function initDayjs() {
  const data = await findDayjsLocale();
  dayjs.locale(data);
  return data;
}

async function findDayjsLocale() {
  const supportedLocales = dayjsLocale.map((locale) => locale.key);
  const found = _findLocale(supportedLocales, locale);
  if (found) {
    const { default: data } = await import(
      `../../../node_modules/dayjs/esm/locale/${found}.js`
    );
    return data;
  }
  return null;
}

await init();

// listen for session change
session.subscribe(init);

export function _findLocale(
  locales: readonly string[],
  locale: string,
  tr = getNormalizedLocale,
) {
  const parts = getNormalizedLocale(locale).split("-");
  for (let i = parts.length; i > 0; --i) {
    const current = parts.slice(0, i).join("-");
    const found = locales.find((item) => tr(item) === current);
    if (found) {
      return found;
    }
  }

  return null;
}

function findCurrencyCode() {
  const found = _findLocale(Object.keys(SUPPORTED_CURRENCY_CODES), locale);
  return found ? SUPPORTED_CURRENCY_CODES[found] : DEFAULT_CURRENCY_CODE;
}

function findDateFormat(data: any) {
  if (data) {
    const format = data?.formats?.L;
    if (format) {
      return format
        .replace(/\u200f/g, "") // ar
        .replace(/YYYY年MMMD日/g, "YYYY-MM-DD") // zh-tw
        .replace(/MMM/g, "MM") // Don't support MMM
        .replace(/\bD\b/g, "DD") // D -> DD
        .replace(/\bM\b/g, "MM"); // M -> MM
    } else if (getCountry(locale) === "us") {
      // dayjs has no locale "en-us", and locale "en" has undefined formats
      return "MM/DD/YYYY";
    }
  }
  return dateFormat || DEFAULT_DATE_FORMAT;
}

function findLocale() {
  const userLang = session.info?.user?.lang;
  if (userLang?.includes("-")) {
    return userLang;
  }

  const found =
    userLang && _findLocale(navigator.languages, userLang, getShortLocale);
  return found || userLang || navigator.language || DEFAULT_LANGUAGE;
}

export const moment = dayjs;

export type Moment = Dayjs;

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace l10n {
  export function getLocale() {
    return locale;
  }

  export function findLocale(locales: string[], locale: string) {
    return _findLocale(locales, locale);
  }

  export function getDateFormat() {
    return dateFormat;
  }

  export function formatNumber(
    value: number,
    options?: Intl.NumberFormatOptions,
  ) {
    let { minimumFractionDigits, maximumFractionDigits } = options ?? {};
    minimumFractionDigits = limitScale(minimumFractionDigits);
    maximumFractionDigits = limitScale(maximumFractionDigits);
    return new Intl.NumberFormat(getLocale(), {
      ...options,
      minimumFractionDigits,
      maximumFractionDigits,
    }).format(value);
  }

  export function formatCurrency(
    value: number,
    options?: Intl.NumberFormatOptions,
  ) {
    return new Intl.NumberFormat(getLocale(), {
      style: "currency",
      currency,
      ...options,
    }).format(value);
  }

  export function formatDateTime(
    date: number | Date,
    options?: Intl.DateTimeFormatOptions,
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
