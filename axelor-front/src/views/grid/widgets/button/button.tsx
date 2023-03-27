import { Field } from "@/services/client/meta.types";
import { legacyClassNames } from "@/styles/legacy";
import { Box } from "@axelor/ui";
import { GridColumnProps } from "@axelor/ui/src/grid/grid-column";

export function Button(props: GridColumnProps) {
  const { data: field } = props;
  const { name, icon, css, help } = field as Field;

  if (!icon) return null;

  function handleClick(e: React.MouseEvent<HTMLElement>) {
    e.preventDefault();
  }

  function renderIcon() {
    return (
      <a href=" " onClick={handleClick} title={help}>
        <Box as="i" me={2} className={legacyClassNames("fa", css, icon)} />
      </a>
    );
  }

  function renderImage() {
    return (
      <Box>
        <img
          title={help}
          height={24}
          className={legacyClassNames(css)}
          src={icon}
          alt={name}
          onClick={handleClick}
        />
      </Box>
    );
  }

  return icon.includes("fa-") ? renderIcon() : renderImage();
}
