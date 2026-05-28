import {
  CaretPositionConfig,
  ConformConfig,
  Mask,
  MaskArray,
  MaskFunction,
  MaskFunctionConfig,
  PLACEHOLDER,
} from "./types";

const EMPTY_STRING = "";

type CharMeta = { char: string; isNew: boolean };

export function convertMaskToPlaceholder(
  mask: MaskArray = [],
  placeholderChar = PLACEHOLDER,
) {
  return mask
    .map((char) => (char instanceof RegExp ? placeholderChar : char))
    .join("");
}

export function coerceValue(value?: string | number | null): string {
  if (typeof value === "string") return value;
  if (typeof value === "number" && Number.isFinite(value)) return String(value);
  if (value === undefined || value === null) return EMPTY_STRING;
  throw new Error(
    "The 'value' provided to MaskedInput needs to be a string or a number. Received: " +
      JSON.stringify(value),
  );
}

export function resolveMask(
  mask: Mask,
  rawValue: string,
  config?: MaskFunctionConfig,
): MaskArray | false {
  if (mask === false) return false;
  if (Array.isArray(mask)) return mask;
  return (mask as MaskFunction)(rawValue, config);
}

/**
 * Pads the deleted range with placeholder chars to preserve character positions
 * on deletion when keepCharPositions is enabled.
 */
function compensateForDeletion(
  rawValue: string,
  placeholder: string,
  placeholderChar: string,
  indexOfFirstChange: number,
  indexOfLastChange: number,
): string {
  let compensatoryPlaceholder = EMPTY_STRING;

  for (let i = indexOfFirstChange; i < indexOfLastChange; i++) {
    if (placeholder[i] === placeholderChar) {
      compensatoryPlaceholder += placeholderChar;
    }
  }

  return (
    rawValue.slice(0, indexOfFirstChange) +
    compensatoryPlaceholder +
    rawValue.slice(indexOfFirstChange, rawValue.length)
  );
}

/**
 * Builds the annotated `CharMeta[]` array from rawValue and removes characters
 * that match literal positions in the placeholder.
 */
function stripLiteralChars(
  rawValueArr: CharMeta[],
  placeholder: string,
  placeholderChar: string,
  indexOfFirstChange: number,
  lengthDifference: number,
  previousConformedValueLength: number,
  maskLength: number,
): CharMeta[] {
  for (let i = rawValueArr.length - 1; i >= 0; i--) {
    const char = rawValueArr[i]?.char;

    if (char !== placeholderChar) {
      const shouldOffset =
        i >= indexOfFirstChange && previousConformedValueLength === maskLength;
      if (char === placeholder[shouldOffset ? i - lengthDifference : i]) {
        rawValueArr.splice(i, 1);
      }
    }
  }

  return rawValueArr;
}

/**
 * Finds the index of the next placeholder slot in the remaining rawValueArr,
 * used inside the keepCharPositions+addition branch.
 */
function findNextPlaceholderIndex(
  rawValueArr: CharMeta[],
  placeholderChar: string,
): number | null {
  for (let j = 0; j < rawValueArr.length; j++) {
    const nextChar = rawValueArr[j];
    if (nextChar.char !== placeholderChar && nextChar.isNew === false) {
      break;
    }
    if (nextChar.char === placeholderChar) {
      return j;
    }
  }
  return null;
}

export function conformToMask(
  rawValue: string = EMPTY_STRING,
  mask: MaskArray,
  config: ConformConfig = {},
): string {
  const guide = config.guide ?? true;
  const previousConformedValue = config.previousConformedValue ?? EMPTY_STRING;
  const placeholderChar = config.placeholderChar ?? PLACEHOLDER;
  const placeholder =
    config.placeholder ?? convertMaskToPlaceholder(mask, placeholderChar);
  const currentCaretPosition = config.currentCaretPosition ?? 0;
  const keepCharPositions = config.keepCharPositions;
  const suppressGuide =
    guide === false && previousConformedValue !== EMPTY_STRING;
  const rawValueLength = rawValue.length;
  const previousConformedValueLength = previousConformedValue.length;
  const placeholderLength = placeholder.length;
  const maskLength = mask.length;
  const lengthDifference = rawValueLength - previousConformedValueLength;
  const isAddition = lengthDifference > 0;
  const indexOfFirstChange =
    currentCaretPosition + (isAddition ? -lengthDifference : 0);
  const indexOfLastChange = indexOfFirstChange + Math.abs(lengthDifference);

  if (keepCharPositions && !isAddition) {
    rawValue = compensateForDeletion(
      rawValue,
      placeholder,
      placeholderChar,
      indexOfFirstChange,
      indexOfLastChange,
    );
  }

  const rawValueArr = stripLiteralChars(
    rawValue.split(EMPTY_STRING).map((char, i) => ({
      char,
      isNew: i >= indexOfFirstChange && i < indexOfLastChange,
    })),
    placeholder,
    placeholderChar,
    indexOfFirstChange,
    lengthDifference,
    previousConformedValueLength,
    maskLength,
  );

  let conformedValue = EMPTY_STRING;

  outer: for (let i = 0; i < placeholderLength; i++) {
    const charInPlaceholder = placeholder[i];

    if (charInPlaceholder === placeholderChar) {
      if (rawValueArr.length > 0) {
        while (rawValueArr.length > 0) {
          const { char, isNew } = rawValueArr.shift()!;

          if (char === placeholderChar && suppressGuide !== true) {
            conformedValue += placeholderChar;
            continue outer;
          }

          const maskRule = mask[i];
          if (maskRule instanceof RegExp && maskRule.test(char)) {
            if (
              keepCharPositions &&
              isNew !== false &&
              previousConformedValue !== EMPTY_STRING &&
              guide !== false &&
              isAddition
            ) {
              const indexOfNextPlaceholder = findNextPlaceholderIndex(
                rawValueArr,
                placeholderChar,
              );

              if (indexOfNextPlaceholder !== null) {
                conformedValue += char;
                rawValueArr.splice(indexOfNextPlaceholder, 1);
              } else {
                i--;
              }
            } else {
              conformedValue += char;
            }

            continue outer;
          }
        }
      }

      if (suppressGuide === false) {
        conformedValue += placeholder.slice(i, placeholderLength);
        break;
      }

      break;
    } else {
      conformedValue += charInPlaceholder;
    }
  }

  if (suppressGuide && !isAddition) {
    let lastFilledIndex: number | null = null;

    for (let i = 0; i < conformedValue.length; i++) {
      if (placeholder[i] === placeholderChar) {
        lastFilledIndex = i;
      }
    }

    conformedValue =
      lastFilledIndex !== null
        ? conformedValue.slice(0, lastFilledIndex + 1)
        : EMPTY_STRING;
  }

  return conformedValue;
}

