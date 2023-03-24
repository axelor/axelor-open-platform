import { forwardRef } from "react";
import ReactDatePicker from "react-datepicker";

import "react-datepicker/dist/react-datepicker.css";
import "./date-picker.scss";

const DatePicker = forwardRef((props, ref) => {
  return <ReactDatePicker {...props} ref={ref} />;
});

export default DatePicker;
