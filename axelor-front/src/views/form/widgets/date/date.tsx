import { useAtom, useAtomValue } from "jotai";
import {
  FocusEvent,
  KeyboardEvent,
  MouseEvent,
  SyntheticEvent,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";

import { Box, FocusTrap, useClassNames } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { i18n } from "@/services/client/i18n";
import { l10n, moment } from "@/services/client/l10n";
import { Field, Schema } from "@/services/client/meta.types";
import { getDateTimeFormat, getTimeFormat } from "@/utils/format";
import { toCamelCase } from "@/utils/names";

import { FieldControl, FieldProps, WidgetState } from "../../builder";
import { ViewerInput } from "../string/viewer";
import { DateInput } from "./date-input";
import { Picker } from "./picker";
import { TimeInput } from "./time-input";

function focusInput(inputEl?: HTMLElement) {
  let input = inputEl && inputEl.querySelector("input,textarea");
  if (!input && ["INPUT", "TEXTAREA"].includes(inputEl?.tagName ?? "")) {
    input = inputEl;
  }
  input && (input as HTMLInputElement).focus();
  input && (input as HTMLInputElement).select();
}

function toCalendarFormat(format: string) {
  return format
    .split("")
    .map((c) => (["M", "H"].includes(c) ? c : `${c}`.toLowerCase()))
    .join("");
}

export function DateComponent({
  schema,
  readonly,
  invalid,
  attrs,
  value,
  onChange,
  trapFocus,
}: {
  schema: Schema;
  value?: string | null;
  onChange: (value: string | null, callOnChange?: boolean) => void;
  attrs?: WidgetState["attrs"];
  readonly?: boolean;
  invalid?: boolean;
  trapFocus?: boolean;
}) {
  const { focus } = attrs || {};
  const { uid, placeholder } = schema;
  const pickerRef = useRef<any>();
  const boxRef = useRef<HTMLDivElement>(null);
  const classNames = useClassNames();
  const [open, setOpen] = useState(false);
  const [changed, setChanged] = useState(false);

  const type =
    toCamelCase(
      schema.widget || schema.serverType || schema.type || "",
    )?.toLowerCase() ?? "";
  const isDateTime = type !== "date";
  const hasSeconds = schema.seconds || schema.widgetAttrs?.seconds;
  const dateFormats = useMemo<Record<string, string[]>>(
    () => ({
      datetime: [
        `YYYY-MM-DDTHH:mm${hasSeconds ? ":ss" : ""}`,
        getDateTimeFormat({ props: schema as Field }),
      ],
      date: ["YYYY-MM-DD", l10n.getDateFormat()],
      time: [
        `HH:mm${hasSeconds ? ":ss" : ""}`,
        getTimeFormat({ props: schema as Field }),
      ],
    }),
    [schema, hasSeconds],
  );
  const [valueFormat, format] = dateFormats[type] || dateFormats.date;

  const getInput = useCallback(() => {
    const calendar = pickerRef.current;
    return calendar?.input?.inputElement as HTMLElement;
  }, []);

  const handleOpen = useCallback((focus?: boolean) => {
    setOpen(true);
    if (focus) {
      setTimeout(() => {
        const calendar = pickerRef.current;
        const selectedDay = calendar?.calendar?.componentNode.querySelector(
          '.react-datepicker__day[tabindex="0"]',
        );
        selectedDay && selectedDay.focus({ preventScroll: true });
      }, 100);
    }
  }, []);

  const handleBlur = useCallback(
    (e?: FocusEvent<HTMLInputElement>) => {
      if (changed) {
        let val = value ?? null;
        if (e) {
          const targetValue = e.target?.value ?? null;
          val =
            targetValue && moment(targetValue, format).isValid()
              ? moment(targetValue, format).format(valueFormat)
              : null;
        }
        onChange(val, true);
        setChanged(false);
      }
    },
    [changed, value, onChange, format, valueFormat],
  );

  const handleClose = useCallback(
    (focus?: boolean) => {
      setOpen(false);
      if (focus) {
        focusInput(getInput());
      } else {
        handleBlur();
      }
    },
    [getInput, handleBlur],
  );

  const handleClickOutSide = useCallback(
    (event: MouseEvent<HTMLDivElement>) => {
      const container = boxRef.current;
      if (container && container.contains(event.target as Node)) {
        return;
      }
      handleClose();
    },
    [handleClose],
  );

  const handleKeyDown = useCallback(
    (e: KeyboardEvent<HTMLElement>) => {
      if (open) {
        if (["Tab", "Escape"].includes(e.key)) {
          handleClose(true);
        }
        e.preventDefault();
      }
    },
    [open, handleClose],
  );

  const handleSelect = useCallback(() => {
    handleClose(true);
  }, [handleClose]);

  const handleChange = useCallback(
    (newValue: Date | string | null, event?: SyntheticEvent) => {
      if (
        (event?.target as HTMLElement)?.className ===
        "react-datepicker__now-button"
      ) {
        newValue = moment().format(valueFormat);
        if (newValue === value) return;
      }

      const callOnChange = event?.type === "click" ? true : false;

      onChange(
        newValue && moment(newValue).isValid()
          ? moment(newValue).format(valueFormat)
          : null,
        callOnChange,
      );
      setChanged(!callOnChange);
    },
    [onChange, valueFormat, value],
  );

  const $date = useMemo(() => {
    if (!value) return null;
    const $m = moment(value);
    return $m.isValid() ? $m : null;
  }, [value]);

  const dateValue = useMemo(() => ($date ? $date.toDate() : $date), [$date]);

  const textValue = useMemo(
    () => ($date ? $date.format(format) : ""),
    [format, $date],
  );

  useEffect(() => {
    // if value exist and it's invalid moment date value
    // then it should reset to null
    if (value && !$date) {
      onChange(null, false);
    }
  }, [value, $date, onChange]);

  function render() {
    return (
      <Box ref={boxRef} d="flex">
        <Picker
          id={uid}
          showMonthDropdown
          showYearDropdown
          todayButton={
            isDateTime ? (
              <Box className={classNames("react-datepicker__now-button")}>
                {i18n.get("Now")}
              </Box>
            ) : (
              i18n.get("Today")
            )
          }
          className={classNames("form-control")}
          placeholderText={placeholder}
          showPopperArrow={false}
          portalId="root-app"
          autoFocus={focus || open}
          open={open}
          ref={pickerRef}
          dateFormat={toCalendarFormat(format)}
          selected={dateValue}
          textValue={textValue}
          customInput={
            <DateInput
              invalid={invalid}
              eventOnBlur={handleBlur}
              format={format}
              open={open}
              onOpen={handleOpen}
              onClose={handleClose}
            />
          }
          timeInputLabel={
            (
              <Box>
                <MaterialIcon icon="schedule" fontSize={20} />
              </Box>
            ) as any
          }
          showTimeInput={isDateTime}
          customTimeInput={
            <TimeInput
              hasDate={Boolean(dateValue)}
              format={format}
              onDateChange={handleChange}
            />
          }
          onSelect={handleSelect}
          onChange={handleChange}
          onBlur={handleBlur}
          onKeyDown={handleKeyDown}
          onClickOutside={handleClickOutSide}
        />
      </Box>
    );
  }

  return readonly ? (
    <ViewerInput value={textValue} />
  ) : trapFocus ? (
    <FocusTrap enabled={open}>{render()}</FocusTrap>
  ) : (
    render()
  );
}

export function Date(props: FieldProps<string>) {
  const { schema, readonly, widgetAtom, valueAtom, invalid } = props;
  const [value, setValue] = useAtom(valueAtom);
  const { attrs } = useAtomValue(widgetAtom);
  return (
    <FieldControl {...props}>
      <DateComponent
        schema={schema}
        readonly={readonly}
        invalid={invalid}
        attrs={attrs}
        value={value}
        onChange={setValue}
      />
    </FieldControl>
  );
}
