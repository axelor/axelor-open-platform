import clsx from "clsx";
import { useEffect, useState } from "react";

import { Box, CircularProgress } from "@axelor/ui";

import styles from "./loader.module.scss";
import { i18n } from "@/services/client/i18n.ts";

export type LoaderProps = {
  text?: string;
  delay?: number;
};

export function Loader({
  text = i18n.get("Loading..."),
  delay = 400,
}: LoaderProps) {
  const [show, setShow] = useState<boolean>(false);

  useEffect(() => {
    let canceled = false;
    let timer = setTimeout(() => {
      if (!canceled) {
        setShow(true);
      }
    }, delay);
    return () => {
      canceled = true;
      clearTimeout(timer);
    };
  }, [delay]);

  return (
    <Box className={clsx(styles.loader, { [styles.show]: show })}>
      <Box>
        <CircularProgress size={25} indeterminate />
      </Box>
      <Box>{text}</Box>
    </Box>
  );
}
