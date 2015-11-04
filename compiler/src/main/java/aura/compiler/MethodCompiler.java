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

import static aura.compiler.Functions.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import aura.compiler.config.Config;
import aura.compiler.llvm.*;
import aura.compiler.llvm.Invoke;
import aura.compiler.llvm.Add;
import aura.compiler.llvm.AliasRef;
import aura.compiler.llvm.Alloca;
import aura.compiler.llvm.And;
import aura.compiler.llvm.ArrayType;
import aura.compiler.llvm.Ashr;
import aura.compiler.llvm.BasicBlock;
import aura.compiler.llvm.BasicBlockRef;
import aura.compiler.llvm.Bitcast;
import aura.compiler.llvm.Br;
import aura.compiler.llvm.Call;
import aura.compiler.llvm.Constant;
import aura.compiler.llvm.ConstantBitcast;
import aura.compiler.llvm.ConstantTrunc;
import aura.compiler.llvm.Fadd;
import aura.compiler.llvm.Fdiv;
import aura.compiler.llvm.FloatingPointConstant;
import aura.compiler.llvm.FloatingPointType;
import aura.compiler.llvm.Fmul;
import aura.compiler.llvm.Fpext;
import aura.compiler.llvm.Fptrunc;
import aura.compiler.llvm.Fsub;
import aura.compiler.llvm.Function;
import aura.compiler.llvm.FunctionRef;
import aura.compiler.llvm.FunctionType;
import aura.compiler.llvm.Getelementptr;
import aura.compiler.llvm.Global;
import aura.compiler.llvm.GlobalRef;
import aura.compiler.llvm.Icmp;
import aura.compiler.llvm.Instruction;
import aura.compiler.llvm.IntegerConstant;
import aura.compiler.llvm.IntegerType;
import aura.compiler.llvm.Label;
import aura.compiler.llvm.Load;
import aura.compiler.llvm.Lshr;
import aura.compiler.llvm.Mul;
import aura.compiler.llvm.NullConstant;
import aura.compiler.llvm.Or;
import aura.compiler.llvm.PointerType;
import aura.compiler.llvm.Ret;
import aura.compiler.llvm.Sext;
import aura.compiler.llvm.Shl;
import aura.compiler.llvm.Sitofp;
import aura.compiler.llvm.Store;
import aura.compiler.llvm.StructureConstantBuilder;
import aura.compiler.llvm.Sub;
import aura.compiler.llvm.Switch;
import aura.compiler.llvm.Trunc;
import aura.compiler.llvm.Type;
import aura.compiler.llvm.Unreachable;
import aura.compiler.llvm.Value;
import aura.compiler.llvm.Variable;
import aura.compiler.llvm.VariableRef;
import aura.compiler.llvm.Xor;
import aura.compiler.llvm.Zext;
import aura.compiler.trampoline.Anewarray;
import aura.compiler.trampoline.Checkcast;
import aura.compiler.trampoline.GetField;
import aura.compiler.trampoline.GetStatic;
import aura.compiler.trampoline.Instanceof;
import aura.compiler.trampoline.Invokeinterface;
import aura.compiler.trampoline.Invokespecial;
import aura.compiler.trampoline.Invokestatic;
import aura.compiler.trampoline.Invokevirtual;
import aura.compiler.trampoline.LdcClass;
import aura.compiler.trampoline.Multianewarray;
import aura.compiler.trampoline.New;
import aura.compiler.trampoline.PutField;
import aura.compiler.trampoline.PutStatic;
import aura.compiler.trampoline.Trampoline;

import soot.Body;
import soot.CharType;
import soot.Immediate;
import soot.Local;
import soot.Modifier;
import soot.NullType;
import soot.PackManager;
import soot.PatchingChain;
import soot.PrimType;
import soot.RefLikeType;
import soot.SootClass;
import soot.SootField;
import soot.SootFieldRef;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Trap;
import soot.Unit;
import soot.UnitBox;
import soot.jimple.AddExpr;
import soot.jimple.AndExpr;
import soot.jimple.ArrayRef;
import soot.jimple.BinopExpr;
import soot.jimple.CastExpr;
import soot.jimple.CaughtExceptionRef;
import soot.jimple.CmpExpr;
import soot.jimple.CmpgExpr;
import soot.jimple.CmplExpr;
import soot.jimple.ConditionExpr;
import soot.jimple.DefinitionStmt;
import soot.jimple.DivExpr;
import soot.jimple.EnterMonitorStmt;
import soot.jimple.EqExpr;
import soot.jimple.ExitMonitorStmt;
import soot.jimple.Expr;
import soot.jimple.FieldRef;
import soot.jimple.GeExpr;
import soot.jimple.GotoStmt;
import soot.jimple.GtExpr;
import soot.jimple.IfStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InstanceOfExpr;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.LeExpr;
import soot.jimple.LengthExpr;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.LtExpr;
import soot.jimple.MulExpr;
import soot.jimple.NeExpr;
import soot.jimple.NegExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.NopStmt;
import soot.jimple.OrExpr;
import soot.jimple.ParameterRef;
import soot.jimple.RemExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.ShlExpr;
import soot.jimple.ShrExpr;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticFieldRef;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.SubExpr;
import soot.jimple.TableSwitchStmt;
import soot.jimple.ThisRef;
import soot.jimple.ThrowStmt;
import soot.jimple.UshrExpr;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.XorExpr;
import soot.jimple.toolkits.annotation.tags.ArrayCheckTag;
import soot.jimple.toolkits.annotation.tags.NullCheckTag;
import soot.tagkit.DoubleConstantValueTag;
import soot.tagkit.FloatConstantValueTag;
import soot.tagkit.IntegerConstantValueTag;
import soot.tagkit.LongConstantValueTag;
import soot.tagkit.StringConstantValueTag;
import soot.tagkit.Tag;
import soot.util.Chain;

/**
 *
 * @version $Id$
 */
public class MethodCompiler extends AbstractMethodCompiler {

    private Function function;
    private Map<Unit, List<Trap>> trapsAt;
    private Value env;
    private ModuleBuilder moduleBuilder;
    
    private Variable dims;
    
    public MethodCompiler(Config config) {
        super(config);
    }
    
