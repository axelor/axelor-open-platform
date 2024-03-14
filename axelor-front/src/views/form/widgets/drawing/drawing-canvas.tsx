import { useCallback, useEffect, useRef } from "react";

import { Box } from "@axelor/ui";

import { i18n } from "@/services/client/i18n";

import SignaturePad from "signature_pad";

import styles from "./drawing-canvas.module.scss";

interface SignaturePadProps {
  drawingHeight?: string;
  drawingWidth?: string;
  strokeColor?: string;
  strokeWidth?: string;
  setSignaturePad: (value: SignaturePad) => void;
  maximize?: boolean;
}

export default function DrawingCanvas(props: SignaturePadProps) {
  const {
    drawingHeight = "200",
    drawingWidth = "500",
    strokeColor = "black",
    strokeWidth = "0.5",
    setSignaturePad,
  } = props;

  const canvasContainerRef = useRef<HTMLDivElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const signaturePadRef = useRef<SignaturePad | null>(null);

  const handleClear = useCallback(() => {
    signaturePadRef.current?.clear();
  }, []);

  useEffect(() => {
    if (!canvasRef.current) return;

    const signaturePad = new SignaturePad(canvasRef.current, {
      penColor: strokeColor,
      minWidth: parseFloat(strokeWidth),
      maxWidth: parseFloat(strokeWidth),
      dotSize: parseFloat(strokeWidth),
    });
    setSignaturePad(signaturePad);
    signaturePadRef.current = signaturePad;
  }, [setSignaturePad, strokeColor, strokeWidth]);

  return (
    <Box
      d="flex"
      flex={1}
      flexDirection="column"
      justifyContent="center"
      alignItems="center"
      ref={canvasContainerRef}
    >
      <Box
        as="canvas"
        height={drawingHeight}
        width={drawingWidth}
        ref={canvasRef}
        className={styles.canvas}
        border
      />
      <Box as="a" onClick={handleClear} className={styles.clearButton}>
        {i18n.get("Clear")}
      </Box>
    </Box>
  );
}
