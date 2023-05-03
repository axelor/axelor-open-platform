import {
  Box,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
} from "@axelor/ui";

import { Loader } from "@/components/loader/loader";
import { useAsync } from "@/hooks/use-async";
import { request } from "@/services/client/client";
import { i18n } from "@/services/client/i18n";
import { Formatters } from "@/utils/format";

import styles from "./system.module.scss";

const formatDate = (value: number) => {
  return Formatters.datetime(new Date(value));
};

const formatNumber = (value: string) => {
  const num = value.replace(" Kb", "");
  return Formatters.decimal(num) + " Kb";
};

export function System() {
  const load = async () => {
    const res = await request({
      url: "ws/app/sysinfo",
    });
    if (res.ok) {
      return await res.json();
    }
    return Promise.reject();
  };

  const { state, data: sys } = useAsync(load, []);

  if (state === "loading" || state === "hasError") {
    return <Loader />;
  }

  return (
    <Box flex={1}>
      <Box>
        <Box>
          <h4>{i18n.get("Environment")}</h4>
        </Box>
        <Box p={2}>
          <dl className={styles.dlist}>
            <dt>{i18n.get("Operating System")}</dt>
            <dd>
              {sys.osName} {sys.osVersion} [{sys.osArch}]
            </dd>
            <dt>{i18n.get("Java Runtime")}</dt>
            <dd>{sys.javaRuntime}</dd>
            <dt>{i18n.get("Java Version")}</dt>
            <dd>{sys.javaVersion}</dd>
          </dl>
        </Box>
      </Box>
      <Box>
        <Box>
          <h4>{i18n.get("Memory")}</h4>
        </Box>
        <Box p={2}>
          <dl className={styles.dlist}>
            <dt>{i18n.get("Total Memory")}</dt>
            <dd>{formatNumber(sys.memTotal)}</dd>
            <dt>{i18n.get("Max Memory")}</dt>
            <dd>{formatNumber(sys.memMax)}</dd>
            <dt>{i18n.get("Used Memory")}</dt>
            <dd>{formatNumber(sys.memUsed)}</dd>
            <dt>{i18n.get("Free Memory")}</dt>
            <dd>{formatNumber(sys.memFree)}</dd>
          </dl>
        </Box>
      </Box>
      <Box>
        <Box>
          <h4>{i18n.get("Active Users")}</h4>
        </Box>
        <Box p={2}>
          <Table flex={1}>
            <TableHead>
              <TableRow>
                <TableCell as="th">#</TableCell>
                <TableCell as="th">{i18n.get("User")}</TableCell>
                <TableCell as="th">{i18n.get("Log in time")}</TableCell>
                <TableCell as="th">{i18n.get("Last access time")}</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {sys.users.map((login: any, index: number) => (
                <TableRow key={index}>
                  <TableCell>{index + 1}</TableCell>
                  <TableCell>{login.user}</TableCell>
                  <TableCell>{formatDate(login.loginTime)}</TableCell>
                  <TableCell>{formatDate(login.accessTime)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Box>
      </Box>
    </Box>
  );
}
