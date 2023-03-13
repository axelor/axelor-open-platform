import { forwardRef } from "react";
import ReactDatePicker from "react-datepicker";
import { useDateFnsLocale } from "@/hooks/use-date-fns-locale";
import Loading from "../loading";

import "react-datepicker/dist/react-datepicker.css";
import "./date-picker.scss";

const DatePicker = forwardRef((props, ref) => {
  const dateFnsLocale = useDateFnsLocale();
  return dateFnsLocale ? <ReactDatePicker {...props} ref={ref} /> : <Loading />;
});

export default DatePicker;
