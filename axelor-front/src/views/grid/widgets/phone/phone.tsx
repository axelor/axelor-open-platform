import { useMemo } from "react";
import { FlagImage, usePhoneInput } from "react-international-phone";

import { Box } from "@axelor/ui";
import { GridColumnProps } from "@axelor/ui/grid";

import { i18n } from "@/services/client/i18n";
import { _findLocale, l10n } from "@/services/client/l10n";
import { Schema } from "@/services/client/meta.types";
import {
  FALLBACK_COUNTRIES,
  FLAG_SOURCES,
} from "@/views/form/widgets/phone/utils";

import "react-international-phone/style.css";
import styles from "./phone.module.scss";

export function Phone(props: GridColumnProps) {
  const { rawValue: value, data } = props;
  const { initialCountry } = data as Schema;

  const locale = l10n.getLocale();

  const defaultCountry = useMemo(() => {
    let defaultCountry = initialCountry;

    if (!defaultCountry) {
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

    return defaultCountry;
  }, [initialCountry, locale]);

  const noPrefix = !!value && !value.startsWith("+");

  const text = useMemo(() => {
    return (noPrefix ? value.replace(/^0/, "") : value) ?? "";
  }, [value, noPrefix]);

  const { inputValue, phone, country } = usePhoneInput({
    value: text,
    defaultCountry,
    disableDialCodeAndPrefix: noPrefix,
  });

  const { iso2 } = country;

  return (
    value && (
      <>
        <Box title={i18n.get(country.name)} className={styles.flagContainer}>
          <FlagImage
            className={styles.flag}
            iso2={iso2}
            src={FLAG_SOURCES[iso2]}
          />
        </Box>
        <Box
          as="a"
          target="_blank"
          href={`tel:${noPrefix ? value : phone}`}
          className={styles.link}
          onClick={(e) => e.stopPropagation()} // Prevent going into edit when clicking on link.
        >
          {inputValue}
        </Box>
      </>
    )
  );
}
