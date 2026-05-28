import { useEffect, useState } from "react";

import { Box, useRefs } from "@axelor/ui";
import { withStyled } from "@axelor/ui/core/styled";

export const AccessibleButton = withStyled(Box)((props, ref) => {
  const [element, setElement] = useState<HTMLElement | null>(null);
  const mergedRef = useRefs(ref, setElement);

  useEffect(() => {
    if (!element) return;

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Enter" || event.key === " ") {
        event.preventDefault();
        element.click();
      }
    };

    element.addEventListener("keydown", handleKeyDown);
    return () => {
      element.removeEventListener("keydown", handleKeyDown);
    };
  }, [element]);

  return <Box ref={mergedRef} rounded role="button" {...props} />;
});
