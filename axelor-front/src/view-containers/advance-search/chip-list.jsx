import React from "react";
import { Box, Icon } from "@axelor/ui";
import { Chip } from "./chip";
import { legacyClassNames } from "@/styles/legacy";
import styles from "./chip-list.module.scss";

function ChipList({ className, value, icons, onClear }) {
  function handleClear() {
    onClear && onClear();
  }

  return (
    <Box border d="flex">
      <Box
        className={legacyClassNames(className, styles["chip-list"])}
        d="flex"
        flex={1}
        p={1}
      >
        <Chip
          className={styles.chip}
          color={"indigo"}
          label={value}
          onDelete={handleClear}
        />
      </Box>
      <Box
        d="flex"
        justifyContent="flex-end"
        alignItems="center"
        position="relative"
        className={styles.actions}
      >
        {icons.map((icon) => (
          <Icon
            key={icon.name}
            ms={1}
            me={1}
            color="secondary"
            as={icon.as}
            onClick={icon.onClick}
            className={styles.pointer}
          />
        ))}
      </Box>
    </Box>
  );
}

ChipList.defaultProps = {
  t: (e) => e,
  filters: [],
  domain: [],
};

export default ChipList;
