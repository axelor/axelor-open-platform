const DATE_PICKER_INTERACTIVE_SELECTOR =
  ".react-datepicker, .react-datepicker-popper, .react-datepicker__portal";

export function isDatePickerTarget(target: EventTarget | null): boolean {
  return target instanceof Element
    ? Boolean(target.closest(DATE_PICKER_INTERACTIVE_SELECTOR))
    : false;
}
