import { useCallback } from "react";

import { Box, Button, Link } from "@axelor/ui";

import { dialogs } from "@/components/dialogs";
import { i18n } from "@/services/client/i18n";
import { session } from "@/services/client/session";

import { System } from "../system";

import AppLogo from "../../assets/axelor.svg?react";

export function About() {
  const { info } = session;
  const { application: app, user } = info || { application: {}, user: {} };

  const year = new Date().getFullYear();
  const technical = user?.technical;

  const showSystemInfo = useCallback(() => {
    dialogs.info({
      title: i18n.get("System Information"),
      content: <System />,
    });
  }, []);

  return (
    <Box flex={1} textAlign="center">
      <Box>
        <Box as={AppLogo} style={{ width: 175, height: 100 }} />
        <h5>
          {app.name} - {app.description}
        </h5>
      </Box>
      <div>
        <p>
          {i18n.get("Version")}: {app.version}
        </p>
        <p>{app.copyright}</p>
        <Box d="flex" g={1} flexDirection="column">
          {app.home && <Link href={app.home}>{app.home}</Link>}
          {app.help && <Link href={app.help}>{i18n.get("Documentation")}</Link>}
          <Link href="http://www.gnu.org/licenses/agpl.html">
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

        <p>Copyright (c) 2005-{year} Axelor. All Rights Reserved.</p>
        <Box d="flex" g={1} flexDirection="column">
          <Link href="http://www.axelor.com">http://www.axelor.com</Link>
          <Link
            href={`http://docs.axelor.com/adk/${app.aopVersion?.substring(
              0,
              app.aopVersion?.lastIndexOf("."),
            )}`}
          >
            {i18n.get("Documentation")}
          </Link>
          <Link href="http://www.gnu.org/licenses/agpl.html">
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
