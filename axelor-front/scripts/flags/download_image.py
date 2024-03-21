#!/usr/bin/env python3

import os
import sys
import requests
import argparse
from concurrent.futures import ThreadPoolExecutor, as_completed


def download_image(url, directory, timeout=10):
    """
    Download an image from a URL into a specified directory, unless the file already exists.

    Args:
    - url (str): The URL of the image.
    - directory (str): The directory where the image will be saved.
    """

    filename = url.split("/")[-1]  # Extract filename from URL
    filepath = os.path.join(directory, filename)

    if os.path.exists(filepath):
        print(f"{filename} already exists in {directory}; skipping download.")
        return True  # Skip downloading if the file already exists

    try:
        response = requests.get(url, timeout=timeout)
        response.raise_for_status()  # Raises stored HTTPError, if one occurred

        with open(filepath, "wb") as f:
            f.write(response.content)
    except requests.HTTPError as e:
        print(f"HTTP error occurred: {e}", file=sys.stderr)
    except requests.ConnectionError as e:
        print(f"Connection error occurred: {e}", file=sys.stderr)
    except requests.Timeout as e:
        print(f"Request timed out: {e}", file=sys.stderr)
    except Exception as e:
        print(f"Other error occurred: {e}", file=sys.stderr)
    else:
        print(f"Successfully downloaded {filename} to {directory}")
        return True

    return False


def download_parallel(urls, directory, threads):
    """
    Manage downloading of files in parallel.

    Args:
    - urls (list): List of URLs to download.
    - directory (str): Directory to save the images.
    - threads (int): Number of threads to use for parallel downloads.
    """

    if not os.path.exists(directory):
        os.makedirs(directory)  # Create the directory if it doesn't exist

    with ThreadPoolExecutor(max_workers=threads) as executor:
        futures = (executor.submit(download_image, url, directory) for url in urls)
        for future in as_completed(futures):
            if not future.result():
                return False

    return True


if __name__ == "__main__":
    # Parse arguments from command line
    parser = argparse.ArgumentParser(
        description="Download images from URLs to a specified directory."
    )
    parser.add_argument(
        "-d",
        "--directory",
        default="twemoji",
        type=str,
        help='Directory to save images. Defaults to "flags".',
    )
    parser.add_argument(
        "-t",
        "--threads",
        default=4,
        type=int,
        help="Number of threads to use for parallel downloads. Defaults to 4.",
    )
    args = parser.parse_args()

    directory = args.directory
    threads = args.threads

    urls = [line.strip() for line in sys.stdin if line.strip()]
    success = download_parallel(urls, directory, threads)

    if not success:
        sys.exit(1)
