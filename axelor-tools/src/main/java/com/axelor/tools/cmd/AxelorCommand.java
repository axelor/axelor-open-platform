package com.axelor.tools.cmd;

import io.airlift.command.Option;
import io.airlift.command.OptionType;

public abstract class AxelorCommand implements Runnable {

	@Option(type = OptionType.GLOBAL, name = "-v", description = "Verbose mode")
	public boolean verbose;
}
