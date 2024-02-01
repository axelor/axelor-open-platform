import { defaultCountries } from "react-international-phone";

export const FLAGS = defaultCountries.map((country) => {
  const iso2 = country[1];
  const src = `img/flags/${toSymbol(iso2)}.svg`;
  return { iso2, src };
});

export const FLAG_SOURCES = FLAGS.reduce(
  (acc, flag) => {
    const { iso2, src } = flag;
    acc[iso2] = src;
    return acc;
  },
  {} as Record<string, string>,
);

function toSymbol(code: string) {
  const offset = 0x41 - 0x1f1e6; // "A" - "🇦"
  return Array.from(code.toUpperCase())
    .map((letter) => ((letter.codePointAt(0) ?? 0) - offset).toString(16))
    .join("-");
}
