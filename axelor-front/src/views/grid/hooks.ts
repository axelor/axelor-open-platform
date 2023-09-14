import { useAsync } from "@/hooks/use-async";
import { toCamelCase, toKebabCase } from "@/utils/names";

export function useWidgetComp(type: string) {
  return useAsync(async () => {
    const module = toKebabCase(type);
    const name = toCamelCase(type);
    const { [name]: Comp } = await import(`./widgets/${module}/index.ts`);
    return Comp as React.ElementType;
  }, [type]);
}
