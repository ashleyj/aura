/*
 * Copyright (C) 2013 RoboVM AB
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
package aura.compiler;

import aura.compiler.llvm.FunctionRef;
import aura.compiler.llvm.FunctionType;
import aura.compiler.llvm.Type;
import soot.SootFieldRef;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.jimple.DefinitionStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains intrinsic functions. These are functions that will replace calls
 * to specific methods in the runtime package to speed things up.
 */
public class Intrinsics {

    private static final Map<String, FunctionRef> SIMPLE_INTRINSICS;
    
    static {
        SIMPLE_INTRINSICS = new HashMap<String, FunctionRef>();
        SIMPLE_INTRINSICS.put("java/lang/Class/getSuperclass()Ljava/lang/Class;", 
                new FunctionRef("intrinsics.java_lang_Class_getSuperclass", 
                        new FunctionType(Types.OBJECT_PTR, Types.ENV_PTR, Types.OBJECT_PTR)));
        SIMPLE_INTRINSICS.put("java/lang/Class/getComponentType()Ljava/lang/Class;", 
                new FunctionRef("intrinsics.java_lang_Class_getComponentType", 
                        new FunctionType(Types.OBJECT_PTR, Types.ENV_PTR, Types.OBJECT_PTR)));
        SIMPLE_INTRINSICS.put("java/lang/Class/isArray()Z", 
                new FunctionRef("intrinsics.java_lang_Class_isArray", 
                        new FunctionType(Type.I8, Types.ENV_PTR, Types.OBJECT_PTR)));
        SIMPLE_INTRINSICS.put("java/lang/Class/isPrimitive()Z", 
                new FunctionRef("intrinsics.java_lang_Class_isPrimitive", 
                        new FunctionType(Type.I8, Types.ENV_PTR, Types.OBJECT_PTR)));
        SIMPLE_INTRINSICS.put("java/lang/Object/getClass()Ljava/lang/Class;", 
                new FunctionRef("intrinsics.java_lang_Object_getClass", 
                        new FunctionType(Types.OBJECT_PTR, Types.ENV_PTR, Types.OBJECT_PTR)));
        SIMPLE_INTRINSICS.put("java/lang/Math/abs(F)F", 
                new FunctionRef("intrinsics.java_lang_Math_abs_F", 
                        new FunctionType(Type.FLOAT, Types.ENV_PTR, Type.FLOAT)));
        SIMPLE_INTRINSICS.put("java/lang/Math/abs(D)D", 
                new FunctionRef("intrinsics.java_lang_Math_abs_D", 
                        new FunctionType(Type.DOUBLE, Types.ENV_PTR, Type.DOUBLE)));
        SIMPLE_INTRINSICS.put("java/lang/Math/sqrt(D)D", 
                new FunctionRef("intrinsics.java_lang_Math_sqrt", 
                        new FunctionType(Type.DOUBLE, Types.ENV_PTR, Type.DOUBLE)));
        SIMPLE_INTRINSICS.put("java/lang/Math/cos(D)D", 
                new FunctionRef("intrinsics.java_lang_Math_cos", 
                        new FunctionType(Type.DOUBLE, Types.ENV_PTR, Type.DOUBLE)));
        SIMPLE_INTRINSICS.put("java/lang/Math/sin(D)D", 
                new FunctionRef("intrinsics.java_lang_Math_sin", 
                        new FunctionType(Type.DOUBLE, Types.ENV_PTR, Type.DOUBLE)));
    }
    
    private static final FunctionRef LDC_PRIM_Z = new FunctionRef("intrinsics.ldc_prim_Z", new FunctionType(Types.OBJECT_PTR, Types.ENV_PTR));
    private static final FunctionRef LDC_PRIM_B = new FunctionRef("intrinsics.ldc_prim_B", new FunctionType(Types.OBJECT_PTR, Types.ENV_PTR));
    private static final FunctionRef LDC_PRIM_C = new FunctionRef("intrinsics.ldc_prim_C", new FunctionType(Types.OBJECT_PTR, Types.ENV_PTR));
    private static final FunctionRef LDC_PRIM_S = new FunctionRef("intrinsics.ldc_prim_S", new FunctionType(Types.OBJECT_PTR, Types.ENV_PTR));
    private static final FunctionRef LDC_PRIM_I = new FunctionRef("intrinsics.ldc_prim_I", new FunctionType(Types.OBJECT_PTR, Types.ENV_PTR));
    private static final FunctionRef LDC_PRIM_J = new FunctionRef("intrinsics.ldc_prim_J", new FunctionType(Types.OBJECT_PTR, Types.ENV_PTR));
    private static final FunctionRef LDC_PRIM_F = new FunctionRef("intrinsics.ldc_prim_F", new FunctionType(Types.OBJECT_PTR, Types.ENV_PTR));
    private static final FunctionRef LDC_PRIM_D = new FunctionRef("intrinsics.ldc_prim_D", new FunctionType(Types.OBJECT_PTR, Types.ENV_PTR));
    
    public static FunctionRef getIntrinsic(SootMethod currMethod, Stmt stmt, InvokeExpr expr) {
        SootMethodRef methodRef = expr.getMethodRef();
        FunctionRef fref = SIMPLE_INTRINSICS.get(Types.getInternalName(methodRef.declaringClass()) + "/"
                                + methodRef.name() + Types.getDescriptor(methodRef));
        if (fref != null) {
            return fref;
        }
        
        if (methodRef.name().startsWith("memmove") 
                && "VM".equals(methodRef.declaringClass().getName())) {
            
            return new FunctionRef("intrinsics.aura_rt_VM_" + methodRef.name(),
                    new FunctionType(Type.VOID, Types.ENV_PTR, Type.I64, Type.I64, Type.I64));
        }

        if ("arraycopy".equals(methodRef.name()) 
                && "java.lang.System".equals(methodRef.declaringClass().getName())
                && "_getChars".equals(currMethod.getName())
                && "java.lang.String".equals(currMethod.getDeclaringClass().getName())) {
            
            return new FunctionRef("intrinsics.java_lang_System_arraycopy_C", 
                    new FunctionType(Type.VOID, Types.ENV_PTR, Types.OBJECT_PTR, Type.I32, Types.OBJECT_PTR, Type.I32, Type.I32));
        }

        return null;
    }

    public static FunctionRef getIntrinsic(SootMethod currMethod, DefinitionStmt stmt) {
        soot.Value rightOp = stmt.getRightOp();
        if (rightOp instanceof StaticFieldRef) {
            SootFieldRef fieldRef = ((StaticFieldRef) rightOp).getFieldRef();
            if ("TYPE".equals(fieldRef.name())) {
                String declClass = fieldRef.declaringClass().getName();
                if ("java.lang.Boolean".equals(declClass)) {
                    return LDC_PRIM_Z;
                }
                if ("java.lang.Byte".equals(declClass)) {
                    return LDC_PRIM_B;
                }
                if ("java.lang.Character".equals(declClass)) {
                    return LDC_PRIM_C;
                }
                if ("java.lang.Short".equals(declClass)) {
                    return LDC_PRIM_S;
                }
                if ("java.lang.Integer".equals(declClass)) {
                    return LDC_PRIM_I;
                }
                if ("java.lang.Long".equals(declClass)) {
                    return LDC_PRIM_J;
                }
                if ("java.lang.Float".equals(declClass)) {
                    return LDC_PRIM_F;
                }
                if ("java.lang.Double".equals(declClass)) {
                    return LDC_PRIM_D;
                }
            }
        }
        
        return null;
    }

}
