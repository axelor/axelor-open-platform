import { Barcode as BarcodeUi } from "@axelor/ui";
import { useAtom } from "jotai";
import { useState } from "react";

import { FieldControl, FieldProps } from "../../builder";
import { String } from "../string";

export function Barcode(props: FieldProps<string>) {
  const { schema, readonly, valueAtom } = props;
  const { height, widgetAttrs } = schema;
  const {
    barcodeWidth,
    barcodeLineColor,
    barcodeDisplayValue,
    barcodeBackgroundColor,
    barcodeFormat,
  } = widgetAttrs;

  const [value] = useAtom(valueAtom);

  const [isBarcodeValid, setIsBarcodeValid] = useState(true);

  // Reset visibility when value change
  const [prevValue, setPrevValue] = useState(value);
  if (value !== prevValue) {
    setPrevValue(value);
    setIsBarcodeValid(true);
  }

  if (readonly && value)
    return (
      <FieldControl {...props}>
        {isBarcodeValid && (
          <BarcodeUi
            value={value}
            height={height}
            barWidth={barcodeWidth}
            lineColor={barcodeLineColor}
            displayValue={barcodeDisplayValue}
            backgroundColor={barcodeBackgroundColor}
            format={barcodeFormat}
            onInvalid={() => setIsBarcodeValid(false)}
            data-testid="barcode"
          />
        )}
      </FieldControl>
    );

  return <String {...props} />;
}