    protected Function doCompile(ModuleBuilder moduleBuilder, SootMethod method) {
        function = createMethodFunction(method);
        moduleBuilder.addFunction(function);
        this.moduleBuilder = moduleBuilder;
        
        env = function.getParameterRef(0);

        trapsAt = new HashMap<Unit, List<Trap>>();
        
        Body body = method.retrieveActiveBody();
        
        NopStmt prependedNop = null;
        if (method.isStatic() && !body.getUnits().getFirst().getBoxesPointingToThis().isEmpty()) {
            // Fix for issue #1. This prevents an NPE in Soot's ArrayBoundsCheckerAnalysis. The NPE
            // occurs for static methods which start with a unit that is the target of some other
            // unit. We work around this by inserting a nop statement as the first unit in such 
            // methods. See http://www.sable.mcgill.ca/listarchives/soot-list/msg01397.html.
            Unit insertionPoint = body.getUnits().getFirst();
            prependedNop = Jimple.v().newNopStmt();
            body.getUnits().getNonPatchingChain().insertBefore(prependedNop, insertionPoint);
        }
        
        PackManager.v().getPack("jtp").apply(body);
        PackManager.v().getPack("jop").apply(body);
        PackManager.v().getPack("jap").apply(body);

        if (body.getUnits().getFirst() == prependedNop && prependedNop.getBoxesPointingToThis().isEmpty()) {
            // Remove the nop we inserted above to work around the bug in Soot's 
            // ArrayBoundsCheckerAnalysis which has now been run.
            body.getUnits().getNonPatchingChain().removeFirst();
        }
        
        PatchingChain<Unit> units = body.getUnits();
        Map<Unit, List<Unit>> branchTargets = getBranchTargets(body);
        Map<Unit, Integer> trapHandlers = getTrapHandlers(body);
        Map<Unit, Integer> selChanges = new HashMap<Unit, Integer>();
        
        int multiANewArrayMaxDims = 0;
        Set<Local> locals = new HashSet<Local>();
        boolean emitCheckStackOverflow = false;
        for (Unit unit : units) {
            if (unit instanceof DefinitionStmt) {
                DefinitionStmt stmt = (DefinitionStmt) unit;
                if (stmt.getLeftOp() instanceof Local) {
                    Local local = (Local) stmt.getLeftOp();
                    if (!locals.contains(local)) {
                        Type type = Types.getLocalType(local.getType());
                        Alloca alloca = new Alloca(function.newVariable(local.getName(), type), type);
                        alloca.attach(local);
                        function.add(alloca);
                        locals.add(local);
                    }
                }
                if (stmt.getRightOp() instanceof NewMultiArrayExpr) {
                    NewMultiArrayExpr expr = (NewMultiArrayExpr) stmt.getRightOp();
                    multiANewArrayMaxDims = Math.max(multiANewArrayMaxDims, expr.getSizeCount());
                }
                if (stmt.getRightOp() instanceof InvokeExpr) {
                	emitCheckStackOverflow = true;
                }
            }
            if (unit instanceof InvokeStmt) {
            	emitCheckStackOverflow = true;
            }
        }
        
        dims = null;
        if (multiANewArrayMaxDims > 0) {
            dims = function.newVariable("dims", new PointerType(new ArrayType(multiANewArrayMaxDims, Type.I32)));
            function.add(new Alloca(dims, new ArrayType(multiANewArrayMaxDims, Type.I32)));
        }
        
        if (emitCheckStackOverflow) {
            call(CHECK_STACK_OVERFLOW);
        }
        
        Value trycatchContext = null;
        if (!body.getTraps().isEmpty()) {
            List<List<Trap>> recordedTraps = new ArrayList<List<Trap>>();
            for (Unit unit : units) {
                // Calculate the predecessor units of unit 
                Set<Unit> incoming = new HashSet<Unit>();
                if (units.getFirst() != unit && units.getPredOf(unit).fallsThrough()) {
                    incoming.add(units.getPredOf(unit));
                }
                if (branchTargets.keySet().contains(unit)) {
                    incoming.addAll(branchTargets.get(unit));
                }
                
                if (unit == units.getFirst() || trapHandlers.containsKey(unit) 
                        || trapsDiffer(unit, incoming)) {
                    
                    List<Trap> traps = getTrapsAt(unit);
                    if (traps.isEmpty()) {
                        selChanges.put(unit, 0);
                    } else {
                        int index = recordedTraps.indexOf(traps);
                        if (index == -1) {
                            index = recordedTraps.size();
                            recordedTraps.add(traps);
                        }
                        selChanges.put(unit, index + 1);
                    }
                }
            }
            
            StructureConstantBuilder landingPadsPtrs = new StructureConstantBuilder();
            for (List<Trap> traps : recordedTraps) {
                StructureConstantBuilder landingPads = new StructureConstantBuilder();
                for (Trap trap : traps) {
                    SootClass exClass = trap.getException();
                    StructureConstantBuilder landingPad = new StructureConstantBuilder();
                    if ("java.lang.Throwable".equals(exClass.getName()) || exClass.isPhantom()) {
                        landingPad.add(new NullConstant(Type.I8_PTR));
                    } else {
                        catches.add(Types.getInternalName(exClass));
                        if (exClass == sootClass) {
                            /*
                             * The class being compiled is an exception class
                             * with a catch clause which catches itself. We
                             * cannot reference the info struct directly since
                             * we don't know the type of it and it hasn't been
                             * emitted by ClassCompiler yet. Use the internal
                             * i8* alias instead which ClassCompiler will emit.
                             * See #1007.
                             */
                            landingPad.add(new AliasRef(Symbols.infoStructSymbol(Types.getInternalName(exClass)) + "_i8ptr",
                                    Type.I8_PTR));
                        } else {
                            Global g = new Global(Symbols.infoStructSymbol(Types.getInternalName(exClass)), Type.I8_PTR, true);
                            if (!moduleBuilder.hasSymbol(g.getName())) {
                                moduleBuilder.addGlobal(g);
                            }
                            landingPad.add(g.ref());
                        }
                    }
                    landingPad.add(new IntegerConstant(trapHandlers.get(trap.getHandlerUnit()) + 1));
                    landingPads.add(landingPad.build());
                }
                landingPads.add(new StructureConstantBuilder().add(new NullConstant(Type.I8_PTR)).add(new IntegerConstant(0)).build());
                Global g = moduleBuilder.newGlobal(landingPads.build(), true);
                landingPadsPtrs.add(new ConstantBitcast(g.ref(), Type.I8_PTR));
            }
            Global g = moduleBuilder.newGlobal(landingPadsPtrs.build(), true);
            Variable ctx = function.newVariable(Types.TRYCATCH_CONTEXT_PTR);
            Variable bcCtx = function.newVariable(Types.BC_TRYCATCH_CONTEXT_PTR);
            function.add(new Alloca(bcCtx, Types.BC_TRYCATCH_CONTEXT));
            Variable selPtr = function.newVariable(new PointerType(Type.I32));
            function.add(new Getelementptr(selPtr, bcCtx.ref(), 0, 0, 1));
            function.add(new Store(new IntegerConstant(0), selPtr.ref()));        
            Variable bcCtxLandingPadsPtr = function.newVariable(Type.I8_PTR_PTR);
            function.add(new Getelementptr(bcCtxLandingPadsPtr, bcCtx.ref(), 0, 1));
            function.add(new Store(new ConstantBitcast(g.ref(), Type.I8_PTR), bcCtxLandingPadsPtr.ref()));
            function.add(new Bitcast(ctx, bcCtx.ref(), Types.TRYCATCH_CONTEXT_PTR));
            trycatchContext = ctx.ref();
            Value result = call(RVM_TRYCATCH_ENTER, env, trycatchContext);
            Map<IntegerConstant, BasicBlockRef> alt = new TreeMap<IntegerConstant, BasicBlockRef>();
            for (Entry<Unit, Integer> entry : trapHandlers.entrySet()) {
                alt.put(new IntegerConstant(entry.getValue() + 1), function.newBasicBlockRef(new Label(entry.getKey())));
            }
            function.add(new Switch(result, function.newBasicBlockRef(new Label(units.getFirst())), alt));
            if (!branchTargets.containsKey(units.getFirst())) {
                function.newBasicBlock(new Label(units.getFirst()));
            }
        }
        
        if ("<clinit>".equals(method.getName())) {
            initializeClassFields();
        }
        
        for (Unit unit : units) {
            if (branchTargets.containsKey(unit) || trapHandlers.containsKey(unit)) {
                BasicBlock oldBlock = function.getCurrentBasicBlock();
                function.newBasicBlock(new Label(unit));
                if (oldBlock != null) {
                    Instruction last = oldBlock.last();
                    if (last == null || !isTerminator(last)) {
                        oldBlock.add(new Br(function.newBasicBlockRef(new Label(unit))));
                    }
                }
            }
            
            if (selChanges.containsKey(unit)) {
                int sel = selChanges.get(unit);
                // trycatchContext->sel = sel
                Variable selPtr = function.newVariable(new PointerType(Type.I32));
                function.add(new Getelementptr(selPtr, trycatchContext, 0, 1)).attach(unit);
                function.add(new Store(new IntegerConstant(sel), selPtr.ref())).attach(unit);
            }
            
            if (unit instanceof DefinitionStmt) {
                assign((DefinitionStmt) unit);
            } else if (unit instanceof ReturnStmt) {
                if (!body.getTraps().isEmpty()) {
                    trycatchLeave(function);
                }
                return_((ReturnStmt) unit);
            } else if (unit instanceof ReturnVoidStmt) {
                if (!body.getTraps().isEmpty()) {
                    trycatchLeave(function);
                }
                returnVoid((ReturnVoidStmt) unit);
            } else if (unit instanceof IfStmt) {
                if_((IfStmt) unit);
            } else if (unit instanceof LookupSwitchStmt) {
                lookupSwitch((LookupSwitchStmt) unit);
            } else if (unit instanceof TableSwitchStmt) {
                tableSwitch((TableSwitchStmt) unit);
            } else if (unit instanceof GotoStmt) {
                goto_((GotoStmt) unit);
            } else if (unit instanceof ThrowStmt) {
                throw_((ThrowStmt) unit);
            } else if (unit instanceof InvokeStmt) {
                invoke((InvokeStmt) unit);
            } else if (unit instanceof EnterMonitorStmt) {
                enterMonitor((EnterMonitorStmt) unit);
            } else if (unit instanceof ExitMonitorStmt) {
                exitMonitor((ExitMonitorStmt) unit);
            } else if (unit instanceof NopStmt) {
                nop((NopStmt) unit);
            } else {
                throw new IllegalArgumentException("Unknown Unit type: " + unit.getClass());
            }
        }
        
        if (this.className.equals("java/lang/Object") && "<init>".equals(method.getName())) {
            // Compile Object.<init>(). JLS 12.6.1: "An object o is not finalizable until its constructor has invoked 
            // the constructor for Object on o and that invocation has completed successfully".
            // Object.<init>() calls register_finalizable() in header.ll which checks if the class of 'this' is finalizable.
            // If it is the object will be registered for finalization.
            for (BasicBlock bb : function.getBasicBlocks()) {
                if (bb.last() instanceof Ret) {
                    // Insert a call to register_finalizable() before this ret
                    Call call = new Call(REGISTER_FINALIZABLE, env, function.getParameterRef(1));
                    call.attach(bb.last().getAttachment(Unit.class));
                    bb.insertBefore(bb.last(), call);
                }
            }
        }

        return function;
    }
    
