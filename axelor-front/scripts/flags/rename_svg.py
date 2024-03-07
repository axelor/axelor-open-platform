#!/usr/bin/env python3

import os
import shutil
import argparse


def codepoint_to_letter(code):
    # Subtracting the base to align with ASCII values for A-Z
    return chr(int(code, 16) - 0x1F1E6 + ord("A"))


def rename_file(original_name):
    name_parts = os.path.splitext(original_name)[0].split("-")
    new_name_parts = []
    for part in name_parts:
        new_name_parts.append(codepoint_to_letter(part))

    return "".join(new_name_parts).lower() + ".svg"


def main(source_directory, target_directory):
    os.makedirs(target_directory, exist_ok=True)

    for filename in os.listdir(source_directory):
        if filename.endswith(".svg"):
            new_filename = rename_file(filename)
            source_path = os.path.join(source_directory, filename)
            target_path = os.path.join(target_directory, new_filename)

            shutil.copy(source_path, target_path)
            print(f"Copied and renamed {filename} to {new_filename}")


if __name__ == "__main__":
    # Parse arguments from command line
    parser = argparse.ArgumentParser(
        description="Copy files from source to target directory and rename them."
    )
    parser.add_argument(
        "source_directory", type=str, help="Path to the source directory"
    )
    parser.add_argument(
        "target_directory", type=str, help="Path to the target directory"
    )

    args = parser.parse_args()

    main(args.source_directory, args.target_directory)
