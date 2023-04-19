import { DataContext } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { Field } from "@/services/client/meta.types";
import { WidgetErrors } from "@/views/form/builder";

export type ValiationOptions = {
  props: Field;
  context: DataContext;
};

export type Validate = (
  value: any,
  options: ValiationOptions
) => WidgetErrors | undefined;

const isEmpty = (value: any) =>
  value === undefined || value === null || value === "";

const validateRequired: Validate = (value, { props }) => {
  const { title, required } = props;
  if (required && isEmpty(value)) {
    return { required: i18n.get("{0} is required", title) };
  }
};

export const validate: Validate = (value, options) => {
  let errors = validateRequired(value, options);
  if (errors) {
    return errors;
  }
};
