import { createContext, ReactNode, useContext, useMemo } from "react";

interface PropertiesContextProviderProps {
  invalids: Record<string, boolean>;
  setInvalids: React.Dispatch<React.SetStateAction<Record<string, boolean>>>;
  readonly?: boolean;
  getCssVar?: (name: string) => string | undefined;
}

const PropertiesContext = createContext<PropertiesContextProviderProps>({
  invalids: {},
  setInvalids: () => {},
});

export function PropertiesContextProvider({
  children,
  invalids,
  readonly,
  getCssVar,
  setInvalids,
}: PropertiesContextProviderProps & {
  children: ReactNode;
}) {
  const value = useMemo(
    () => ({
      getCssVar,
      readonly,
      invalids,
      setInvalids,
    }),
    [getCssVar, readonly, invalids, setInvalids],
  );

  return (
    <PropertiesContext.Provider value={value}>
      {children}
    </PropertiesContext.Provider>
  );
}

export function usePropertiesContext() {
  return useContext(PropertiesContext);
}
