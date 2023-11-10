import { useCallback, useEffect, useRef, useState } from "react";

import { Dialog, DialogContent, Fade, Portal } from "@axelor/ui";

import { i18n } from "@/services/client/i18n";
import { SessionInfo, session } from "@/services/client/session";

import { Loader } from "../loader/loader";
import { LoginForm } from "../login-form";
import { useHttpBlock } from "./use-block";
import { useHttpWatch } from "./use-watch";

import styles from "./http-watch.module.scss";

export function HttpWatch() {
  const { count, resume } = useHttpWatch();
  return (
    <>
      <HttpIndicator count={count} />
      <HttpBlock count={count} />
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
        <div className={styles.indicator}>{i18n.get("Loading...")}</div>
      </Fade>
    </Portal>
  );
}

function HttpBlock({ count }: { count: number }) {
  const [wait, setWait] = useState(false);
  const [blocked] = useHttpBlock();

  const active = blocked || count > 0;

  useEffect(() => {
    const delay = active ? 5000 : 300;
    const timer = setTimeout(() => {
      setWait(active);
    }, delay);
    return () => {
      clearTimeout(timer);
    };
  }, [active, count]);

  return (
    <Portal>
      <Fade in={blocked || wait} mountOnEnter unmountOnExit>
        <div className={styles.block}>
          <Fade in={wait} mountOnEnter unmountOnExit>
            <div className={styles.wait}>
              <Loader delay={10} text={i18n.get("Please wait...")} />
            </div>
          </Fade>
        </div>
      </Fade>
    </Portal>
  );
}

function HttpAuth({ resume }: { resume?: () => void }) {
  const prev = session.info?.user?.login;
  const handleSuccess = useCallback(
    (info: SessionInfo) => {
      const curr = info.user?.login;
      if (prev !== curr) {
        window.location.reload();
      } else {
        resume?.();
      }
    },
    [prev, resume],
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
