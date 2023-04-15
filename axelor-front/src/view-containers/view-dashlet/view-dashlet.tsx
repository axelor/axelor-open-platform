import { ReactElement } from "react";
import { ScopeProvider } from "jotai-molecules";
import { DashletScope } from "./handler";

export function DashletView({ children }: { children?: ReactElement }) {
  return (
    <ScopeProvider scope={DashletScope} value={{}}>
      {children}
    </ScopeProvider>
  );
}