    /**
     * Returns <code>true</code> if the {@link Trap}s at {@link Unit} <code>unit</code>
     * differ from any of those at the {@link Unit}s that branch to <code>unit</code>.
     */
    private boolean trapsDiffer(Unit unit, Collection<Unit> incomingUnits) {
        List<Trap> traps = getTrapsAt(unit);
        for (Unit incomingUnit : incomingUnits) {
            if (!traps.equals(getTrapsAt(incomingUnit))) {
                return true;
            }
        }
        return false;
    }
    
    private Map<Unit, List<Unit>> getBranchTargets(Body body) {
        Map<Unit, List<Unit>> result = new HashMap<Unit, List<Unit>>();
        for (Unit unit : body.getUnits()) {
            if (unit.branches()) {
                List<Unit> targetUnits = new ArrayList<Unit>();
                for (UnitBox ub : unit.getUnitBoxes()) {
                    targetUnits.add(ub.getUnit());
                }
                if (unit.fallsThrough()) {
                    targetUnits.add(body.getUnits().getSuccOf(unit));
                }
                for (Unit targetUnit : targetUnits) {
                    List<Unit> sourceUnits = result.get(targetUnit);
                    if (sourceUnits == null) {
                        sourceUnits = new ArrayList<Unit>();
                        result.put(targetUnit, sourceUnits);
                    }
                    sourceUnits.add(unit);
                }
            }
        }
        return result;
    }
    
    private Map<Unit, Integer> getTrapHandlers(Body body) {
        Map<Unit, Integer> trapHandlers = new HashMap<Unit, Integer>();
        for (Trap trap : body.getTraps()) {
            Unit beginUnit = trap.getBeginUnit();
            Unit endUnit = trap.getEndUnit();
            if (beginUnit != endUnit && !trapHandlers.containsKey(trap.getHandlerUnit())) {
                trapHandlers.put(trap.getHandlerUnit(), trapHandlers.size());
            }
        }
        return trapHandlers;
    }
        
    private void initializeClassFields() {
        for (SootField field : sootMethod.getDeclaringClass().getFields()) {
            if (!field.isStatic()) {
                continue;
            }
            for (Tag tag : field.getTags()) {
                Value value = null;
                if (tag instanceof DoubleConstantValueTag) {
                    DoubleConstantValueTag dtag = (DoubleConstantValueTag) tag;
                    value = new FloatingPointConstant(dtag.getDoubleValue());
                } else if (tag instanceof FloatConstantValueTag) {
                    FloatConstantValueTag ftag = (FloatConstantValueTag) tag;
                    value = new FloatingPointConstant(ftag.getFloatValue());
                } else if (tag instanceof IntegerConstantValueTag) {
                    IntegerConstantValueTag itag = (IntegerConstantValueTag) tag;
                    value = new IntegerConstant(itag.getIntValue());
                    IntegerType type = (IntegerType) Types.getType(field.getType());
                    if (type.getBits() < 32) {
                        value = new ConstantTrunc((Constant) value, type);
                    }
                } else if (tag instanceof LongConstantValueTag) {
                    LongConstantValueTag ltag = (LongConstantValueTag) tag;
                    value = new IntegerConstant(ltag.getLongValue());
                } else if (tag instanceof StringConstantValueTag) {
                    String s = ((StringConstantValueTag) tag).getStringValue();
                    value = call(ldcString(s), env);
                }
                
                if (value != null) {
                    FunctionRef fn = FunctionBuilder.setter(field).ref();
                    call(fn, env, value);
                }
            }
        }
    }

    
    private static boolean isTerminator(Instruction instr) {
        return instr instanceof Ret || instr instanceof Br 
            || instr instanceof Invoke || instr instanceof Unreachable 
            || instr instanceof Switch;
    }

