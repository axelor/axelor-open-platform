import { legacyClassNames } from "@/styles/legacy";
import { Box } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";
import { Chip } from "./chip";
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
          <MaterialIcon
            key={icon.name}
            icon={icon.icon}
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
