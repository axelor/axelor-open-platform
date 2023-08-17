import { forwardRef } from "react";
import padStart from "lodash/padStart";
import { Box } from "@axelor/ui";

interface TimeInputProps {
  format?: string;
  value?: string;
  onChange?: (value: string) => void;
}

export const TimeInput = forwardRef<HTMLDivElement, TimeInputProps>(
  ({ format, value, onChange }, ref) => {
    const [hr, mm, ss] = `${value}`.split(":");
    const hasSeconds = format?.includes("ss");

    const toText = (val: number) => padStart(`${val}`, 2, "0");
    const setValue = (hr: string, mm: string, ss: string) =>
      onChange && onChange(`${hr}:${mm}${hasSeconds ? `${ss}` : ""}`);

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
      onChange: (value: string) => void
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
        {hasSeconds && renderSelect(24, ss, handleSecondChange)}
      </div>
    );
  }
);
