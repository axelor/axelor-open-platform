import { useCallback } from "react";

import { Box, Button, Link } from "@axelor/ui";

import { dialogs } from "@/components/dialogs";
import { i18n } from "@/services/client/i18n";
import { session } from "@/services/client/session";

import { System } from "../system";

import { ReactComponent as AppLogo } from "../../assets/axelor.svg";

export function About() {
  const { info } = session;
  const { app, user } = info || { app: {}, user: {} };

  const year = new Date().getFullYear();
  const technical = user.technical;

  const showSystemInfo = useCallback(() => {
    dialogs.info({
      size: "lg",
      title: i18n.get("System Information"),
      content: <System />,
    });
  }, []);

  return (
    <Box flex={1} textAlign="center">
      <Box>
        <Box as={AppLogo} style={{ width: 175, height: 100 }} />
        <h4>
          {app.name} - {app.description}
        </h4>
      </Box>
      <div>
        <p>
          <strong>
            {i18n.get("Version")}: {app.version}
          </strong>
        </p>
        <p>{app.copyright}</p>
        {app.home && (
          <p>
            {i18n.get("Website")}: <Link href={app.home}>{app.home}</Link>.
          </p>
        )}
        <p>
          <strong>{i18n.get("Links")}</strong>
        </p>
        <ul style={{ listStyle: "none" }}>
          {app.help && (
            <li>
              <Link href={app.help}>{i18n.get("Documentation")}</Link>
            </li>
          )}
          <li>
            <Link href="http://www.gnu.org/licenses/agpl.html">
              {i18n.get("License")}
            </Link>
          </li>
        </ul>
      </div>
      <Box mt={4}>
        <h4>Axelor SDK</h4>
      </Box>
      <div>
        <p>
          <strong>
            {i18n.get("Version")}: {app.sdk}
          </strong>
        </p>
        <p>Copyright (c) 2005-{year} Axelor. All Rights Reserved.</p>
        <p>
          {i18n.get("Website")}:{" "}
          <Link href="http://www.axelor.com">http://www.axelor.com</Link>.
        </p>
        <p>
          <strong>{i18n.get("Links")}</strong>
        </p>
        <ul style={{ listStyle: "none" }}>
          <li>
            <Link href={`http://docs.axelor.com/adk/${app.sdk}`}>
              {i18n.get("Documentation")}
            </Link>
          </li>
          <li>
            <Link href="http://www.gnu.org/licenses/agpl.html">
              {i18n.get("License")}
            </Link>
          </li>
        </ul>
      </div>
      {technical && (
        <Button variant="link" onClick={showSystemInfo}>
          {i18n.get("System Information")}
        </Button>
      )}
    </Box>
  );
}
