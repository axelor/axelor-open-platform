#!/usr/bin/env python3

import argparse
import os

WIDTH = 24

CSS_TEMPLATE = """.flag[data-country="{country_code}"] {{
  object-position: {position}px 0px;
}}

"""

CSS_INITIAL = """.flag {
  height: 24px;
  width: 24px;
  object-fit: none;
}

"""


# Function to generate CSS from country codes
def generate_css(country_codes):
    css_styles = CSS_INITIAL

    for index, code in enumerate(country_codes):
        position = -WIDTH * index
        css_styles += CSS_TEMPLATE.format(country_code=code, position=position)
    return css_styles


if __name__ == "__main__":
    # Parse arguments from command line
    parser = argparse.ArgumentParser(description="Generate CSS for country flags.")
    parser.add_argument("dir", type=str, help="Directory containing flag SVG files.")
    parser.add_argument(
        "-o", "--output", type=str, default="output.css", help="Output CSS file name."
    )
    args = parser.parse_args()

    # Retrieve country codes from specified directory
    try:
        country_codes = [
            os.path.splitext(filename)[0]
            for filename in sorted(os.listdir(args.dir))
            if filename.endswith(".svg")
        ]
    except Exception as e:
        print(f"Error reading directory {args.dir}: {e}")
        exit(1)

    # Generate CSS
    css = generate_css(country_codes)

    # Write the generated CSS to the specified output file
    try:
        with open(args.output, "w") as file:
            file.write(css.strip() + "\n")
        print(f"CSS successfully generated and saved to {args.output}")
    except Exception as e:
        print(f"Error writing to file {args.output}: {e}")
