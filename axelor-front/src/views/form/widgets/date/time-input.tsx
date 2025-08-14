import {
  forwardRef,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { Box, ClickAwayListener, Popper, clsx } from "@axelor/ui";
import padStart from "lodash/padStart";

import styles from "./time-unit.module.scss";

interface TimeInputProps {
  dateValue?: Date | null;
  format?: string;
  value?: string;
  onUpdate?: (value: Date) => void;
}

const toText = (val: number) => padStart(`${val}`, 2, "0");

type TimeSelectOption = { value: string; title: string };

function TimeSelect({
  size,
  value = "0",
  onChange,
}: {
  size: number;
  value: string;
  onChange: (val: string) => void;
}) {
  const [open, setOpen] = useState(false);
  const [popperContent, setPopperContent] = useState<HTMLDivElement | null>(
    null,
  );
  const boxRef = useRef<HTMLDivElement>(null);

  const options = useMemo(
    () =>
      new Array(size).fill(0).map(
        (_, ind) =>
          ({
            value: `${ind}`,
            title: toText(ind),
          }) as TimeSelectOption,
      ),
    [size],
  );

  const openPicker = useCallback(() => setOpen(true), []);
  const closePicker = useCallback(() => setOpen(false), []);

  const selected = useMemo(
    () => options.find((x) => x.value === String(+value)) ?? null,
    [options, value],
  );

  function handleChange(opt: TimeSelectOption) {
    onChange(opt.value);
    closePicker();
  }

  useEffect(() => {
    if (open && popperContent) {
      const item = popperContent.querySelector("[aria-selected='true']");
      item?.scrollIntoView({
        block: "center",
      });
    }
  }, [open, popperContent]);

  return (
    <>
      <Box
        ref={boxRef}
        d="flex"
        position="relative"
        className={clsx(styles.timeInputSelect, { [styles.open]: open })}
        onClick={openPicker}
      >
        {selected?.title}
        <Box className={styles.arrow} />
      </Box>
      <Popper
        placement={"bottom"}
        target={boxRef.current!}
        open={open}
        disablePortal
      >
        <ClickAwayListener onClickAway={closePicker}>
          <Box ref={setPopperContent} className={styles.popperContent}>
            {options.map((opt) => {
              const isSelected = selected === opt;
              return (
                <Box
                  className={styles.option}
                  key={opt.value}
                  onClick={() => handleChange(opt)}
                  {...(isSelected && { ["aria-selected"]: true })}
                >
                  {isSelected && <span className={styles.tick}>✓</span>}
                  {opt.title}
                </Box>
              );
            })}
          </Box>
        </ClickAwayListener>
      </Popper>
    </>
  );
}

export const TimeInput = forwardRef<HTMLDivElement, TimeInputProps>(
  ({ format, value, dateValue: propDate, onUpdate }, ref) => {
    const [times, setTimes] = useState({
      hour: "",
      minute: "",
      second: "",
    });
    const hasSeconds = format?.includes("ss");

    const setValue = (hour: string, minute: string, second: string) => {
      const date = propDate || new Date();

      date.setHours(Number(hour || 0));
      date.setMinutes(Number(minute || 0));
      date.setSeconds(hasSeconds ? Number(second || 0) : 0);

      setTimes({ hour, minute, second });
      onUpdate?.(date);
    };

    function handleHourChange(val: string) {
      const { minute, second } = times;
      setValue(val, minute, second);
    }

    function handleMinuteChange(val: string) {
      const { hour, second } = times;
      setValue(hour, val, second);
    }

    function handleSecondChange(val: string) {
      const { hour, minute } = times;
      setValue(hour, minute, val);
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
      <Box d="flex" gap={10} ref={ref}>
        <TimeSelect value={times.hour} size={24} onChange={handleHourChange} />
        <TimeSelect
          value={times.minute}
          size={60}
          onChange={handleMinuteChange}
        />
        {hasSeconds && (
          <TimeSelect
            value={times.second}
            size={60}
            onChange={handleSecondChange}
          />
        )}
      </Box>
    );
  },
);
