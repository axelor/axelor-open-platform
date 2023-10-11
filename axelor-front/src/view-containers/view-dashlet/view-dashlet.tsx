import { ScopeProvider } from "jotai-molecules";
import { ReactElement, useMemo } from "react";

import { DashletScope } from "./handler";

export function DashletView({ children }: { children?: ReactElement }) {
  const scope = useMemo(() => ({}), []);
  return (
    <ScopeProvider scope={DashletScope} value={scope}>
      {children}
    </ScopeProvider>
  );
}
