import { DataContext } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { Field } from "@/services/client/meta.types";
import { isUUID } from "@/utils/types";
import { WidgetErrors } from "@/views/form/builder";

import { toKebabCase } from "./names";

export type ValiationOptions = {
  props: Field;
  context: DataContext;
};

export type Validate = (
  value: any,
  options: ValiationOptions,
) => WidgetErrors | undefined;

const isEmpty = (value: any) =>
  value === undefined ||
  value === null ||
  value === "" ||
  (Array.isArray(value) && value.length === 0);

const validateRequired: Validate = (value, { props }) => {
  const { title, required } = props;
  if (required && isEmpty(value)) {
    return { required: i18n.get("{0} is required", title) };
  }
};

const validatePattern: Validate = (value, { props }) => {
  const { title, pattern, widget } = props;
  if (
    typeof value === "string" &&
    value &&
    ((["duration", "time"].includes(widget ?? "") && value?.includes?.("_")) ||
      (pattern && !new RegExp(pattern).test(value)))
  ) {
    return { pattern: i18n.get("{0} is not in proper format", title) };
  }
};

const validateRange: Validate = (value, { props }) => {
  const { title, minSize, maxSize } = props;
  if (minSize != null && value < +(minSize ?? 0)) {
    return { min: i18n.get("{0} is too small", title) };
  }
  if (maxSize != null && value > +(maxSize ?? 0)) {
    return { max: i18n.get("{0} is too big", title) };
  }
};

const validateString: Validate = (value, { props, context }) => {
  return (
    validateRequired(value, { props, context }) ||
    validateRange(value?.length, { props, context }) ||
    validatePattern(value, { props, context })
  );
};

const validateNumber: Validate = (value, { props, context }) => {
  return (
    validateRequired(value, { props, context }) ||
    validateRange(value, { props, context })
  );
};

const validateUuid: Validate = (value, { props, context }) => {
  return (
    validateRequired(value, { props, context }) ||
    (!isEmpty(value) &&
      !isUUID(value) && {
        invalid: i18n.get("{0} is not in proper format", props.title),
      } || undefined)
  );
};

export const Validators = {
  string: validateString,
  integer: validateNumber,
  long: validateNumber,
  decimal: validateNumber,
  uuid: validateUuid,
};

export const validate: Validate = (value, { props, context }) => {
  let type = props?.serverType ?? props?.type;
  if (type) type = toKebabCase(type);

  if (type === "enum") type = "enumeration";
  if (props?.selection) type = "selection";

  const func = Validators[type as keyof typeof Validators];
  if (func) {
    return func(value, { props, context });
  }

  const errors = validateRequired(value, { props, context });
  if (errors) {
    return errors;
  }
};
