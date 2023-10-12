import { Box } from "@axelor/ui";
import padStart from "lodash/padStart";
import { SyntheticEvent, forwardRef } from "react";

interface TimeInputProps {
  date?: Date | number;
  hasDate?: boolean;
  format?: string;
  value?: string;
  onChange?: (value: string) => void;
  onDateChange?: (value: Date | string | null, event?: SyntheticEvent) => void;
}

export const TimeInput = forwardRef<HTMLDivElement, TimeInputProps>(
  ({ format, value, hasDate, date: propDate, onDateChange }, ref) => {
    const [hr, mm, ss] = `${
      (hasDate && propDate
        ? (propDate as Date)?.toLocaleTimeString(undefined, {
            hour12: false,
            hour: "2-digit",
            minute: "2-digit",
            second: "2-digit",
          })
        : null) || value
    }`.split(":");
    const hasSeconds = format?.includes("ss");

    const toText = (val: number) => padStart(`${val}`, 2, "0");
    const setValue = (hr: string, mm: string, ss: string) => {
      const isPropDateValid =
        hasDate &&
        propDate instanceof Date &&
        !isNaN(propDate as unknown as number);
      const date = isPropDateValid ? propDate : new Date();

      date.setHours(Number(hr || 0));
      date.setMinutes(Number(mm || 0));
      date.setSeconds(hasSeconds ? Number(ss || 0) : 0);

      onDateChange?.(date);
    };

    function handleHourChange(value: string) {
      setValue(value, mm, ss);
    }

    function handleMinuteChange(value: string) {
      setValue(hr, value, ss);
    }

    function handleSecondChange(value: string) {
      setValue(hr, mm, value);
    }

    function renderSelect(
      size: number,
      value: string,
      onChange: (value: string) => void,
    ) {
      return (
        <Box
          mx={1}
          as="select"
          value={`${parseInt(value)}`}
          onChange={(e: any) => onChange(e.target.value!)}
          className={"form-control-sm"}
        >
          {new Array(size).fill(0).map((_, ind) => {
            const text = toText(ind);
            return (
              <option key={ind} value={ind}>
                {text}
              </option>
            );
          })}
        </Box>
      );
    }

    return (
      <div ref={ref}>
        {renderSelect(24, hr, handleHourChange)}
        {renderSelect(60, mm, handleMinuteChange)}
        {hasSeconds && renderSelect(60, ss, handleSecondChange)}
      </div>
    );
  },
);
