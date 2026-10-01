import { useAtomValue } from "jotai";
import {
  ChangeEvent,
  ClipboardEvent,
  FocusEvent,
  InputHTMLAttributes,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import {
  CountryData,
  CountrySelectorDropdown,
  FlagImage,
  getActiveFormattingMask,
  ParsedCountry,
  usePhoneInput,
} from "react-international-phone";

import { AdornedInput, Box, Button, Portal, clsx } from "@axelor/ui";

import { Icon } from "@/components/icon";
import { TextLink } from "@/components/text-link";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { i18n } from "@/services/client/i18n";
import { useViewRoute } from "@/view-containers/views/scope";
import { FieldControl, FieldProps } from "../../builder";
import { useInput } from "../../builder/hooks";
import {
  DEFAULT_COUNTRIES,
  FLAGS,
  getPhoneInfo,
  normalizePastedPhoneValue,
  useDefaultCountry,
} from "./utils";

import "react-international-phone/style.css";

import flags from "@/assets/flags.svg";
import styles from "./phone.module.scss";

export function Phone({
  inputProps,
  ...props
}: FieldProps<string> & {
  inputProps?: Pick<
    InputHTMLAttributes<HTMLInputElement>,
    "type" | "autoComplete" | "placeholder" | "onFocus"
  >;
}) {
  const { schema, readonly, widgetAtom, valueAtom, invalid } = props;
  const { uid, placeholder: _placeholder, widgetAttrs } = schema;
  const {
    placeholderNumberType,
    initialCountry,
    preferredCountries: _preferredCountries,
    onlyCountries: _onlyCountries,
  }: {
    placeholderNumberType?: "FIXED_LINE" | "MOBILE";
    initialCountry?: string;
    preferredCountries?: string;
    onlyCountries?: string;
  } = widgetAttrs;

  const { attrs } = useAtomValue(widgetAtom);
  const { focus, required } = attrs;

  const onlyCountries = useMemo(
    () =>
      _onlyCountries?.split(/\W+/).map((country) => country.toLowerCase()) ??
      [],
    [_onlyCountries],
  );

  const defaultCountry = useDefaultCountry(initialCountry, onlyCountries);

  const preferredCountries = useMemo(() => {
    if (_preferredCountries) {
      return _preferredCountries
        .split(/\W+/)
        .map((country) => country.toLowerCase());
    }

    return [
      ...new Set([
        defaultCountry,
        ...navigator.languages
          .map((language) => language.split("-")[1]?.toLowerCase())
          .filter(
            (country) =>
              country &&
              (!onlyCountries.length || onlyCountries.includes(country)),
          ),
      ]),
    ];
  }, [_preferredCountries, defaultCountry, onlyCountries]);

  const value = useAtomValue(valueAtom);

  const {
    text: _text,
    onChange,
    onBlur: _onBlur,
    onKeyDown,
    setValue,
  } = useInput(valueAtom, {
    schema,
  });

  const text = _text;
  const noPrefix = !!text && !text.startsWith("+");

  const countries = useMemo(() => {
    // Filter out countries that are not in `onlyCountries`, if specified.
    let countries = onlyCountries.length
      ? DEFAULT_COUNTRIES.filter((country) =>
          onlyCountries.includes(country[1]),
        )
      : DEFAULT_COUNTRIES;

    // Translate country names
    countries = countries.map((country) => {
      const [name, ...rest] = country;
      return [i18n.get(name), ...rest] as CountryData;
    });
    countries.sort((a, b) => a[0].localeCompare(b[0]));

    return countries;
  }, [onlyCountries]);

  const selectedDialCodeRef = useRef<string>(undefined);

  // Avoids feeding usePhoneInput a stale/cleared value that would look
  // like an external change and reset it mid-typing.
  const lastPhoneRef = useRef<string>("");
  const lastEmittedRef = useRef<string | undefined>(undefined);

  const handlePhoneChange = useCallback(
    ({ phone, country }: { phone: string; country: ParsedCountry }) => {
      lastPhoneRef.current = phone;
      if (noPrefix) return;
      const dialCode = `+${country.dialCode}`;
      const selectedDialCode = selectedDialCodeRef.current;
      if (selectedDialCode === phone) {
        selectedDialCodeRef.current = undefined;
      }
      const emittedValue =
        phone !== dialCode || selectedDialCode === phone ? phone : "";
      lastEmittedRef.current = emittedValue;
      onChange({
        target: { value: emittedValue },
      } as ChangeEvent<HTMLInputElement>);
    },
    [noPrefix, onChange],
  );

  const handleNationalChange = useCallback(
    (e: ChangeEvent<HTMLInputElement>) => {
      const sanitized = e.target.value.replace(/[^\d\s+\-().]/g, "");
      onChange({
        ...e,
        target: { ...e.target, value: sanitized },
      } as ChangeEvent<HTMLInputElement>);
    },
    [onChange],
  );

  /* eslint-disable react-hooks/refs -- lastEmittedRef/lastPhoneRef only track
   * our own last emission to detect the onChange echo; they must not trigger a
   * re-render, so they are read during render on purpose. */
  const {
    inputValue,
    phone,
    country,
    setCountry,
    handlePhoneValueChange,
    inputRef,
  } = usePhoneInput({
    defaultCountry,
    countries: countries,
    // If `text` matches our last emission, it's our own onChange echoing
    // back; feed the hook its own phone instead so it sees no change.
    value: noPrefix
      ? ""
      : text === lastEmittedRef.current
        ? lastPhoneRef.current
        : text,
    onChange: handlePhoneChange,
    disableDialCodePrefill: true,
  });
  /* eslint-enable react-hooks/refs */
  const applyPhoneInputValue = useCallback(
    (value: string, data: string = value, selectionStart = value.length) => {
      handlePhoneValueChange({
        preventDefault() {},
        nativeEvent: { inputType: "insertFromPaste", data },
        target: { value, selectionStart },
      } as unknown as ChangeEvent<HTMLInputElement>);
    },
    [handlePhoneValueChange],
  );

  const onBlur = useCallback(() => {
    const dialCode = `+${country.dialCode}`;
    _onBlur({
      target: { value: phone !== dialCode || text === phone ? phone : "" },
    } as FocusEvent<HTMLInputElement>);
  }, [_onBlur, country.dialCode, phone, text]);

  const { id: routeId } = useViewRoute();
  const routeIdRef = useRef(routeId);

  useEffect(() => {
    if (routeId === routeIdRef.current || text) return;
    routeIdRef.current = routeId;
    if (country.iso2 !== defaultCountry) {
      setCountry(defaultCountry);
    }
  }, [routeId, text, country.iso2, defaultCountry, setCountry]);

  const placeholder = useMemo(() => {
    if (_placeholder) return _placeholder;

    const { dialCode } = country;

    // For placeholder, we just need the default mask for specified country.
    let phoneFormat = getActiveFormattingMask({ phone: "", country });

    // Special case for French mobile phone numbers
    if (
      dialCode === "33" &&
      placeholderNumberType?.toUpperCase() === "MOBILE"
    ) {
      phoneFormat = phoneFormat.replace(".", "6");
    }

    let currentNumber = 0;
    const numbers = phoneFormat.replace(/\./g, () => `${++currentNumber % 10}`);
    const placeholder = noPrefix ? numbers : `+${dialCode} ${numbers}`;

    return placeholder;
  }, [_placeholder, country, noPrefix, placeholderNumberType]);

  const buttonRef = useRef<HTMLButtonElement>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const [showDropdown, setShowDropdown] = useState<boolean>(false);

  const toggleTimeRef = useRef<number>(0);

  const toggleDropdown = useCallback(() => {
    toggleTimeRef.current = new Date().getTime();
    setShowDropdown(!showDropdown);
  }, [showDropdown]);

  // Position for portaled dropdown
  const dropdownPos = useMemo(() => {
    if (!showDropdown) return {};
    // The button is measured only when the dropdown opens, while the button
    // is already mounted; showDropdown is the intended trigger.
    // eslint-disable-next-line react-hooks/refs
    const { bottom, left } = buttonRef.current?.getBoundingClientRect() ?? {};
    return { top: bottom, left };
  }, [showDropdown]);

  // Close dropdown on scroll.
  useEffect(() => {
    if (!showDropdown) return;

    const handleScroll = (event: Event) => {
      if (!dropdownRef.current?.contains(event.target as Element)) {
        setShowDropdown(false);
      }
    };

    window.addEventListener("scroll", handleScroll, true);
    return () => window.removeEventListener("scroll", handleScroll, true);
  }, [showDropdown]);

  const countryIso2 = useMemo(() => {
    const { iso2 } = country;
    return onlyCountries.length && !onlyCountries.includes(iso2)
      ? defaultCountry
      : iso2;
  }, [country, defaultCountry, onlyCountries]);

  const hasValue = noPrefix ? !!_text : !!text && text === phone;
  const showButton = hasValue || !readonly;

  const [{ isPossible: isPossibleNumber, numberType }, setPhoneInfo] = useState<{
    isPossible: boolean;
    numberType: string | undefined;
  }>({ isPossible: false, numberType: undefined });

  useAsyncEffect(async () => {
    const phoneNumber = await getPhoneInfo(
      noPrefix ? _text : phone,
      noPrefix ? countryIso2 : undefined,
    );
    setPhoneInfo({
      isPossible: phoneNumber.isPossible(),
      numberType: phoneNumber.getDisplayType(),
    });
  }, [_text, countryIso2, noPrefix, phone]);

  const handleOpenPhoneLink = useCallback(() => {
    window.open(
      `tel:${noPrefix ? value : phone}`,
      "_blank",
      "noopener,noreferrer",
    );
  }, [noPrefix, value, phone]);

  const applyDefaultPaste = useCallback(
    (pastedText: string) => {
      const input = inputRef.current;
      const selectionStart = input?.selectionStart ?? inputValue.length;
      const selectionEnd = input?.selectionEnd ?? selectionStart;
      const value = `${inputValue.slice(0, selectionStart)}${pastedText}${inputValue.slice(selectionEnd)}`;

      applyPhoneInputValue(
        value,
        pastedText,
        selectionStart + pastedText.length,
      );
    },
    [applyPhoneInputValue, inputRef, inputValue],
  );

  const handlePaste = useCallback(
    async (event: ClipboardEvent<HTMLInputElement>) => {
      const pastedText = event.clipboardData.getData("text");

      if (noPrefix && !pastedText.trimStart().startsWith("+")) return;

      event.preventDefault();

      const normalized = await normalizePastedPhoneValue(
        pastedText,
        countryIso2,
      );

      if (!normalized) {
        applyDefaultPaste(pastedText);
        return;
      }

      const normalizedCountry = normalized.countryIso2;

      if (
        normalizedCountry &&
        onlyCountries.length &&
        !onlyCountries.includes(normalizedCountry)
      ) {
        applyDefaultPaste(pastedText);
        return;
      }

      if (noPrefix) {
        setValue(normalized.phone, true);
        return;
      }

      applyPhoneInputValue(normalized.phone);
    },
    [
      applyPhoneInputValue,
      applyDefaultPaste,
      countryIso2,
      noPrefix,
      setValue,
      onlyCountries,
    ],
  );

  const handleCountrySelect = useCallback(
    (country: ParsedCountry) => {
      if (noPrefix) {
        const digits = _text.replace(/\D/g, "");
        const national = digits.replace(/^0+/, "");
        setValue(`+${country.dialCode}${national}`, true);
      } else if (country.iso2 !== countryIso2) {
        selectedDialCodeRef.current = `+${country.dialCode}`;
        setValue(selectedDialCodeRef.current, true);
        setCountry(country.iso2);
      }
      setShowDropdown(false);
      inputRef.current?.focus();
    },
    [_text, countryIso2, inputRef, noPrefix, setCountry, setValue],
  );

  const handleDropdownClose = useCallback(() => {
    if (new Date().getTime() - toggleTimeRef.current > 200) {
      setTimeout(() => setShowDropdown(false), 100);
    }
  }, []);

  return (
    <FieldControl {...props} className={styles.container}>
      <Box className={clsx(styles.phone, { [styles.readonly]: readonly })}>
        {showButton && (
          <>
            <Box title={i18n.get(country.name)}>
              <Button
                ref={buttonRef}
                className={styles.country}
                onMouseDown={(event) => {
                  event.preventDefault();
                  if (event.button === 0) {
                    toggleDropdown();
                  }
                }}
                disabled={readonly}
              >
                {noPrefix ? (
                  <XxFlag className={styles.xxFlag} />
                ) : (
                  <FlagImage
                    iso2={countryIso2}
                    src={flags}
                    className={styles.flag}
                  />
                )}
                {!readonly && (
                  <Icon
                    icon={`arrow_drop_${showDropdown ? "up" : "down"}`}
                    className={styles.arrow}
                  />
                )}
              </Button>
            </Box>
            {!readonly && (
              <Portal>
                <Box
                  ref={dropdownRef}
                  className={styles.dropdown}
                  style={dropdownPos}
                >
                  <CountrySelectorDropdown
                    show={showDropdown}
                    selectedCountry={countryIso2}
                    onSelect={handleCountrySelect}
                    onClose={handleDropdownClose}
                    preferredCountries={preferredCountries}
                    countries={countries}
                    flags={FLAGS}
                    listItemFlagClassName={styles.flag}
                  />
                </Box>
              </Portal>
            )}
          </>
        )}
        {readonly ? (
          <TextLink
            href={`tel:${noPrefix ? value : phone}`}
            className={styles.link}
            title={numberType}
          >
            {hasValue && (noPrefix ? _text : inputValue)}
          </TextLink>
        ) : (
          <Box className={styles.inputWrapper}>
            <AdornedInput
              key={focus ? "focused" : "normal"}
              ref={inputRef}
              data-input
              type="tel"
              id={uid}
              autoFocus={focus}
              placeholder={placeholder}
              value={noPrefix ? _text : inputValue}
              invalid={invalid}
              required={required}
              onKeyDown={onKeyDown}
              onChange={noPrefix ? handleNationalChange : handlePhoneValueChange}
              onPaste={handlePaste}
              onBlur={noPrefix ? _onBlur : onBlur}
              title={numberType}
              className={clsx(styles.input, {
                [styles.warning]: !invalid && hasValue && !isPossibleNumber,
              })}
              endAdornment={
                <Button
                  onClick={handleOpenPhoneLink}
                  disabled={!isPossibleNumber}
                  title={i18n.get("Call")}
                >
                  <Icon icon="telephone" />
                </Button>
              }
              {...inputProps}
            />
          </Box>
        )}
      </Box>
    </FieldControl>
  );
}

function XxFlag({ className }: { className?: string }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      className={className}
      aria-hidden="true"
      focusable="false"
    >
      <defs>
        <clipPath id="xx-flag-clip">
          <rect x=".5" y="4" width="23" height="16" rx="1.75" />
        </clipPath>
      </defs>
      <rect
        x=".5"
        y="4"
        width="23"
        height="16"
        rx="1.75"
        fill="var(--bs-secondary-bg)"
        stroke="var(--bs-secondary-color)"
        strokeWidth="1"
      />
      <line
        x1=".5"
        y1="4"
        x2="23.5"
        y2="20"
        stroke="var(--bs-secondary-color)"
        strokeWidth="1"
        clipPath="url(#xx-flag-clip)"
      />
      <line
        x1="23.5"
        y1="4"
        x2=".5"
        y2="20"
        stroke="var(--bs-secondary-color)"
        strokeWidth="1"
        clipPath="url(#xx-flag-clip)"
      />
    </svg>
  );
}
