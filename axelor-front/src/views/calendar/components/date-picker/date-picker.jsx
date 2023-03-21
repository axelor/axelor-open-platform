import { forwardRef } from "react";
import ReactDatePicker from "react-datepicker";
import { useDateFnsLocale } from "@/hooks/use-date-fns-locale";

import "react-datepicker/dist/react-datepicker.css";
import "./date-picker.scss";

const DatePicker = forwardRef((props, ref) => {
  const dateFnsLocale = useDateFnsLocale();
  return dateFnsLocale ? <ReactDatePicker {...props} ref={ref} /> : null;
});

export default DatePicker;
