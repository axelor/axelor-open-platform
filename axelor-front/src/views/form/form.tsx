import { FormView } from "@/services/client/meta.types";
import { ViewProps } from "../types";
import { Form as FormComponent } from "./builder";

import styles from "./form.module.scss";

export function Form({ meta }: ViewProps<FormView>) {
  return (
    <div className={styles.formViewContainer}>
      <FormComponent className={styles.formView} meta={meta} />
    </div>
  );
}
