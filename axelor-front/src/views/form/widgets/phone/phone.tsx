import { useAtomValue } from "jotai";
import { ChangeEvent, useEffect, useMemo, useRef } from "react";
import { CountrySelector, usePhoneInput } from "react-international-phone";

import { Box, Input, clsx } from "@axelor/ui";

import { _findLocale, l10n } from "@/services/client/l10n";
import { FieldControl, FieldProps } from "../../builder";
import { useInput } from "../../builder/hooks";

import "react-international-phone/style.css";
import styles from "./phone.module.scss";

// Fallback country codes when country is not found in language code
const FALLBACK_COUNTRIES: Record<string, string> = {
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

export function Phone({
  inputProps,
  ...props
}: FieldProps<string> & {
  inputProps?: Pick<
    React.InputHTMLAttributes<HTMLInputElement>,
    "type" | "autoComplete" | "placeholder" | "onFocus"
  >;
}) {
  const { schema, readonly, widgetAtom, valueAtom, invalid } = props;
  const { uid, placeholder: _placeholder, widgetAttrs } = schema;
  const { placeholderNumberType } = widgetAttrs;

  const { attrs } = useAtomValue(widgetAtom);
  const { focus, required } = attrs;

  const locale = l10n.getLocale();

  const defaultCountry = useMemo(() => {
    // If user locale has no country code, look for a match in `navigator.languages`.
    const [
      language,
      country = _findLocale(
        navigator.languages.filter((l) => l.split("-")[1]),
        locale,
        (l) => l.split("-")[0],
      )
        ?.split("-")[1]
        ?.toLowerCase(),
    ] = locale.split("-").map((value) => value.toLowerCase());
    return country ?? FALLBACK_COUNTRIES[language] ?? language;
  }, [locale]);

  const preferredCountries = useMemo(
    () => [
      ...new Set([
        defaultCountry,
        ...navigator.languages
          .map((l) => l.split("-")[1]?.toLowerCase())
          .filter(Boolean),
      ]),
    ],
    [defaultCountry],
  );

  const { text, onChange, onBlur, onKeyDown, setValue } = useInput(valueAtom, {
    schema,
  });

  const {
    inputValue,
    phone,
    country,
    setCountry,
    handlePhoneValueChange,
    inputRef,
  } = usePhoneInput({
    defaultCountry: "fr",
    value: text,
    onChange: ({ phone, country }) => {
      onChange({
        target: { value: phone !== `+${country.dialCode}` ? phone : "" },
      } as ChangeEvent<HTMLInputElement>);
    },
  });

  const placeholder = useMemo(() => {
    if (_placeholder) return _placeholder;

    const { format = ".".repeat(9), dialCode } = country;
    let phoneFormat = typeof format === "string" ? format : format.default;

    // Special case for French mobile numbers
    if (dialCode === "33" && placeholderNumberType === "MOBILE") {
      phoneFormat = phoneFormat.replace(".", "6");
    }

    let currentNumber = 1;
    const numbers = phoneFormat.replace(/\./g, () => `${currentNumber++ % 10}`);
    const placeholder = `+${dialCode} ${numbers}`;

    return placeholder;
  }, [_placeholder, country, placeholderNumberType]);

  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!text && !containerRef.current?.contains(document.activeElement)) {
      setCountry(defaultCountry);
    }
  }, [defaultCountry, inputRef, setCountry, text]);

  const hasValue = text && text === phone;

  return (
    <FieldControl {...props} className={clsx(styles.container)}>
      <Box
        className={clsx(styles.phone, { [styles.readonly]: readonly })}
        ref={containerRef}
      >
        {(hasValue || !readonly) && (
          <CountrySelector
            selectedCountry={country?.iso2}
            onSelect={(selectedCountry) => {
              if (selectedCountry.iso2 === country.iso2) return;
              setCountry(selectedCountry.iso2);
              setValue(null);
            }}
            hideDropdown={readonly}
            preferredCountries={preferredCountries}
            className={clsx(styles.country)}
          />
        )}
        {readonly ? (
          <Box
            as="a"
            target="_blank"
            href={`tel:${phone}`}
            className={clsx(styles.link)}
          >
            {hasValue && inputValue}
          </Box>
        ) : (
          <Input
            ref={inputRef}
            {...(focus && { key: "focused" })}
            data-input
            type="text"
            id={uid}
            autoFocus={focus}
            placeholder={placeholder}
            value={inputValue}
            invalid={invalid}
            required={required}
            onKeyDown={onKeyDown}
            onChange={handlePhoneValueChange}
            onBlur={onBlur}
            {...inputProps}
          />
        )}
      </Box>
    </FieldControl>
  );
}
