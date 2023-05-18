import { useCallback, useEffect, useMemo, useState } from "react";
import { useSetAtom } from "jotai";
import { Box } from "@axelor/ui";

import { CustomView } from "@/services/client/meta.types";
import { DataRecord } from "@/services/client/data.types";
import { custom } from "@/services/client/meta";
import { useViewContext, useViewTab } from "@/view-containers/views/scope";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { useTemplate } from "@/hooks/use-parser";
import { ViewProps } from "../types";
import { legacyClassNames } from "@/styles/legacy";
import { useDashletHandlerAtom } from "@/view-containers/view-dashlet/handler";
import { ReportBox } from "./widgets/report-box";
import { ReportTable } from "./widgets/report-table";
import styles from "./custom.module.scss";

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
        "report-box": ReportBox,
        "report-table": (props: any) => <ReportTable {...props} view={view} />,
      },
    }),
    [view]
  );

  return (
    <Box d="flex" flexGrow={1} borderTop className={styles.container}>
      <Box d="flex" className={legacyClassNames(view.css)} flexGrow={1}>
        <Template context={dataContext} options={options} />
      </Box>
    </Box>
  );
}
