import { useMemo } from "react";
import { FlagImage, usePhoneInput } from "react-international-phone";

import { Box } from "@axelor/ui";
import { GridColumnProps } from "@axelor/ui/grid";

import { TextLink } from "@/components/text-link";
import { i18n } from "@/services/client/i18n";
import { Schema } from "@/services/client/meta.types";
import {
  getPhoneInfo,
  useDefaultCountry,
} from "@/views/form/widgets/phone/utils";

import "react-international-phone/style.css";

import flags from "@/assets/flags.svg";
import styles from "./phone.module.scss";

export function Phone(props: GridColumnProps) {
  const { rawValue: value, data } = props;
  const { initialCountry } = data as Schema;

  const defaultCountry = useDefaultCountry(initialCountry);

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
  const show = value && inputValue;

  const numberType = useMemo(
    () => (show ? getPhoneInfo(phone).getDisplayType() : undefined),
    [phone, show],
  );

  return (
    show && (
      <>
        <Box title={i18n.get(country.name)} className={styles.flagContainer}>
          <FlagImage className={styles.flag} iso2={iso2} src={flags} />
        </Box>
        <TextLink
          href={`tel:${noPrefix ? value : phone}`}
          className={styles.link}
          title={numberType}
          onClick={(e) => e.stopPropagation()} // Prevent going into edit when clicking on link.
        >
          {inputValue}
        </TextLink>
      </>
    )
  );
}
