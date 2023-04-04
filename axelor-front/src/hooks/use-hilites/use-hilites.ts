import { DataContext } from "@/services/client/data.types";
import { Hilite } from "@/services/client/meta.types";
import { parseExpression as parse } from "../use-parser/utils";

export function useHilites(hilites: Hilite[], context: DataContext) {
  return hilites.filter((x) => parse(x.condition ?? "")(context));
}