    private Value immediate(Unit unit, Immediate v) {
        // v is either a soot.Local or a soot.jimple.Constant
        if (v instanceof soot.Local) {
            Local local = (Local) v;
            Type type = Types.getLocalType(v.getType());
            VariableRef var = new VariableRef(local.getName(), new PointerType(type));
            Variable tmp = function.newVariable(type);
            function.add(new Load(tmp, var, !sootMethod.getActiveBody().getTraps().isEmpty())).attach(unit);
            return new VariableRef(tmp);
        } else if (v instanceof soot.jimple.IntConstant) {
            return new IntegerConstant(((soot.jimple.IntConstant) v).value);
        } else if (v instanceof soot.jimple.LongConstant) {
            return new IntegerConstant(((soot.jimple.LongConstant) v).value);
        } else if (v instanceof soot.jimple.FloatConstant) {
            return new FloatingPointConstant(((soot.jimple.FloatConstant) v).value);
        } else if (v instanceof soot.jimple.DoubleConstant) {
            return new FloatingPointConstant(((soot.jimple.DoubleConstant) v).value);
        } else if (v instanceof soot.jimple.NullConstant) {
            return new NullConstant(Types.OBJECT_PTR);
        } else if (v instanceof soot.jimple.StringConstant) {
            String s = ((soot.jimple.StringConstant) v).value;
            return call(unit, ldcString(s), env);
        } else if (v instanceof soot.jimple.ClassConstant) {
            // ClassConstant is either the internal name of a class or the descriptor of an array
            String targetClassName = ((soot.jimple.ClassConstant) v).getValue();
            if (Types.isArray(targetClassName) && Types.isPrimitiveComponentType(targetClassName)) {
                String primitiveDesc = targetClassName.substring(1);
                Variable result = function.newVariable(Types.OBJECT_PTR);
                function.add(new Load(result, new ConstantBitcast(
                        new GlobalRef("array_" + primitiveDesc, Types.CLASS_PTR), new PointerType(Types.OBJECT_PTR)))).attach(unit);
                return result.ref();
            } else {
                FunctionRef fn = null;
                if (targetClassName.equals(this.className)) {
                    fn = FunctionBuilder.ldcInternal(sootMethod.getDeclaringClass()).ref();
                } else {
                    Trampoline trampoline = new LdcClass(className, ((soot.jimple.ClassConstant) v).getValue());
                    trampolines.add(trampoline);
                    fn = trampoline.getFunctionRef();
                }
                return call(unit, fn, env);
            }
        }
        throw new IllegalArgumentException("Unknown Immediate type: " + v.getClass());
    }

    private FunctionRef ldcString(String s) {
        byte[] modUtf8 = Strings.stringToModifiedUtf8Z(s);
        FunctionRef fref = new FunctionRef(Symbols.ldcStringSymbol(modUtf8), new FunctionType(Types.OBJECT_PTR, Types.ENV_PTR));
        if (moduleBuilder.hasSymbol(fref.getName())) {
            return fref;
        }
        Global g = new Global(Symbols.ldcStringPtrSymbol(modUtf8), Linkage.weak, new NullConstant(Types.OBJECT_PTR));
        moduleBuilder.addGlobal(g);
        Function f = new FunctionBuilder(fref).linkage(Linkage.weak).build();
        moduleBuilder.addFunction(f);
        Value result = Functions.call(f, BC_LDC_STRING, f.getParameterRef(0), g.ref(),
                moduleBuilder.getString(s), new IntegerConstant(s.length()));
        f.add(new Ret(result));
        return fref;
    }

    private Value widenToI32Value(Unit unit, Value value, boolean unsigned) {
        Type type = value.getType();
        if (type instanceof IntegerType && ((IntegerType) type).getBits() < 32) {
            Variable t = function.newVariable(Type.I32);
            if (unsigned) {
                function.add(new Zext(t, value, Type.I32)).attach(unit);
            } else {
                function.add(new Sext(t, value, Type.I32)).attach(unit);
            }
            return t.ref();
        } else {
            return value;
        }
    }
    
    private Value narrowFromI32Value(Unit unit, Type type, Value value) {
        if (value.getType() == Type.I32 && ((IntegerType) type).getBits() < 32) {
            Variable t = function.newVariable(type);
            function.add(new Trunc(t, value, type)).attach(unit);
            value = t.ref();
        }
        return value;
    }
    
    private Value call(Value fn, Value ... args) {
        return call(null, fn, args);
    }
    
    private Value call(Unit unit, Value fn, Value ... args) {
        Variable result = null;
        Type returnType = ((FunctionType) fn.getType()).getReturnType();
        if (returnType != Type.VOID) {
            result = function.newVariable(returnType);
        }
        function.add(new Call(result, fn, args)).attach(unit);
        return result == null ? null : result.ref();
    }
    
//    private Value callOrInvoke(Unit unit, Value fn, Value ... args) {
//        Variable result = null;
//        Type returnType = ((FunctionType) fn.getType()).getReturnType();
//        if (returnType != VOID) {
//            result = this.function.newVariable(returnType);
//        }
//        List<Trap> traps = getTrapsAt(unit);
//        if (!traps.isEmpty()) {
//            Label label = new Label();
//            BasicBlockRef to = function.newBasicBlockRef(label);
//            BasicBlockRef unwind = function.newBasicBlockRef(new Label(traps));
//            function.add(new Invoke(result, fn, to, unwind, args));
//            function.newBasicBlock(label);
//            recordedTraps.add(traps);
//        } else {
//            function.add(new Call(result, fn, args));
//        }
//        return result == null ? null : result.ref();
//    }
    
    private boolean canAccessDirectly(FieldRef ref) {
        SootClass sootClass = this.sootMethod.getDeclaringClass();
        SootFieldRef fieldRef = ref.getFieldRef();
        if (!fieldRef.declaringClass().equals(sootClass)) {
            return false;
        }

        try {
            SootField field = sootClass.getField(fieldRef.name(), fieldRef.type());
            /* 
             * The field exists.
             */
            if (field.isStatic()) {
                // Static fields have to be accessed using getstatic/putstatic.
                // If not we want an exception to be thrown so we need a trampoline.
                return ref instanceof StaticFieldRef;
            }
            // Instance fields have to be accessed using getfield/putfield.
            // If not we want an exception to be thrown so we need a trampoline.
            return ref instanceof InstanceFieldRef;
        } catch (RuntimeException e) {
            // SootClass.getField(...) throws RuntimeException if the field
            // isn't declared in the class.
            return false;
        }            
    }
    
    private boolean canCallDirectly(InvokeExpr expr) {
        if (expr instanceof InterfaceInvokeExpr) {
            // Never possible
            return false;
        }
        SootClass sootClass = this.sootMethod.getDeclaringClass();
        SootMethodRef methodRef = expr.getMethodRef();
        if (!methodRef.declaringClass().equals(sootClass)) {
            return false;
        }
        try {
            SootMethod method = sootClass.getMethod(methodRef.name(), 
                    methodRef.parameterTypes(), methodRef.returnType());
            if (method.isAbstract()) {
                return false;
            }
            /*
             * The method exists and isn't abstract. Non virtual (invokespecial) 
             * as well as static calls and calls to final methods can be done directly.
             */
            if (method.isStatic()) {
                // Static methods must be called using invokestatic. If not we 
                // want an exception to be thrown so we need a trampoline.
                return expr instanceof StaticInvokeExpr;
            }
            if (expr instanceof SpecialInvokeExpr) {
                return true;
            }
            if (expr instanceof VirtualInvokeExpr) {
                // Either the class or the method must be final or 
                // the method must be private
                return Modifier.isFinal(sootClass.getModifiers()) 
                        || Modifier.isFinal(method.getModifiers()) 
                        || method.isPrivate();
            }
            return false;
        } catch (RuntimeException e) {
            // SootClass.getMethod(...) throws RuntimeException if the method
            // isn't declared in the class.
            return false;
        }
    }
    
