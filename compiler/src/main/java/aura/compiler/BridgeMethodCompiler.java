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
package aura.compiler;

import aura.compiler.config.Config;
import aura.compiler.llvm.*;
import aura.compiler.llvm.Icmp.Condition;
import aura.compiler.trampoline.Invokestatic;
import soot.LongType;
import soot.SootMethod;
import soot.tagkit.AnnotationTag;

import java.util.*;
import java.util.Map.Entry;

import static aura.compiler.Annotations.*;
import static aura.compiler.Functions.call;
import static aura.compiler.llvm.Linkage._private;

/**
 * @author niklas
 *
 */
public class BridgeMethodCompiler extends BroMethodCompiler {

    public BridgeMethodCompiler(Config config) {
        super(config);
    }

    private void validateBridgeMethod(SootMethod method) {
        if (!method.isNative()) {
            throw new IllegalArgumentException("@Bridge annotated method " 
                    + method + " must be native");
        }
        AnnotationTag bridgeAnnotation = getAnnotation(method, BRIDGE);
        if (readBooleanElem(bridgeAnnotation, "dynamic", false)) {
            if (!method.isStatic() || method.getParameterCount() == 0
                    || method.getParameterType(0) != LongType.v()
                    || !hasParameterAnnotation(method, 0, POINTER)) {
                throw new IllegalArgumentException("Dynamic @Bridge annotated method " 
                        + method + " must be static and take a @Pointer long as first parameter");
            }
        }
        if (hasVariadicAnnotation(method)) {
            int first = getVariadicParameterIndex(method);
            if (first < 0) {
                throw new IllegalArgumentException("Invalid @Variadic index on @Bridge annotated method " 
                        + method + ". Index must be 0 or greater.");
            }
            if (method.isStatic() && first == 0) {
                throw new IllegalArgumentException("At least 1 parameter must come " 
                        + "before the first va_arg parameter on static @Bridge annotated method " 
                        + method);
            }
            for (int i = first; i < method.getParameterCount(); i++) {
                if (Bro.isPassByValue(method, i)) {
                    throw new IllegalArgumentException("Parameter " + (i + 1) + " of @Bridge annotated method " 
                        + method + " cannot be passed @ByVal when part of a va_arg parameter list");
                }
            }
        }
    }

    protected static FunctionRef getBridgeCWrapperRef(FunctionType functionType, String name) {
        Type returnType = functionType.getReturnType();
        Type wrapperReturnType = returnType instanceof StructureType ? Type.VOID : returnType;
        List<Type> wrapperParamTypes = new ArrayList<>();
        wrapperParamTypes.add(Type.I8_PTR);
        if (returnType instanceof StructureType) {
            wrapperParamTypes.add(Type.I8_PTR);
        }
        for (Type t : functionType.getParameterTypes()) {
            if (t instanceof StructureType || t instanceof PointerType) {
                wrapperParamTypes.add(Type.I8_PTR);
            } else {
                wrapperParamTypes.add(t);
            }
        }
        FunctionType wrapperFnType = new FunctionType(wrapperReturnType,
                wrapperParamTypes.toArray(new Type[wrapperParamTypes.size()]));
        return new FunctionRef(name, wrapperFnType);
    }
    protected static String createBridgeCWrapper(Type returnType, Type[] hiParameterTypes, Type[] loParameterTypes, String name) {
        if (hiParameterTypes.length != loParameterTypes.length) {
            // Varargs
            if (hiParameterTypes.length < loParameterTypes.length) {
                throw new IllegalArgumentException("For va_arg functions lo's types parameter types must be a prefix of the hi type's parameters");
            }
            if (!Arrays.asList(hiParameterTypes).subList(0, loParameterTypes.length).equals(Arrays.asList(loParameterTypes))) {
                throw new IllegalArgumentException("For va_arg functions lo's types parameter types must be a prefix of the hi type's parameters");
            }
        } else {
            if (!Arrays.equals(hiParameterTypes, loParameterTypes)) {
                throw new IllegalArgumentException("hi and lo parameter types must be equal");
            }
        }
        
        // We order structs by name in reverse order. The names are constructed
        // such that nested structs get names which are naturally ordered after
        // their parent struct.
        Map<String, String> structs = new TreeMap<String, String>(Collections.reverseOrder());
        
        String hiReturnType = returnType instanceof StructureType 
                ? "void" : getHiType(returnType);

        StringBuilder hiSignature = new StringBuilder();
        hiSignature
            .append(hiReturnType)
            .append(' ')
            .append(name)
            .append("(void* target");
        if (returnType instanceof StructureType) {
            hiSignature.append(", void* ret");
        }

        StringBuilder loSignature = new StringBuilder();
        String loReturnType = getLoType(returnType, name, 0, structs);
        loSignature
            .append(loReturnType)
            .append(' ')
            .append("(*)(");
        
        StringBuilder body = new StringBuilder(" {\n");
        StringBuilder args = new StringBuilder();
        
        for (int i = 0; i < hiParameterTypes.length; i++) {
            String arg = "p" + i;
            if (i > 0) {
                args.append(", ");
            }

            String hiParamType = getHiType(hiParameterTypes[i]);
            hiSignature.append(", ");
            hiSignature.append(hiParamType).append(' ').append(arg);

            if (i < loParameterTypes.length) {
                String loParamType = getLoType(hiParameterTypes[i], name, i + 1, structs);
                if (i > 0) {
                    loSignature.append(", ");
                }
                loSignature.append(loParamType);
    
                if (hiParameterTypes[i] instanceof StructureType) {
                    args.append("*((").append(loParamType).append("*)").append(arg).append(')');
                } else {
                    args.append(arg);
                }
            } else {
                args.append(arg);
            }
        }
        hiSignature.append(')');
        if (loParameterTypes.length == 0) {
            loSignature.append("void");
        } else if (loParameterTypes.length < hiParameterTypes.length) {
            loSignature.append(", ...");
        }
        loSignature.append(')');
        
        for (Entry<String, String> struct : structs.entrySet()) {
            body.append("    struct " + struct.getKey() + " " + struct.getValue() + ";\n");
        }
        
        if (returnType instanceof StructureType) {
            body.append("    *((" + loReturnType + "*)ret) = ((" + loSignature + ") target)(" + args + ");\n");
        } else if (returnType != Type.VOID) {
            body.append("    return ((" + loSignature + ") target)(" + args + ");\n");
        } else {
            body.append("    ((" + loSignature + ") target)(" + args + ");\n");
        }
        body.append("}\n");

        return hiSignature.toString() + body.toString();
    }
    
