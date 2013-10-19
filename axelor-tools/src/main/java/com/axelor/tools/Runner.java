package com.axelor.tools;

import io.airlift.command.Cli;
import io.airlift.command.Cli.CliBuilder;
import io.airlift.command.Help;

import com.axelor.tools.cmd.ConfigCommand;
import com.axelor.tools.cmd.GenerateCommand;

public final class Runner {

	public static void main(String[] args) {

		CliBuilder<Runnable> builder = Cli.<Runnable>builder("axelor")
				.withDescription("axelor commands")
				.withDefaultCommand(Help.class)
				.withCommand(Help.class)
				.withCommand(ConfigCommand.class)
				.withCommand(GenerateCommand.class);

		Cli<Runnable> runner = builder.build();
		runner.parse(args).run();
	}
}
