/*
 * Copyright (C) 2012 RoboVM AB
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
package aura.compiler.util.io;

import java.io.File;
import java.io.IOException;

import aura.compiler.config.Config;
import org.apache.commons.io.FileUtils;
import aura.compiler.CompilerException;
import aura.compiler.config.OS;

/**
 * Takes a file and compresses it via the <code>libhfscompressor.dylib</code> in
 * <code>robovm/bin</code>. Used on object files.</p>
 * 
 * Compression can be disabled completely by setting the <code>ROBOVM_DISABLE_COMPRESSION</code>
 * environment variable.
 * 
 * @author badlogic
 *
 */
public class HfsCompressor {

    public void compress(File file, byte[] data, Config config) throws IOException, InterruptedException {
        FileUtils.writeByteArrayToFile(file, data);
    }
}
