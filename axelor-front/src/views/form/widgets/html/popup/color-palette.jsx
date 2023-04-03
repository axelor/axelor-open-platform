import { cancelEvent } from "../utils";

function HSVtoRGB(h, s, v) {
  let r, g, b, i, f, p, q, t;

  i = Math.floor(h * 6);
  f = h * 6 - i;
  p = v * (1 - s);
  q = v * (1 - f * s);
  t = v * (1 - (1 - f) * s);

  switch (i % 6) {
    case 0:
      [r, g, b] = [v, t, p];
      break;
    case 1:
      [r, g, b] = [q, v, p];
      break;
    case 2:
      [r, g, b] = [p, v, t];
      break;
    case 3:
      [r, g, b] = [p, q, v];
      break;
    case 4:
      [r, g, b] = [t, p, v];
      break;
    case 5:
      [r, g, b] = [v, p, q];
      break;
    default:
      break;
  }

  const [hr, hg, hb] = [
    Math.floor(r * 255).toString(16),
    Math.floor(g * 255).toString(16),
    Math.floor(b * 255).toString(16),
  ];

  return `#${`0${hr}`.slice(-2)}${`0${hg}`.slice(-2)}${`0${hb}`.slice(-2)}`;
}

function ColorPalettePopup({ commands, onClick }) {
  const rows = [];

  for (let row = 1; row < 15; ++row) {
    const row = [];
    for (let col = 0; col < 25; ++col) {
      let color;
      if (col === 24) {
        const gray = Math.floor((255 / 13) * (14 - row)).toString(16);
        const hexg = (gray.length < 2 ? "0" : "") + gray;
        color = "#" + hexg + hexg + hexg;
      } else {
        const hue = col / 24;
        const saturation = row <= 8 ? row / 8 : 1;
        const value = row > 8 ? (16 - row) / 8 : 1;
        color = HSVtoRGB(hue, saturation, value);
      }
      row.push(color);
    }
    rows.push(row);
  }

  return (
    <table className="color-palette-table">
      <tbody>
        {rows.map((cols, ind) => (
          <tr key={ind}>
            {cols.map((color, ind) => (
              <td
                key={ind}
                style={{ backgroundColor: color }}
                title={color}
                onClick={(e) => {
                  cancelEvent(e);
                  onClick && onClick(commands, color);
                }}
              />
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
}

export default ColorPalettePopup;
