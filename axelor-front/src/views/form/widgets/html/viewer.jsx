import { Box, clsx } from "@axelor/ui";
import classes from "./viewer.module.css";

function HTMLViewer({ value, className = "" }) {
  return (
    <Box
      p={1}
      className={clsx(classes.content, className)}
      dangerouslySetInnerHTML={{ __html: value }}
    />
  );
}

export default HTMLViewer;
