const usedColors: Record<string, string> = {};
const randomColors: Record<string, string> = {};

let usedColorIndex = 0;

const colors: string[] = [
  "#304ffe",
  "#ef6c00",
  "#2e7d32",
  "#00695c",
  "#c62828",
  "#6a1b9a",
  "#ad1457",
  "#00838f",
  "#4e342e",
  "#0d47a1",
  "#e65100",
  "#1b5e20",
  "#004d40",
  "#4a148c",
  "#b71c1c",
  "#880e4f",
  "#006064",
  "#3e2723",
  "#1976d2",
  "#f57c00",
  "#388e3c",
  "#00796b",
  "#7b1fa2",
  "#d32f2f",
  "#c2185b",
  "#0097a7",
  "#5d4037",
  "#1e88e5",
  "#fb8c00",
  "#43a047",
  "#00897b",
  "#8e24aa",
  "#e53935",
  "#d81b60",
  "#00acc1",
  "#6d4c41",
  "#2196f3",
  "#ff9800",
  "#4caf50",
  "#009688",
  "#9c27b0",
  "#f44336",
  "#e91e63",
  "#00bcd4",
  "#795548",
];

export const DEFAULT_COLOR = "var(--bs-link-color)";

export function getColor(value: string): string {
  function findColor() {
    const color = colors[usedColorIndex];
    usedColorIndex = (usedColorIndex + 1) % colors.length;
    return color;
  }
  return usedColors[value] || (usedColors[value] = findColor());
}

export function getRandomColor(value: string): string {
  function findColor() {
    const used = Object.values(randomColors);
    let list = colors.filter((c) => !used.includes(c));
    if (!list.length) {
      list = colors;
    }
    return list[Math.floor(Math.random() * list.length)];
  }
  return randomColors[value] || (randomColors[value] = findColor());
}
