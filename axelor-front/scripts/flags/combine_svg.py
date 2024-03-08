#!/usr/bin/env python3

import xml.etree.ElementTree as ET
import argparse
import os

DEFAULT_SIZE = 36
TARGET_SIZE = 24

ET.register_namespace("", "http://www.w3.org/2000/svg")


def combine_svgs_horizontally(svg_directory, output_file):
    namespaces = {"svg": "http://www.w3.org/2000/svg"}
    combined_svg = ET.Element("svg")
    current_x_offset = 0
    max_height = 0

    # Loop through all SVG files in the provided directory
    for svg_file in sorted(os.listdir(svg_directory)):
        if svg_file.endswith(".svg"):
            tree = ET.parse(os.path.join(svg_directory, svg_file))
            root = tree.getroot()

            # Original dimensions
            width = int(root.attrib.get("width", DEFAULT_SIZE))
            height = int(root.attrib.get("height", DEFAULT_SIZE))

            # Calculate scale based on target size (for uniform scaling, we consider height only or width depending on your preference)
            scale_factor = TARGET_SIZE / height

            # Scaled dimensions
            scaled_width = width * scale_factor
            scaled_height = TARGET_SIZE  # Since we are using height to calculate scale

            # Update max_height with scaled height
            max_height = max(max_height, scaled_height)

            # Apply transformation (scaling + translation) to group
            transform_value = f"translate({current_x_offset},0) scale({scale_factor})"
            group = ET.SubElement(
                combined_svg, "g", attrib={"transform": transform_value}
            )

            for element in root.findall(".//svg:*", namespaces=namespaces):
                group.append(element)

            # Update the current X offset by the scaled width
            current_x_offset += scaled_width

    combined_svg.set("width", str(current_x_offset))
    combined_svg.set("height", str(max_height))

    tree = ET.ElementTree(combined_svg)
    tree.write(output_file)


if __name__ == "__main__":
    # Parse arguments from command line
    parser = argparse.ArgumentParser(description="Combine SVG files horizontally.")
    parser.add_argument(
        "svg_directory", type=str, help="Directory containing SVG files to be combined"
    )
    parser.add_argument(
        "-o",
        "--output",
        type=str,
        default="combined.svg",
        help="Output filename for the combined SVG",
    )
    args = parser.parse_args()

    combine_svgs_horizontally(args.svg_directory, args.output)

    print(f"Combined SVG saved to {args.output}")
