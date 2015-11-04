/*
 * Copyright (C) 2015 RoboVM AB
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
package aura.llvm;

import java.util.ArrayList;
import java.util.List;

import aura.llvm.binding.Attribute;
import aura.llvm.binding.LLVM;
import aura.llvm.binding.Linkage;
import aura.llvm.binding.ValueRef;

/**
 * 
 */
public class Function {
    private ValueRef ref;

    Function(ValueRef ref) {
        this.ref = ref;
    }

    protected ValueRef getRef() {
        return ref;
    }
    
    public String getName() {
        return LLVM.GetValueName(getRef());
    }
    
    public Linkage getLinkage() {
        return LLVM.GetLinkage(getRef());
    }

    public void setLinkage(Linkage linkage) {
        LLVM.SetLinkage(getRef(), linkage);
    }
    
    public Attribute[] getAttributes() {
        int mask = LLVM.GetFunctionAttr(getRef());
        List<Attribute> result = new ArrayList<>();
        for (Attribute a : Attribute.values()) {
            if ((a.swigValue() & mask) != 0) {
                result.add(a);
            }
        }
        return result.toArray(new Attribute[result.size()]);
    }
    
    public void addAttribute(Attribute attribute) {
        LLVM.AddFunctionAttr(getRef(), attribute.swigValue());
    }

    public void removeAttribute(Attribute attribute) {
        LLVM.RemoveFunctionAttr(getRef(), attribute.swigValue());
    }
}
