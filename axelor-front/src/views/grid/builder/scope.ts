import { DataContext } from "@/services/client/data.types";
import { DefaultActionHandler } from "@/view-containers/action";

export class GridActionHandler extends DefaultActionHandler {
  #prepareContext: () => DataContext;

  constructor(prepareContext: () => DataContext) {
    super();
    this.#prepareContext = prepareContext;
  }

  getContext() {
    return this.#prepareContext();
  }
}
