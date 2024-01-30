import { GridColumnProps } from "@axelor/ui/grid";
import { FlagImage, usePhoneInput } from "react-international-phone";

import { Box } from "@axelor/ui";
import "react-international-phone/style.css";
import styles from "./phone.module.scss";

export function Phone(props: GridColumnProps) {
  const { rawValue: value } = props;

  const { inputValue, phone, country } = usePhoneInput({
    value: value ?? "",
  });

  return (
    value && (
      <>
        <FlagImage iso2={country.iso2} className={styles.country} />
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