    protected Function doCompile(ModuleBuilder moduleBuilder, SootMethod method) {
        validateBridgeMethod(method);

        AnnotationTag bridgeAnnotation = getAnnotation(method, BRIDGE);
        boolean dynamic = readBooleanElem(bridgeAnnotation, "dynamic", false);
        boolean optional = readBooleanElem(bridgeAnnotation, "optional", false);
        boolean useCWrapper = requiresCWrapper(method);
        
        Function fn = createMethodFunction(method);
        moduleBuilder.addFunction(fn);
        
        Type[] parameterTypes = fn.getType().getParameterTypes();
        String[] parameterNames = fn.getParameterNames();
        ArrayList<Argument> args = new ArrayList<Argument>();
        for (int i = 0; i < parameterTypes.length; i++) {
            args.add(new Argument(new VariableRef(parameterNames[i], parameterTypes[i])));
        }
        
        VariableRef env = fn.getParameterRef(0);
        
        // Load the address of the resolved @Bridge method
        Variable targetFn = fn.newVariable(Type.I8_PTR);
        if (!dynamic) {
            Global targetFnPtr = new Global(Symbols.bridgePtrSymbol(method), 
                    _private, new NullConstant(Type.I8_PTR));
            moduleBuilder.addGlobal(targetFnPtr);
            fn.add(new Load(targetFn, targetFnPtr.ref()));
    
            Label nullLabel = new Label();
            Label notNullLabel = new Label();
            Variable nullCheck = fn.newVariable(Type.I1);
            fn.add(new Icmp(nullCheck, Condition.eq, targetFn.ref(), new NullConstant(Type.I8_PTR)));
            fn.add(new Br(nullCheck.ref(), fn.newBasicBlockRef(nullLabel), fn.newBasicBlockRef(notNullLabel)));
            fn.newBasicBlock(nullLabel);
            Functions.call(fn, optional ? Functions.BC_THROW_UNSATISIFED_LINK_ERROR_OPTIONAL_BRIDGE_NOT_BOUND
                            : Functions.BC_THROW_UNSATISIFED_LINK_ERROR_BRIDGE_NOT_BOUND, env,
                    moduleBuilder.getString(className), moduleBuilder.getString(method.getName()),
                    moduleBuilder.getString(Types.getDescriptor(method)));
            fn.add(new Unreachable());
            fn.newBasicBlock(notNullLabel);
        } else {
            // Dynamic @Bridge methods pass the target function pointer as a
            // long in the first parameter.
            fn.add(new Inttoptr(targetFn, fn.getParameterRef(1), targetFn.getType()));
            args.remove(1);
        }
        
        // Marshal args
        
        // Remove Env* from args
        args.remove(0);

        // Save the Object->handle mapping for each marshaled object. We need it
        // after the native call to call updateObject() on the marshaler for 
        // each value. Since the LLVM variables that store these values are used 
        // after the native call we get the nice side effect that neither the
        // Java objects nor the handles can be garbage collected while we're in
        // native code.
        List<MarshaledArg> marshaledArgs = new ArrayList<MarshaledArg>();
        
        FunctionType targetFnType = getBridgeFunctionType(method, dynamic, false);
        Type[] targetParameterTypes = targetFnType.getParameterTypes();
        
        if (!method.isStatic()) {
            MarshalerLookup.MarshalerMethod marshalerMethod = config.getMarshalerLookup().findMarshalerMethod(new MarshalerLookup.MarshalSite(method, MarshalerLookup.MarshalSite.RECEIVER));
            MarshaledArg marshaledArg = new MarshaledArg();
            marshaledArg.paramIndex = MarshalerLookup.MarshalSite.RECEIVER;
            marshaledArgs.add(marshaledArg);
            Type nativeType = targetParameterTypes[0];
            Value nativeValue = marshalObjectToNative(fn, marshalerMethod, marshaledArg, 
                    useCWrapper ? Type.I8_PTR : nativeType, env, args.get(0).getValue(),
                    Bro.MarshalerFlags.CALL_TYPE_BRIDGE);
            args.set(0, new Argument(nativeValue));
        }
        
        for (int i = 0, argIdx = 0; i < method.getParameterCount(); i++) {
            if (dynamic && i == 0) {
                // Skip the target function pointer for dynamic bridge methods.
                continue;
            }
            if (!method.isStatic() && argIdx == 0) {
                // Skip the receiver in args. It doesn't correspond to a parameter.
                argIdx++;
            }
            soot.Type type = method.getParameterType(i);
            if (Bro.needsMarshaler(type)) {

                MarshalerLookup.MarshalerMethod marshalerMethod = config.getMarshalerLookup().findMarshalerMethod(new MarshalerLookup.MarshalSite(method, i));
                
                // The return type of the marshaler's toNative() method is derived from the target function type.
                Type nativeType = targetParameterTypes[argIdx];

                if (nativeType instanceof PrimitiveType) {
                    Value nativeValue = marshalValueObjectToNative(fn, marshalerMethod, nativeType, env, 
                            args.get(argIdx).getValue(), Bro.MarshalerFlags.CALL_TYPE_BRIDGE);
                    args.set(argIdx, new Argument(nativeValue));
                } else {
                    ParameterAttribute[] parameterAttributes = new ParameterAttribute[0];
                    if (Bro.isPassByValue(method, i) || Bro.isStructRet(method, i)) {
                        // The parameter must not be null. We assume that Structs 
                        // never have a NULL handle so we just check that the Java
                        // Object isn't null.
                        Functions.call(fn, Functions.CHECK_NULL, env, args.get(argIdx).getValue());
                    }
            
                    MarshaledArg marshaledArg = new MarshaledArg();
                    marshaledArg.paramIndex = i;
                    marshaledArgs.add(marshaledArg);
                    Value nativeValue = marshalObjectToNative(fn, marshalerMethod, marshaledArg, 
                            useCWrapper ? Type.I8_PTR : nativeType, env, args.get(argIdx).getValue(),
                            Bro.MarshalerFlags.CALL_TYPE_BRIDGE);
                    args.set(argIdx, new Argument(nativeValue, parameterAttributes));
                }
                
            } else {
                args.set(argIdx, new Argument(marshalPrimitiveToNative(fn, method, i, args.get(argIdx).getValue())));                    
            }
            
            argIdx++;
        }        
        
        Variable structResult = null;
        Value targetFnRef = null;
        
        if (useCWrapper) {
            args.add(0, new Argument(targetFn.ref()));

            if (targetFnType.getReturnType() instanceof StructureType) {
                // Allocate space on the stack big enough to hold the returned struct
                Variable tmp = fn.newVariable(new PointerType(targetFnType.getReturnType()));
                fn.add(new Alloca(tmp, targetFnType.getReturnType()));
                structResult = fn.newVariable(Type.I8_PTR);
                fn.add(new Bitcast(structResult, tmp.ref(), Type.I8_PTR));
                args.add(1, new Argument(structResult.ref()));
            }

            String wrapperName = Symbols.bridgeCSymbol(method);
            FunctionType wrapperFnType = getBridgeFunctionType(method, dynamic, true);
            getCWrapperFunctions().add(createBridgeCWrapper(targetFnType.getReturnType(), 
                    targetFnType.getParameterTypes(), wrapperFnType.getParameterTypes(), wrapperName));
            FunctionRef wrapperFnRef = getBridgeCWrapperRef(targetFnType, wrapperName);
            moduleBuilder.addFunctionDeclaration(new FunctionDeclaration(wrapperFnRef));
            targetFnRef = wrapperFnRef;
        } else {
            Variable tmp = fn.newVariable(targetFnType);
            fn.add(new Bitcast(tmp, targetFn.ref(), targetFnType));
            targetFnRef = tmp.ref();
        }
        
        // Execute the call to native code
        BasicBlockRef bbSuccess = fn.newBasicBlockRef(new Label("success"));
        BasicBlockRef bbFailure = fn.newBasicBlockRef(new Label("failure"));
        Functions.pushNativeFrame(fn);
        Functions.trycatchAllEnter(fn, env, bbSuccess, bbFailure);
        fn.newBasicBlock(bbSuccess.getLabel());
        Value result = Functions.callWithArguments(fn, targetFnRef, args);
        Functions.trycatchLeave(fn, env);
        Functions.popNativeFrame(fn);

        updateObject(method, fn, env, Bro.MarshalerFlags.CALL_TYPE_BRIDGE, marshaledArgs);
        
        // Marshal the return value
        if (Bro.needsMarshaler(method.getReturnType())) {
            MarshalerLookup.MarshalerMethod marshalerMethod = config.getMarshalerLookup().findMarshalerMethod(new MarshalerLookup.MarshalSite(method));
            String targetClassName = Types.getInternalName(method.getReturnType());
            
            if (structResult != null) {
                // Copy to the heap.
                DataLayout dataLayout = config.getDataLayout();
                Value heapCopy = Functions.call(fn, Functions.BC_COPY_STRUCT, env, structResult.ref(),
                        new IntegerConstant(dataLayout.getAllocSize(targetFnType.getReturnType())));
                result = marshalNativeToObject(fn, marshalerMethod, null, env, 
                        targetClassName, heapCopy, Bro.MarshalerFlags.CALL_TYPE_BRIDGE);
            } else if (targetFnType.getReturnType() instanceof PrimitiveType) {
                result = marshalNativeToValueObject(fn, marshalerMethod, env, 
                        targetClassName, result, Bro.MarshalerFlags.CALL_TYPE_BRIDGE);
            } else {
                result = marshalNativeToObject(fn, marshalerMethod, null, env, 
                        targetClassName, result, Bro.MarshalerFlags.CALL_TYPE_BRIDGE);
            }
        } else {
            result = marshalNativeToPrimitive(fn, method, result);
        }
        
        fn.add(new Ret(result));
        
        fn.newBasicBlock(bbFailure.getLabel());
        Functions.trycatchLeave(fn, env);
        Functions.popNativeFrame(fn);
        
        Value ex = call(fn, Functions.BC_EXCEPTION_CLEAR, env);
        
        // Call Marshaler.updateObject() for each object that was marshaled before
        // the call.
        updateObject(method, fn, env, Bro.MarshalerFlags.CALL_TYPE_BRIDGE, marshaledArgs);
        
        Functions.call(fn, Functions.BC_THROW, env, ex);
        fn.add(new Unreachable());
        
        return fn;
    }

    private void updateObject(SootMethod method, Function fn, Value env, long flags, List<MarshaledArg> marshaledArgs) {
        for (MarshaledArg value : marshaledArgs) {
            MarshalerLookup.MarshalerMethod marshalerMethod = config.getMarshalerLookup().findMarshalerMethod(new MarshalerLookup.MarshalSite(method, value.paramIndex));
            SootMethod afterMethod = ((MarshalerLookup.PointerMarshalerMethod) marshalerMethod).getAfterBridgeCallMethod();
            if (afterMethod != null) {
                Invokestatic invokestatic = new Invokestatic(
                        Types.getInternalName(method.getDeclaringClass()),
                        Types.getInternalName(afterMethod.getDeclaringClass()),
                        afterMethod.getName(),
                        Types.getDescriptor(afterMethod));
                trampolines.add(invokestatic);
                Functions.call(fn, invokestatic.getFunctionRef(),
                        env, value.object, value.handle,
                        new IntegerConstant(flags));
            }
        }
    }
}
