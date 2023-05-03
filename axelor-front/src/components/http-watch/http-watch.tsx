import { useCallback, useEffect, useRef, useState } from "react";

import { Dialog, DialogContent, Fade, Portal } from "@axelor/ui";

import { SessionInfo, session } from "@/services/client/session";

import { LoginForm } from "../login-form";
import { useHttpWatch } from "./use-watch";

import styles from "./http-watch.module.scss";

export function HttpWatch() {
  const { count, resume } = useHttpWatch();
  return (
    <>
      <HttpIndicator count={count} />
      <HttpAuth resume={resume} />
    </>
  );
}

function HttpIndicator({ count }: { count: number }) {
  const [show, setShow] = useState(false);

  const timerRef = useRef<number>();
  const mountRef = useRef(false);

  const handleShow = useCallback(() => {
    if (timerRef.current && count > 0) return;
    if (timerRef.current) window.clearTimeout(timerRef.current);
    timerRef.current = window.setTimeout(() => {
      if (mountRef.current) setShow(count > 0);
      timerRef.current = undefined;
    }, 300);
  }, [count]);

  useEffect(() => {
    mountRef.current = true;
    return () => {
      mountRef.current = false;
    };
  }, []);

  useEffect(() => () => window.clearTimeout(timerRef.current), []);
  useEffect(handleShow, [handleShow]);

  return (
    <Portal>
      <Fade in={show} mountOnEnter unmountOnExit>
        <div className={styles.indicator}>Loading...</div>
      </Fade>
    </Portal>
  );
}

function HttpAuth({ resume }: { resume?: () => void }) {
  const handleSuccess = useCallback(
    (info: SessionInfo) => {
      const prev = session.info?.user.login;
      const curr = info.user.login;
      if (prev !== curr) {
        window.location.reload();
      } else {
        resume?.();
      }
    },
    [resume]
  );
  return (
    <Portal>
      <Dialog open={Boolean(resume)} backdrop>
        <DialogContent>
          <LoginForm onSuccess={handleSuccess} />
        </DialogContent>
      </Dialog>
    </Portal>
  );
}
