Axelor Development Kit
======================

Axelor Development Kit (ADK) is an open source Java framework for business
application development.

This README describes how to get started quickly and prepare the development
environment for working with the sources.

Prerequisite
------------

* JDK 1.7
* PostgreSQL 9.x

We recommend Oracle JDK only. The OpenJDK distributed with most of the Linux
distributions may work but we have tested only with the Oracle JDK 1.7.

```bash
$ export JAVA_HOME=/path/to/jdk
$ export PATH=$JAVA_HOME/bin:$PATH
```

Install PostgreSQL from your linux distribution’s package repositories.

For ubuntu, you can do this:

```bash
$ sudo apt-get install postgresql
````

Installation
------------

[Download](https://github.com/axelor/axelor-development-kit/releases) the latest
distribution package and extract the package somewhere and set following
environment variables.

```bash
$ export AXELOR_HOME=/path/to/axelor-development-kit
$ export PATH=$AXELOR_HOME/bin:$PATH
```

or you can build from the latest source like this:

```bash
$ git clone https://github.com/axelor/axelor-development-kit.git
$ cd axelor-development-kit
$ ./gradlew installApp
```

```bash
$ export AXELOR_HOME=/path/to/axelor-development-kit/build/install/axelor-development-kit
$ export PATH=$AXELOR_HOME/bin:$PATH
```

You should have a special command `axelor' in your path now. Just try issuing
following command on the terminal:

```bash
$ axelor --help
```

You should see output something like this:

```
Usage: axelor [--help] [--new <NAME>]
Run the interactive shell or create a new axelor project.

  -h, --help          show this help and exit
  -v, --version       display version information
      --new <NAME>    create a new application project

You can also execute shell commands directly like:

  axelor help
  axelor help run
  axelor clean
  axelor build
  axelor run -p 8000

See detailed documentation at http://axelor.com/docs/adk.
```

The command line utility can also be used in interactive mode where the utility
runs in a special shell from where you can issue various commands.

What’s Next?
------------

Follow the [Quickstart Guide](http://axelor.com/docs/adk/quickstart/) for more
detailed introduction.

Links
-----

* [Axelor](http://axelor.com)
* [Documentation](http://axelor.com/docs/adk)
* [License](http://www.gnu.org/licenses/agpl.html)
