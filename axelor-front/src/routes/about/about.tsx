import { useAtomValue } from "jotai";
import { useCallback } from "react";

import { Box, Button, Link } from "@axelor/ui";

import { DefaultAppLogo } from "@/components/app-logo";
import { dialogs } from "@/components/dialogs";
import { COPYRIGHT, useAppSettings } from "@/hooks/use-app-settings";
import { useRoute } from "@/hooks/use-route";
import { i18n } from "@/services/client/i18n";
import { session } from "@/services/client/session";
import { usePopupHandlerAtom } from "@/view-containers/view-popup/handler";
import { System } from "../system";

import styles from "./about.module.scss";

export function About() {
  const { info } = session;
  const { application: app, user } = info || { application: {}, user: {} };

  const technical = user?.technical;

  const { navigate } = useRoute();
  const { name, description, copyright } = useAppSettings();

  const popupHandlerAtom = usePopupHandlerAtom();
  const { close: closePopup } = useAtomValue(popupHandlerAtom);

  const showSwagger = useCallback(() => {
    closePopup?.();
    navigate({ pathname: "/api-documentation" });
  }, [navigate, closePopup]);

  const showSystemInfo = useCallback(() => {
    dialogs.info({
      title: i18n.get("System Information"),
      content: <System />,
    });
  }, []);

  return (
    <Box flex={1} textAlign="center">
      <Box>
        <DefaultAppLogo className={styles.logo} />
        <h5>
          {name} - {description}
        </h5>
      </Box>
      <div>
        <p>
          {i18n.get("Version")}: {app.version}
        </p>
        <p>{copyright}</p>
        <Box d="flex" g={1} flexDirection="column">
          {app.home && <Link href={app.home}>{i18n.get("Home page")}</Link>}
          {app.help && <Link href={app.help}>{i18n.get("Documentation")}</Link>}
          <Link href="https://www.gnu.org/licenses/agpl.html">
            {i18n.get("License")}
          </Link>
        </Box>
      </div>
      <Box mt={4}>
        <h5>Axelor SDK</h5>
      </Box>
      <div>
        <p>
          {i18n.get("Version")}: {app.aopVersion}
          <br />
          {technical && (
            <span style={{ fontSize: "small" }}>
              (build {app.aopBuildDate} - rev {app.aopGitHash?.slice(0, 12)})
            </span>
          )}
        </p>

        <p>{COPYRIGHT}</p>
        <Box d="flex" g={1} flexDirection="column">
          <Link href="https://axelor.com">https://axelor.com</Link>
          <Link
            href={`https://docs.axelor.com/adk/${app.aopVersion?.substring(
              0,
              app.aopVersion?.lastIndexOf("."),
            )}`}
          >
            {i18n.get("Documentation")}
          </Link>
          {technical && app.swaggerUI?.enabled && (
            <Button variant="link" onClick={showSwagger}>
              {i18n.get("API Documentation")}
            </Button>
          )}
          <Link href="https://www.gnu.org/licenses/agpl.html">
            {i18n.get("License")}
          </Link>
        </Box>
      </div>
      {technical && (
        <Box mt={3}>
          <Button variant="link" onClick={showSystemInfo}>
            {i18n.get("System Information")}
          </Button>
        </Box>
      )}
    </Box>
  );
}