/**
 * Encapsulates: normalized char filtering, literal count comparison,
 * target char resolution, and match scanning for caret positioning.
 */
function findTargetCaretPosition(
  conformedValue: string,
  rawValue: string,
  placeholder: string,
  previousPlaceholder: string,
  placeholderChar: string,
  currentCaretPosition: number,
  isAddition: boolean,
): { caretPosition: number; targetChar?: string } {
  const rawValueArr = rawValue
    .slice(0, currentCaretPosition)
    .split(EMPTY_STRING);
  const filteredRawValueArr = rawValueArr.filter(
    (char) => conformedValue.indexOf(char) !== -1,
  );

  let targetChar: string | undefined =
    filteredRawValueArr[filteredRawValueArr.length - 1];

  const previousLiteralCount = previousPlaceholder
    .slice(0, filteredRawValueArr.length)
    .split(EMPTY_STRING)
    .filter((char) => char !== placeholderChar).length;
  const placeholderLiteralCount = placeholder
    .slice(0, filteredRawValueArr.length)
    .split(EMPTY_STRING)
    .filter((char) => char !== placeholderChar).length;

  const isTargetEscaped = placeholderLiteralCount !== previousLiteralCount;
  const isPrevCharSameAsPlaceholder =
    typeof previousPlaceholder[filteredRawValueArr.length - 1] !==
      "undefined" &&
    typeof placeholder[filteredRawValueArr.length - 2] !== "undefined" &&
    previousPlaceholder[filteredRawValueArr.length - 1] !== placeholderChar &&
    previousPlaceholder[filteredRawValueArr.length - 1] !==
      placeholder[filteredRawValueArr.length - 1] &&
    previousPlaceholder[filteredRawValueArr.length - 1] ===
      placeholder[filteredRawValueArr.length - 2];

  if (
    !isAddition &&
    (isTargetEscaped || isPrevCharSameAsPlaceholder) &&
    previousLiteralCount > 0 &&
    placeholder.indexOf(targetChar ?? "") > -1 &&
    typeof rawValue[currentCaretPosition] !== "undefined"
  ) {
    targetChar = rawValue[currentCaretPosition];
  }

  const countPrevCharMatches = filteredRawValueArr.filter(
    (char) => char === targetChar,
  ).length;
  const placeholderMatches = placeholder
    .slice(0, placeholder.indexOf(placeholderChar))
    .split(EMPTY_STRING)
    .filter(
      (char, index) => char === targetChar && rawValue[index] !== char,
    ).length;

  const requiredMatches = placeholderMatches + countPrevCharMatches;

  let encounteredMatches = 0;
  let caretPosition = 0;

  for (let i = 0; i < conformedValue.length; i++) {
    const char = conformedValue[i];
    caretPosition = i + 1;

    if (char === targetChar && ++encounteredMatches >= requiredMatches) {
      break;
    }
  }

  return { caretPosition, targetChar };
}

export function adjustCaretPosition({
  previousConformedValue = EMPTY_STRING,
  previousPlaceholder = EMPTY_STRING,
  currentCaretPosition = 0,
  conformedValue,
  rawValue,
  placeholderChar,
  placeholder,
}: CaretPositionConfig): number {
  if (currentCaretPosition === 0 || !rawValue.length) {
    return 0;
  }

  const rawValueLength = rawValue.length;
  const previousConformedLength = previousConformedValue.length;
  const placeholderLength = placeholder.length;
  const editLength = rawValueLength - previousConformedLength;
  const isAddition = editLength > 0;
  const isFirstInput = previousConformedLength === 0;
  const isPartialMultiCharEdit = editLength > 1 && !isAddition && !isFirstInput;

  if (isPartialMultiCharEdit) {
    return currentCaretPosition;
  }

  const shouldSetCaretToEnd =
    isAddition &&
    (previousConformedValue === conformedValue ||
      conformedValue === placeholder);

  const caretPosition = shouldSetCaretToEnd
    ? currentCaretPosition - editLength
    : findTargetCaretPosition(
        conformedValue,
        rawValue,
        placeholder,
        previousPlaceholder,
        placeholderChar,
        currentCaretPosition,
        isAddition,
      ).caretPosition;

  if (isAddition) {
    for (let i = caretPosition; i <= placeholderLength; i++) {
      if (placeholder[i] === placeholderChar) return i;
      if (i === placeholderLength) return caretPosition;
    }
  } else {
    for (let i = caretPosition; i >= 0; i--) {
      if (placeholder[i - 1] === placeholderChar || i === 0) {
        return i;
      }
    }
  }

  return caretPosition;
}
