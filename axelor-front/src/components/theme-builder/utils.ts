function isThemeColor(color: string) {
  return [
    "primary",
    "secondary",
    "success",
    "info",
    "warning",
    "danger",
    "light",
    "dark",
    "gray_dark",
    "gray_100",
    "gray_200",
    "gray_300",
    "gray_400",
    "gray_500",
    "gray_600",
    "gray_700",
    "gray_800",
    "gray_900",
  ].includes(color);
}

export function isValidCssValue(property?: string, value?: any) {
  if (!value || !property) return true;
  if (property === "color" && isThemeColor(value)) {
    return true;
  }
  return CSS.supports(property, value);
}
