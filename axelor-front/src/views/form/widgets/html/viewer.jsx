import { Box, clsx } from "@axelor/ui";
import classes from "./viewer.module.css";

import { sanitize } from "@/utils/sanitize";

function HTMLViewer({ value, className = "" }) {
  return (
    <Box
      p={1}
      className={clsx(classes.content, className)}
      dangerouslySetInnerHTML={{ __html: sanitize(value) }}
    />
  );
}

export default HTMLViewer;