    private Value invokeExpr(Stmt stmt, InvokeExpr expr) {
        SootMethodRef methodRef = expr.getMethodRef();
        ArrayList<Value> args = new ArrayList<Value>();
        args.add(env);
        if (!(expr instanceof StaticInvokeExpr)) {
            Value base = immediate(stmt, (Immediate) ((InstanceInvokeExpr) expr).getBase());
            checkNull(stmt, base);
            args.add(base);
        }
        int i = 0;
        for (soot.Value sootArg : (List<soot.Value>) expr.getArgs())  {
            Value arg = immediate(stmt, (Immediate) sootArg);
            args.add(narrowFromI32Value(stmt, Types.getType(methodRef.parameterType(i)), arg));
            i++;
        }
        Value result = null;
        FunctionRef functionRef = config.isDebug() ? null : Intrinsics.getIntrinsic(sootMethod, stmt, expr);
        if (functionRef == null) {
            Trampoline trampoline = null;
            String targetClassName = Types.getInternalName(methodRef.declaringClass());
            String methodName = methodRef.name();
            String methodDesc = Types.getDescriptor(methodRef);
            if (expr instanceof SpecialInvokeExpr) {
                soot.Type runtimeType = ((SpecialInvokeExpr) expr).getBase().getType();
                String runtimeClassName = runtimeType == NullType.v() ? targetClassName : Types.getInternalName(runtimeType);
                trampoline = new Invokespecial(this.className, targetClassName, methodName, methodDesc, runtimeClassName);
            } else if (expr instanceof StaticInvokeExpr) {
                trampoline = new Invokestatic(this.className, targetClassName, methodName, methodDesc);
            } else if (expr instanceof VirtualInvokeExpr) {
                soot.Type runtimeType = ((VirtualInvokeExpr) expr).getBase().getType();
                String runtimeClassName = runtimeType == NullType.v() ? targetClassName : Types.getInternalName(runtimeType);
                trampoline = new Invokevirtual(this.className, targetClassName, methodName, methodDesc, runtimeClassName);
            } else if (expr instanceof InterfaceInvokeExpr) {
                trampoline = new Invokeinterface(this.className, targetClassName, methodName, methodDesc);
            }
            trampolines.add(trampoline);

            if (canCallDirectly(expr)) {
                SootMethod method = this.sootMethod.getDeclaringClass().getMethod(methodRef.name(), 
                        methodRef.parameterTypes(), methodRef.returnType());
                if (method.isSynchronized()) {
                    functionRef = FunctionBuilder.synchronizedWrapper(method).ref();
                } else {
                    functionRef = createMethodFunction(method).ref();
                }
            } else {
                functionRef = trampoline.getFunctionRef();
            }
        }
        result = call(stmt, functionRef, args.toArray(new Value[0]));
        if (result != null) {
            return widenToI32Value(stmt, result, methodRef.returnType().equals(CharType.v()));
        } else {
            return null;
        }
    }

    private void checkNull(Stmt stmt, Value base) {
        NullCheckTag nullCheckTag = (NullCheckTag) stmt.getTag("NullCheckTag");
        if (nullCheckTag == null || nullCheckTag.needCheck()) {
            call(stmt, CHECK_NULL, env, base);
        }
    }
    
    private void checkBounds(Stmt stmt, Value base, Value index) {
        ArrayCheckTag arrayCheckTag = (ArrayCheckTag) stmt.getTag("ArrayCheckTag");
        if (arrayCheckTag == null || arrayCheckTag.isCheckLower()) {
            call(stmt, CHECK_LOWER, env, base, index);
        }
        if (arrayCheckTag == null || arrayCheckTag.isCheckUpper()) {
            call(stmt, CHECK_UPPER, env, base, index);
        }
    }
    
    private List<Trap> getTrapsAt(Unit u) {
        List<Trap> result = this.trapsAt.get(u);
        if (result == null) {
            Body body = sootMethod.getActiveBody();
            Chain<Trap> traps = body.getTraps();
            if (traps.isEmpty()) {
                result = Collections.emptyList();
            } else {
                result = new ArrayList<Trap>();
                PatchingChain<Unit> units = body.getUnits();
                for (Trap trap : traps) {
                    Unit beginUnit = trap.getBeginUnit();
                    Unit endUnit = trap.getEndUnit();
                    if (beginUnit != endUnit && u != endUnit) {
                        if (u == beginUnit || (units.follows(u, beginUnit) && units.follows(endUnit, u))) {
                            result.add(trap);
                        }
                    }
                }
            }
            this.trapsAt.put(u, result);
        }
        return result;
    }
    
