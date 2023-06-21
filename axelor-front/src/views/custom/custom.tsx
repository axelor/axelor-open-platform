import { Box } from "@axelor/ui";
import { useSetAtom } from "jotai";
import { useCallback, useEffect, useMemo, useState } from "react";

import { useAsyncEffect } from "@/hooks/use-async-effect";
import { useTemplate } from "@/hooks/use-parser";
import { DataRecord } from "@/services/client/data.types";
import { custom } from "@/services/client/meta";
import { CustomView } from "@/services/client/meta.types";
import { legacyClassNames } from "@/styles/legacy";
import { useDashletHandlerAtom } from "@/view-containers/view-dashlet/handler";
import { useViewContext, useViewTab } from "@/view-containers/views/scope";
import { ViewProps } from "../types";
import styles from "./custom.module.scss";
import { ReportBox } from "./widgets/report-box";
import { ReportTable } from "./widgets/report-table";

export function Custom({ meta }: ViewProps<CustomView>) {
  const { dashlet } = useViewTab();
  const { view } = meta;
  const [dataContext, setDataContext] = useState<{
    data?: DataRecord[];
    first?: DataRecord;
  }>({});
  const setDashletHandlers = useSetAtom(useDashletHandlerAtom());
  const getContext = useViewContext();

  const onRefresh = useCallback(async () => {
    if (view.name) {
      const result = await custom(view.name, getContext());
      setDataContext(result);
    }
  }, [getContext, view]);

  useAsyncEffect(async () => {
    await onRefresh();
  }, [onRefresh]);

  useEffect(() => {
    if (dashlet) {
      setDashletHandlers({
        view,
        onRefresh,
      });
    }
  }, [dashlet, view, onRefresh, setDashletHandlers]);

  const Template = useTemplate(view.template!);
  const options = useMemo(
    () => ({
      components: {
        "report-box": (props: any) => <ReportBox {...props} />,
        "report-table": (props: any) => <ReportTable {...props} view={view} />,
      },
    }),
    [view]
  );

  return (
    <Box d="flex" flexGrow={1} className={styles.container}>
      <Box d="flex" className={legacyClassNames(view.css)} flexGrow={1}>
        <Template context={dataContext} options={options} />
      </Box>
    </Box>
  );
}
