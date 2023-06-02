import { useCallback, useEffect, useRef, useState } from "react";

import { Dialog, DialogContent, Fade, Portal } from "@axelor/ui";

import { i18n } from "@/services/client/i18n";
import { SessionInfo, session } from "@/services/client/session";

import { Loader } from "../loader/loader";
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
  const [block, setBlock] = useState(false);

  const timerRef = useRef<number>();
  const blockRef = useRef<number>();
  const mountRef = useRef(false);

  const handleShow = useCallback(() => {
    if (timerRef.current && count > 0) return;
    if (timerRef.current) window.clearTimeout(timerRef.current);
    timerRef.current = window.setTimeout(() => {
      if (mountRef.current) setShow(count > 0);
      timerRef.current = undefined;
    }, 300);
  }, [count]);

  const handleBlock = useCallback(() => {
    if (blockRef.current && count > 0) return;
    if (blockRef.current) window.clearTimeout(blockRef.current);
    const wait = count > 0 ? 5000 : 300;
    blockRef.current = window.setTimeout(() => {
      if (mountRef.current) setBlock(count > 0);
      blockRef.current = undefined;
    }, wait);
  }, [count]);

  useEffect(() => {
    mountRef.current = true;
    return () => {
      mountRef.current = false;
    };
  }, []);

  useEffect(() => () => window.clearTimeout(timerRef.current), []);
  useEffect(() => () => window.clearTimeout(blockRef.current), []);

  useEffect(handleShow, [handleShow]);
  useEffect(handleBlock, [handleBlock]);

  return (
    <Portal>
      <Fade in={show} mountOnEnter unmountOnExit>
        <div className={styles.indicator}>{i18n.get("Loading...")}</div>
      </Fade>
      <Fade in={block} mountOnEnter unmountOnExit>
        <div className={styles.block}>
          <Loader text={i18n.get("Please Wait...")} />
        </div>
      </Fade>
    </Portal>
  );
}

function HttpAuth({ resume }: { resume?: () => void }) {
  const prev = session.info?.user.login;
  const handleSuccess = useCallback(
    (info: SessionInfo) => {
      const curr = info.user.login;
      if (prev !== curr) {
        window.location.reload();
      } else {
        resume?.();
      }
    },
    [prev, resume]
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