    private void assign(DefinitionStmt stmt) {
        /*
         * leftOp is either a Local, an ArrayRef or a FieldRef
         * rightOp is either a Local, a Ref, or an Expr
         */

        soot.Value rightOp = stmt.getRightOp();
        Value result;

        if (rightOp instanceof Immediate) {
            Immediate immediate = (Immediate) rightOp;
            result = immediate(stmt, immediate);
        } else if (rightOp instanceof ThisRef) {
            result = function.getParameterRef(1);
        } else if (rightOp instanceof ParameterRef) {
            ParameterRef ref = (ParameterRef) rightOp;
            int index = (sootMethod.isStatic() ? 1 : 2) + ref.getIndex();
            Value p = new VariableRef("p" + index, Types.getType(ref.getType()));
            result = widenToI32Value(stmt, p, Types.isUnsigned(ref.getType()));
        } else if (rightOp instanceof CaughtExceptionRef) {
            result = call(stmt, BC_EXCEPTION_CLEAR, env);
        } else if (rightOp instanceof ArrayRef) {
            ArrayRef ref = (ArrayRef) rightOp;
            VariableRef base = (VariableRef) immediate(stmt, (Immediate) ref.getBase());
            if (ref.getType() instanceof NullType) {
                // The base value is always null. Do a null check which will
                // always throw NPE.
                checkNull(stmt, base);
                return;
            } else {
                Value index = immediate(stmt, (Immediate) ref.getIndex());
                checkNull(stmt, base);
                checkBounds(stmt, base, index);
                result = call(stmt, getArrayLoad(ref.getType()), base, index);
                result = widenToI32Value(stmt, result, Types.isUnsigned(ref.getType()));
            }
        } else if (rightOp instanceof InstanceFieldRef) {
            InstanceFieldRef ref = (InstanceFieldRef) rightOp;
            Value base = immediate(stmt, (Immediate) ref.getBase());
            checkNull(stmt, base);
            FunctionRef fn = null;
            if (canAccessDirectly(ref)) {
                fn = new FunctionRef(Symbols.getterSymbol(ref.getFieldRef()), 
                        new FunctionType(Types.getType(ref.getType()), Types.ENV_PTR, Types.OBJECT_PTR));
            } else {
                soot.Type runtimeType = ref.getBase().getType();
                String targetClassName = Types.getInternalName(ref.getFieldRef().declaringClass());
                String runtimeClassName = runtimeType == NullType.v() ? targetClassName : Types.getInternalName(runtimeType);
                Trampoline trampoline = new GetField(this.className, targetClassName, 
                        ref.getFieldRef().name(), Types.getDescriptor(ref.getFieldRef().type()), runtimeClassName);
                trampolines.add(trampoline);
                fn = trampoline.getFunctionRef();
            }
            result = call(stmt, fn, env, base);
            result = widenToI32Value(stmt, result, Types.isUnsigned(ref.getType()));
        } else if (rightOp instanceof StaticFieldRef) {
            StaticFieldRef ref = (StaticFieldRef) rightOp;
            FunctionRef fn = config.isDebug() ? null : Intrinsics.getIntrinsic(sootMethod, stmt);
            if (fn == null) {
                if (canAccessDirectly(ref)) {
                    fn = new FunctionRef(Symbols.getterSymbol(ref.getFieldRef()), 
                            new FunctionType(Types.getType(ref.getType()), Types.ENV_PTR));
                } else {
                    String targetClassName = Types.getInternalName(ref.getFieldRef().declaringClass());
                    Trampoline trampoline = new GetStatic(this.className, targetClassName,
                            ref.getFieldRef().name(), Types.getDescriptor(ref.getFieldRef().type()));
                    trampolines.add(trampoline);
                    fn = trampoline.getFunctionRef();
                }
            }
            result = call(stmt, fn, env);
            result = widenToI32Value(stmt, result, Types.isUnsigned(ref.getType()));
        } else if (rightOp instanceof Expr) {
            if (rightOp instanceof BinopExpr) {
                BinopExpr expr = (BinopExpr) rightOp;
                Type rightType = Types.getLocalType(expr.getType());
                Variable resultVar = function.newVariable(rightType);
                result = resultVar.ref();
                Value op1 = immediate(stmt, (Immediate) expr.getOp1());
                Value op2 = immediate(stmt, (Immediate) expr.getOp2());
                if (rightOp instanceof AddExpr) {
                    if (rightType instanceof IntegerType) {
                        function.add(new Add(resultVar, op1, op2)).attach(stmt);
                    } else {
                        function.add(new Fadd(resultVar, op1, op2)).attach(stmt);
                    }
                } else if (rightOp instanceof AndExpr) {
                    function.add(new And(resultVar, op1, op2)).attach(stmt);
                } else if (rightOp instanceof CmpExpr) {
                    Variable t1 = function.newVariable(Type.I1);
                    Variable t2 = function.newVariable(Type.I1);
                    Variable t3 = function.newVariable(resultVar.getType());
                    Variable t4 = function.newVariable(resultVar.getType());
                    function.add(new Icmp(t1, Icmp.Condition.slt, op1, op2)).attach(stmt);
                    function.add(new Icmp(t2, Icmp.Condition.sgt, op1, op2)).attach(stmt);
                    function.add(new Zext(t3, new VariableRef(t1), resultVar.getType())).attach(stmt);
                    function.add(new Zext(t4, new VariableRef(t2), resultVar.getType())).attach(stmt);
                    function.add(new Sub(resultVar, new VariableRef(t4), new VariableRef(t3))).attach(stmt);
                } else if (rightOp instanceof DivExpr) {
                    if (rightType instanceof IntegerType) {
                        FunctionRef f = rightType == Type.I64 ? LDIV : IDIV;
                        result = call(stmt, f, env, op1, op2);
                    } else {
                        // float or double
                        function.add(new Fdiv(resultVar, op1, op2)).attach(stmt);
                    }
                } else if (rightOp instanceof MulExpr) {
                    if (rightType instanceof IntegerType) {
                        function.add(new Mul(resultVar, op1, op2)).attach(stmt);
                    } else {
                        function.add(new Fmul(resultVar, op1, op2)).attach(stmt);
                    }
                } else if (rightOp instanceof OrExpr) {
                    function.add(new Or(resultVar, op1, op2)).attach(stmt);
                } else if (rightOp instanceof RemExpr) {
                    if (rightType instanceof IntegerType) {
                        FunctionRef f = rightType == Type.I64 ? LREM : IREM;
                        result = call(stmt, f, env, op1, op2);
                    } else {
                        FunctionRef f = rightType == Type.DOUBLE ? DREM : FREM;
                        result = call(stmt, f, env, op1, op2);
                    }
                } else if (rightOp instanceof ShlExpr || rightOp instanceof ShrExpr || rightOp instanceof UshrExpr) {
                    IntegerType type = (IntegerType) op1.getType();
                    int bits = type.getBits();
                    Variable t = function.newVariable(op2.getType());
                    function.add(new And(t, op2, new IntegerConstant(bits - 1, (IntegerType) op2.getType()))).attach(stmt);
                    Value shift = t.ref();
                    if (((IntegerType) shift.getType()).getBits() < bits) {
                        Variable tmp = function.newVariable(type);
                        function.add(new Zext(tmp, shift, type)).attach(stmt);
                        shift = tmp.ref();
                    }
                    if (rightOp instanceof ShlExpr) {
                        function.add(new Shl(resultVar, op1, shift)).attach(stmt);
                    } else if (rightOp instanceof ShrExpr) {
                        function.add(new Ashr(resultVar, op1, shift)).attach(stmt);
                    } else {
                        function.add(new Lshr(resultVar, op1, shift)).attach(stmt);
                    }
                } else if (rightOp instanceof SubExpr) {
                    if (rightType instanceof IntegerType) {
                        function.add(new Sub(resultVar, op1, op2)).attach(stmt);
                    } else {
                        function.add(new Fsub(resultVar, op1, op2)).attach(stmt);
                    }
                } else if (rightOp instanceof XorExpr) {
                    function.add(new Xor(resultVar, op1, op2)).attach(stmt);
                } else if (rightOp instanceof XorExpr) {
                    function.add(new Xor(resultVar, op1, op2)).attach(stmt);
                } else if (rightOp instanceof CmplExpr) {
                    FunctionRef f = op1.getType() == Type.FLOAT ? FCMPL : DCMPL;
                    function.add(new Call(resultVar, f, op1, op2)).attach(stmt);
                } else if (rightOp instanceof CmpgExpr) {
                    FunctionRef f = op1.getType() == Type.FLOAT ? FCMPG : DCMPG;
                    function.add(new Call(resultVar, f, op1, op2)).attach(stmt);
                } else {
                    throw new IllegalArgumentException("Unknown type for rightOp: " + rightOp.getClass());
                }
            } else if (rightOp instanceof CastExpr) {
                Value op = immediate(stmt, (Immediate) ((CastExpr) rightOp).getOp());
                soot.Type sootTargetType = ((CastExpr) rightOp).getCastType();
                soot.Type sootSourceType = ((CastExpr) rightOp).getOp().getType();
                if (sootTargetType instanceof PrimType) {
                    Type targetType = Types.getType(sootTargetType);
                    Type sourceType = Types.getType(sootSourceType);
                    if (targetType instanceof IntegerType && sourceType instanceof IntegerType) {
                        // op is at least I32 and has already been widened if source type had fewer bits then I32
                        IntegerType toType = (IntegerType) targetType;
                        IntegerType fromType = (IntegerType) op.getType();
                        Variable v = function.newVariable(toType);
                        if (fromType.getBits() < toType.getBits()) {
                            // Widening
                            if (Types.isUnsigned(sootSourceType)) {
                                function.add(new Zext(v, op, toType)).attach(stmt);
                            } else {
                                function.add(new Sext(v, op, toType)).attach(stmt);
                            }
                        } else if (fromType.getBits() == toType.getBits()) {
                            function.add(new Bitcast(v, op, toType)).attach(stmt);
                        } else {
                            // Narrow
                            function.add(new Trunc(v, op, toType)).attach(stmt);
                        }
                        result = widenToI32Value(stmt, v.ref(), Types.isUnsigned(sootTargetType));
                    } else if (targetType instanceof FloatingPointType && sourceType instanceof IntegerType) {
                        // we always to a signed conversion since if op is char it has already been zero extended to I32
                        Variable v = function.newVariable(targetType);
                        function.add(new Sitofp(v, op, targetType)).attach(stmt);
                        result = v.ref();
                    } else if (targetType instanceof FloatingPointType && sourceType instanceof FloatingPointType) {
                        Variable v = function.newVariable(targetType);
                        if (targetType == Type.FLOAT && sourceType == Type.DOUBLE) {
                            function.add(new Fptrunc(v, op, targetType)).attach(stmt);
                        } else if (targetType == Type.DOUBLE && sourceType == Type.FLOAT) {
                            function.add(new Fpext(v, op, targetType)).attach(stmt);
                        } else {
                            function.add(new Bitcast(v, op, targetType)).attach(stmt);
                        }
                        result = v.ref();
                    } else {
                        // F2I, F2L, D2I, D2L
                        FunctionRef f = null;
                        if (targetType == Type.I32 && sourceType == Type.FLOAT) {
                            f = F2I;
                        } else if (targetType == Type.I64 && sourceType == Type.FLOAT) {
                            f = F2L;
                        } else if (targetType == Type.I32 && sourceType == Type.DOUBLE) {
                            f = D2I;
                        } else if (targetType == Type.I64 && sourceType == Type.DOUBLE) {
                            f = D2L;
                        } else {
                            throw new IllegalArgumentException();
                        }
                        Variable v = function.newVariable(targetType);
                        function.add(new Call(v, f, op)).attach(stmt);
                        result = v.ref();
                    }
                } else {
                    if (sootTargetType instanceof soot.ArrayType 
                            && ((soot.ArrayType) sootTargetType).getElementType() instanceof PrimType) {
                        soot.Type primType = ((soot.ArrayType) sootTargetType).getElementType();
                        GlobalRef arrayClassPtr = new GlobalRef("array_" + Types.getDescriptor(primType), Types.CLASS_PTR);
                        Variable arrayClass = function.newVariable(Types.CLASS_PTR);
                        function.add(new Load(arrayClass, arrayClassPtr)).attach(stmt);
                        result = call(stmt, CHECKCAST_PRIM_ARRAY, env, arrayClass.ref(), op);
                    } else {
                        String targetClassName = Types.getInternalName(sootTargetType);
                        Trampoline trampoline = new Checkcast(this.className, targetClassName);
                        trampolines.add(trampoline);
                        result = call(stmt, trampoline.getFunctionRef(), env, op);
                    }
                }
            } else if (rightOp instanceof InstanceOfExpr) {
                Value op = immediate(stmt, (Immediate) ((InstanceOfExpr) rightOp).getOp());
                soot.Type checkType = ((InstanceOfExpr) rightOp).getCheckType();
                if (checkType instanceof soot.ArrayType 
                        && ((soot.ArrayType) checkType).getElementType() instanceof PrimType) {
                    soot.Type primType = ((soot.ArrayType) checkType).getElementType();
                    GlobalRef arrayClassPtr = new GlobalRef("array_" + Types.getDescriptor(primType), Types.CLASS_PTR);
                    Variable arrayClass = function.newVariable(Types.CLASS_PTR);
                    function.add(new Load(arrayClass, arrayClassPtr)).attach(stmt);
                    result = call(stmt, INSTANCEOF_PRIM_ARRAY, env, arrayClass.ref(), op);
                } else {
                    String targetClassName = Types.getInternalName(checkType);
                    Trampoline trampoline = new Instanceof(this.className, targetClassName);
                    trampolines.add(trampoline);
                    result = call(stmt, trampoline.getFunctionRef(), env, op);
                }
            } else if (rightOp instanceof NewExpr) {
                String targetClassName = Types.getInternalName(((NewExpr) rightOp).getBaseType());
                FunctionRef fn = null;
                if (targetClassName.equals(this.className)) {
                    fn = FunctionBuilder.allocator(sootMethod.getDeclaringClass()).ref();
                } else {
                    Trampoline trampoline = new New(this.className, targetClassName);
                    trampolines.add(trampoline);
                    fn = trampoline.getFunctionRef();
                }
                result = call(stmt, fn, env);
            } else if (rightOp instanceof NewArrayExpr) {
                NewArrayExpr expr = (NewArrayExpr) rightOp;
                Value size = immediate(stmt, (Immediate) expr.getSize());
                if (expr.getBaseType() instanceof PrimType) {
                    result = call(stmt, getNewArray(expr.getBaseType()), env, size);
                } else {
                    String targetClassName = Types.getInternalName(expr.getType());
                    Trampoline trampoline = new Anewarray(this.className, targetClassName);
                    trampolines.add(trampoline);
                    result = call(stmt, trampoline.getFunctionRef(), env, size);
                }
            } else if (rightOp instanceof NewMultiArrayExpr) {
                NewMultiArrayExpr expr = (NewMultiArrayExpr) rightOp;
                if (expr.getBaseType().numDimensions == 1 && expr.getBaseType().getElementType() instanceof PrimType) {
                    Value size = immediate(stmt, (Immediate) expr.getSize(0));
                    result = call(stmt, getNewArray(expr.getBaseType().getElementType()), env, size);
                } else {
                    for (int i = 0; i < expr.getSizeCount(); i++) {
                        Value size = immediate(stmt, (Immediate) expr.getSize(i));
                        Variable ptr = function.newVariable(new PointerType(Type.I32));
                        function.add(new Getelementptr(ptr, dims.ref(), 0, i)).attach(stmt);
                        function.add(new Store(size, ptr.ref())).attach(stmt);
                    }
                    Variable dimsI32 = function.newVariable(new PointerType(Type.I32));
                    function.add(new Bitcast(dimsI32, dims.ref(), dimsI32.getType())).attach(stmt);
                    String targetClassName = Types.getInternalName(expr.getType());
                    Trampoline trampoline = new Multianewarray(this.className, targetClassName);
                    trampolines.add(trampoline);
                    result = call(stmt, trampoline.getFunctionRef(), env, new IntegerConstant(expr.getSizeCount()), dimsI32.ref());
                }
            } else if (rightOp instanceof InvokeExpr) {
                result = invokeExpr(stmt, (InvokeExpr) rightOp);
            } else if (rightOp instanceof LengthExpr) {
                Value op = immediate(stmt, (Immediate) ((LengthExpr) rightOp).getOp());
                checkNull(stmt, op);
                Variable v = function.newVariable(Type.I32);
                function.add(new Call(v, ARRAY_LENGTH, op)).attach(stmt);
                result = v.ref();
            } else if (rightOp instanceof NegExpr) {
                NegExpr expr = (NegExpr) rightOp;
                Value op = immediate(stmt, (Immediate) expr.getOp());
                Type rightType = op.getType();
                Variable v = function.newVariable(op.getType());
                if (rightType instanceof IntegerType) {
                    function.add(new Sub(v, new IntegerConstant(0, (IntegerType) rightType), op)).attach(stmt);
                } else {
                    function.add(new Fmul(v, new FloatingPointConstant(-1.0, (FloatingPointType) rightType), op)).attach(stmt);
                }
                result = v.ref();
            } else {
                throw new IllegalArgumentException("Unknown type for rightOp: " + rightOp.getClass());
            }
        } else {
            throw new IllegalArgumentException("Unknown type for rightOp: " + rightOp.getClass());
        }

        soot.Value leftOp = stmt.getLeftOp();

        if (leftOp instanceof Local) {
            Local local = (Local) leftOp;
            VariableRef v = new VariableRef(local.getName(), new PointerType(Types.getLocalType(leftOp.getType())));
            function.add(new Store(result, v, !sootMethod.getActiveBody().getTraps().isEmpty())).attach(stmt);
        } else {
            Type leftType = Types.getType(leftOp.getType());
            Value narrowedResult = narrowFromI32Value(stmt, leftType, result);
            if (leftOp instanceof ArrayRef) {
                ArrayRef ref = (ArrayRef) leftOp;
                VariableRef base = (VariableRef) immediate(stmt, (Immediate) ref.getBase());
                Value index = immediate(stmt, (Immediate) ref.getIndex());
                checkNull(stmt, base);
                checkBounds(stmt, base, index);
                if (leftOp.getType() instanceof RefLikeType) {
                    call(stmt, BC_SET_OBJECT_ARRAY_ELEMENT, env, base, index, narrowedResult);
                } else {
                    call(stmt, getArrayStore(leftOp.getType()), base, index, narrowedResult);
                }
            } else if (leftOp instanceof InstanceFieldRef) {
                InstanceFieldRef ref = (InstanceFieldRef) leftOp;
                Value base = immediate(stmt, (Immediate) ref.getBase());
                checkNull(stmt, base);
                FunctionRef fn = null;
                if (canAccessDirectly(ref)) {
                    fn = new FunctionRef(Symbols.setterSymbol(ref.getFieldRef()), 
                            new FunctionType(Type.VOID, Types.ENV_PTR, Types.OBJECT_PTR, Types.getType(ref.getType())));
                } else {
                    soot.Type runtimeType = ref.getBase().getType();
                    String targetClassName = Types.getInternalName(ref.getFieldRef().declaringClass());
                    String runtimeClassName = runtimeType == NullType.v() ? targetClassName : Types.getInternalName(runtimeType);
                    Trampoline trampoline = new PutField(this.className, targetClassName, 
                            ref.getFieldRef().name(), Types.getDescriptor(ref.getFieldRef().type()), runtimeClassName);
                    trampolines.add(trampoline);
                    fn = trampoline.getFunctionRef();
                }
                call(stmt, fn, env, base, narrowedResult);
            } else if (leftOp instanceof StaticFieldRef) {
                StaticFieldRef ref = (StaticFieldRef) leftOp;
                FunctionRef fn = null;
                if (canAccessDirectly(ref)) {
                    fn = new FunctionRef(Symbols.setterSymbol(ref.getFieldRef()), 
                            new FunctionType(Type.VOID, Types.ENV_PTR, Types.getType(ref.getType())));
                } else {
                    String targetClassName = Types.getInternalName(ref.getFieldRef().declaringClass());
                    Trampoline trampoline = new PutStatic(this.className, targetClassName, 
                            ref.getFieldRef().name(), Types.getDescriptor(ref.getFieldRef().type()));
                    trampolines.add(trampoline);
                    fn = trampoline.getFunctionRef();
                }
                call(stmt, fn, env, narrowedResult);
            } else {
                throw new IllegalArgumentException("Unknown type for leftOp: " + leftOp.getClass());
            }
        }
    }

