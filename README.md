Axelor Development Kit
======================

Axelor Development Kit (ADK) is an open source Java framework for business
application development.

This README describes how to get started quickly and prepare the development
environment for working with the sources.

Prerequisite
------------

* [JDK 1.7][uri_jdk]
* [PostgreSQL 9.x][uri_postgresql]

We recommend to use [Oracle JDK][uri_jdk]. The OpenJDK distributed with most of
the Linux distributions may work but we have not tested it extensively.

	$ export JAVA_HOME=/path/to/jdk
	$ export PATH=$JAVA_HOME/bin:$PATH

Install [PostgreSQL][uri_postgresql] from your Linux distribution’s package repositories.

For ubuntu, you can do this:

	$ sudo apt-get install postgresql

Installation
------------

Download the latest [distribution package](../../releases) or get the source from the repository.

	$ git clone https://github.com/axelor/axelor-development-kit.git

	$ cd /path/to/axelor-development-kit
	$ ./gradlew installDist

	$ export AXELOR_HOME=/path/to/axelor-development-kit/build/install/axelor-development-kit
	$ export PATH=$AXELOR_HOME/bin:$PATH

You should have a special command `axelor` in your path now. Just try issuing
following command on the terminal:

	$ axelor --help

You should see output something like this:

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
	
	See detailed documentation at http://docs.axelor.com/adk.

The command line utility can also be used in interactive mode where the utility
runs in a special shell from where you can issue various commands.

What’s Next?
------------

Please check the [documentation][uri_docs] for more detailed introduction.

Links
-----

* [Axelor][uri_axelor]
* [Documentation][uri_docs]
* [License][uri_license]

[uri_axelor]: http://www.axelor.com
[uri_docs]: http://docs.axelor.com/adk
[uri_license]: http://www.gnu.org/licenses/agpl.html
[uri_jdk]: http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html
[uri_postgresql]: http://www.postgresql.org/download/
