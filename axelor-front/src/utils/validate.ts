import { DataContext } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { Field } from "@/services/client/meta.types";
import { WidgetErrors } from "@/views/form/builder";

import { toKebabCase } from "./names";

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

const validatePattern: Validate = (value, { props }) => {
  const { title, pattern } = props;
  if (pattern && !new RegExp(pattern).test(value)) {
    return { invalid: i18n.get("{0} is invalid", title) };
  }
};

const validateRange: Validate = (value, { props }) => {
  const { title, minSize = 0, maxSize = 0 } = props;
  const min = +minSize;
  const max = +maxSize;
  if (min && value < min) {
    return { invalid: i18n.get("{0} is too small", title) };
  }
  if (max && value > max) {
    return { invalid: i18n.get("{0} is too big", title) };
  }
};

const validateString: Validate = (value, { props, context }) => {
  return (
    validateRequired(value, { props, context }) ||
    validatePattern(value, { props, context }) ||
    validateRange(value?.length, { props, context })
  );
};

const validateNumber: Validate = (value, { props, context }) => {
  return (
    validateRequired(value, { props, context }) ||
    validateRange(value, { props, context })
  );
};

export const Validators = {
  string: validateString,
  integer: validateNumber,
  long: validateNumber,
  decimal: validateNumber,
};

export const validate: Validate = (value, { props, context }) => {
  let type = props?.serverType ?? props?.type;
  if (type) type = toKebabCase(type);

  if (type === "enum") type = "enumeration";
  if (props?.selection) type = "selection";

  let func = Validators[type as keyof typeof Validators];
  if (func) {
    return func(value, { props, context });
  }

  let errors = validateRequired(value, { props, context });
  if (errors) {
    return errors;
  }
};
