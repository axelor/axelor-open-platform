import { forwardRef, useEffect, useMemo, useState } from "react";
import { Box } from "@axelor/ui";
import padStart from "lodash/padStart";

interface TimeInputProps {
  dateValue?: Date | null;
  format?: string;
  value?: string;
  onUpdate?: (value: Date) => void;
}

export const TimeInput = forwardRef<HTMLDivElement, TimeInputProps>(
  ({ format, value, dateValue: propDate, onUpdate }, ref) => {
    const [{ hour, minute, second }, setTimes] = useState({
      hour: "",
      minute: "",
      second: "",
    });
    const hasSeconds = format?.includes("ss");

    const toText = (val: number) => padStart(`${val}`, 2, "0");
    const setValue = (hour: string, minute: string, second: string) => {
      const date = propDate || new Date();

      date.setHours(Number(hour || 0));
      date.setMinutes(Number(minute || 0));
      date.setSeconds(hasSeconds ? Number(second || 0) : 0);

      setTimes({ hour, minute, second });
      onUpdate?.(date);
    };

    function handleHourChange(value: string) {
      setValue(value, minute, second);
    }

    function handleMinuteChange(value: string) {
      setValue(hour, value, second);
    }

    function handleSecondChange(value: string) {
      setValue(hour, minute, value);
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

    const timeAsStr = useMemo(
      () =>
        `${
          (propDate
            ? (propDate as Date)?.toLocaleTimeString(undefined, {
                hour12: false,
                hour: "2-digit",
                minute: "2-digit",
                second: "2-digit",
              })
            : null) || value
        }`,
      [propDate, value],
    );

    useEffect(() => {
      const [hour, minute, second] = timeAsStr.split(":");
      setTimes({ hour, minute, second });
    }, [timeAsStr]);

    return (
      <div ref={ref}>
        {renderSelect(24, hour, handleHourChange)}
        {renderSelect(60, minute, handleMinuteChange)}
        {hasSeconds && renderSelect(60, second, handleSecondChange)}
      </div>
    );
  },
);