    private void return_(ReturnStmt stmt) {
        /*
         * op is an Immediate.
         */
        Value op = immediate(stmt, (Immediate) stmt.getOp());
        Value value = narrowFromI32Value(stmt, function.getType().getReturnType(), op);
        function.add(new Ret(value)).attach(stmt);
    }
    
    private void returnVoid(ReturnVoidStmt stmt) {
        function.add(new Ret()).attach(stmt);
    }
    
    private void if_(IfStmt stmt) {
        ConditionExpr condition = (ConditionExpr) stmt.getCondition();
        Value op1 = immediate(stmt, (Immediate) condition.getOp1());
        Value op2 = immediate(stmt, (Immediate) condition.getOp2());
        Icmp.Condition c = null;
        if (condition instanceof EqExpr) {
            c = Icmp.Condition.eq;
        } else if (condition instanceof NeExpr) {
            c = Icmp.Condition.ne;
        } else if (condition instanceof GtExpr) {
            c = Icmp.Condition.sgt;
        } else if (condition instanceof LtExpr) {
            c = Icmp.Condition.slt;
        } else if (condition instanceof GeExpr) {
            c = Icmp.Condition.sge;
        } else if (condition instanceof LeExpr) {
            c = Icmp.Condition.sle;
        }
        Variable result = function.newVariable(Type.I1);
        function.add(new Icmp(result, c, op1, op2)).attach(stmt);
        Unit nextUnit = sootMethod.getActiveBody().getUnits().getSuccOf(stmt);
        function.add(new Br(new VariableRef(result), 
                function.newBasicBlockRef(new Label(stmt.getTarget())), 
                function.newBasicBlockRef(new Label(nextUnit)))).attach(stmt);
    }
    
