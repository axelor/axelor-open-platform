import { FlagImage, usePhoneInput } from "react-international-phone";

import { Box } from "@axelor/ui";
import { GridColumnProps } from "@axelor/ui/grid";

import { FLAG_SOURCES } from "@/views/form/widgets/phone/utils";

import "react-international-phone/style.css";
import styles from "./phone.module.scss";

export function Phone(props: GridColumnProps) {
  const { rawValue: value } = props;

  const { inputValue, phone, country } = usePhoneInput({
    value: value ?? "",
  });

  const { iso2 } = country;

  return (
    value && (
      <>
        <FlagImage
          className={styles.country}
          iso2={iso2}
          src={FLAG_SOURCES[iso2]}
        />
        <Box
          as="a"
          target="_blank"
          href={`tel:${phone}`}
          className={styles.link}
          onClick={(e) => e.stopPropagation()} // Prevent going into edit when clicking on link.
        >
          {inputValue}
        </Box>
      </>
    )
  );
}
