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
package aura.compiler.trampoline;

import aura.compiler.Types;
import aura.compiler.llvm.FunctionType;


/**
 *
 * @version $Id$
 */
public class New extends Trampoline {
    private static final long serialVersionUID = 1L;
    
    public New(String callingClass, String targetClass) {
        super(callingClass, targetClass);
    }

    @Override
    public FunctionType getFunctionType() {
        return new FunctionType(Types.OBJECT_PTR, Types.ENV_PTR);
    }
}