    private void lookupSwitch(LookupSwitchStmt stmt) {
        Map<IntegerConstant, BasicBlockRef> targets = new HashMap<IntegerConstant, BasicBlockRef>();
        for (int i = 0; i < stmt.getTargetCount(); i++) {
            int value = stmt.getLookupValue(i);
            Unit target = stmt.getTarget(i);
            targets.put(new IntegerConstant(value), function.newBasicBlockRef(new Label(target)));
        }
        BasicBlockRef def = function.newBasicBlockRef(new Label(stmt.getDefaultTarget()));
        Value key = immediate(stmt, (Immediate) stmt.getKey());
        function.add(new Switch(key, def, targets)).attach(stmt);
    }
    
    private void tableSwitch(TableSwitchStmt stmt) {
        Map<IntegerConstant, BasicBlockRef> targets = new HashMap<IntegerConstant, BasicBlockRef>();
        for (int i = stmt.getLowIndex(); i <= stmt.getHighIndex(); i++) {
            Unit target = stmt.getTarget(i - stmt.getLowIndex());
            targets.put(new IntegerConstant(i), function.newBasicBlockRef(new Label(target)));
        }
        BasicBlockRef def = function.newBasicBlockRef(new Label(stmt.getDefaultTarget()));
        Value key = immediate(stmt, (Immediate) stmt.getKey());
        function.add(new Switch(key, def, targets)).attach(stmt);
    }
    
    private void goto_(GotoStmt stmt) {
        function.add(new Br(function.newBasicBlockRef(new Label(stmt.getTarget())))).attach(stmt);
    }
    
    private void throw_(ThrowStmt stmt) {
        Value obj = immediate(stmt, (Immediate) stmt.getOp());
        checkNull(stmt, obj);
        call(stmt, BC_THROW, env, obj);
        function.add(new Unreachable()).attach(stmt);
    }
    
    private void invoke(InvokeStmt stmt) {
        invokeExpr(stmt, stmt.getInvokeExpr());
    }
    
    private void enterMonitor(EnterMonitorStmt stmt) {
        Value op = immediate(stmt, (Immediate) stmt.getOp());
        checkNull(stmt, op);
        call(stmt, MONITORENTER, env, op);
    }
    
    private void exitMonitor(ExitMonitorStmt stmt) {
        Value op = immediate(stmt, (Immediate) stmt.getOp());
        checkNull(stmt, op);
        call(stmt, MONITOREXIT, env, op);
    }

    private void nop(NopStmt stmt) {
        /*
         * We need to preserve NOPs as they may be needed by compiler plugins to
         * work properly. There's no NOP bitcode instruction. Instead we use an
         * ADD instruction which has no side-effects. LLVM should be able to
         * optimize it out later on.
         */
        Variable v = function.newVariable(Type.I32);
        function.add(new Add(v, new IntegerConstant(0), new IntegerConstant(0))).attach(stmt);
    }
}
