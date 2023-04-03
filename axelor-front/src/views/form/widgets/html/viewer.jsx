import { Box } from "@axelor/ui";
import classes from "./viewer.module.css";

function HTMLViewer({ value }) {
  return (
    <Box
      p={1}
      className={classes.content}
      dangerouslySetInnerHTML={{ __html: value }}
    />
  );
}

export default HTMLViewer;
