package aura.compiler.config;

import org.apache.commons.cli.*;

/*
 * Copyright (C) 2016 Aura Project
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/gpl-2.0.html>.
 */
public abstract class ArgParser<T> {

    CommandLine cmd;
    Options options;
    String[] args;


    public ArgParser(Options options, String[] args) {
        this.options = options;
        this.args = args;
    }

    public void validate() throws ParseException {
        CommandLineParser parser = new DefaultParser();
        cmd = parser.parse( options, args);
    }

    public abstract void usage(Options options);

    public abstract T populateObject(T objectToPopulate);

    public String getObjectForArg(String arg) throws ParseException {
        validate();

        if (cmd.hasOption(arg)) {
            return cmd.getOptionValue(arg);
        }
        return null;
    }

    public CommandLine getCmd() {
        return cmd;
    }

    public void setCmd(CommandLine cmd) {
        this.cmd = cmd;
    }

    public Options getOptions() {
        return options;
    }

    public void setOptions(Options options) {
        this.options = options;
    }

    public String[] getArgs() {
        return args;
    }

    public void setArgs(String[] args) {
        this.args = args;
    }
}
