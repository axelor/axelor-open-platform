## {{app-name}}

This is an Axelor enterprise application that can be run using the 'axelor'
command-line tool located in the `bin/` directory.

### USAGE

```shell
$ axelor --help
Usage: axelor [-hV] [-c=FILE] [COMMAND]
  -c, --config=FILE   Path to axelor config file.
  -h, --help          Show this help message and exit.
  -V, --version       Print version information and exit.
Commands:
  run       Run the application.
  database  Perform database maintenance operations.
```

### EXAMPLES

- Start the application : `axelor run`
- Start with custom config file : `axelor -c /path/to/config.properties run`
- Show version : `axelor --version`
- Database operations : `axelor database`
- Show help : `axelor --help`

### REQUIREMENTS

- Java 21 or higher
- Properly configured database connection
- Valid axelor configuration file

For more information about Axelor, visit: https://axelor.com
