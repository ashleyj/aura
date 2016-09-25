/*
 * Copyright 2016 Justin Shapcott.
 * Modifications Copyright 2016 Ashley Williams.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package aura.compiler.plugin.shadowframe;

import java.io.IOException;
import java.util.List;

import aura.compiler.Functions;
import aura.compiler.ModuleBuilder;
import aura.compiler.clazz.Clazz;
import aura.compiler.config.Config;
import aura.compiler.llvm.BasicBlock;
import aura.compiler.llvm.Call;
import aura.compiler.llvm.Function;
import aura.compiler.llvm.Instruction;
import aura.compiler.llvm.IntegerConstant;
import aura.compiler.llvm.PlainTextInstruction;
import aura.compiler.llvm.Ret;
import aura.compiler.llvm.Type;
import aura.compiler.llvm.Unreachable;
import aura.compiler.llvm.Value;
import aura.compiler.llvm.VariableRef;
import aura.compiler.plugin.AbstractCompilerPlugin;

import soot.SootMethod;
import soot.Unit;
import soot.tagkit.LineNumberTag;

public class ShadowFramePlugin extends AbstractCompilerPlugin {
    private static final String SHADOW_FRAME_VAR_NAME = "__shadowFrame";
    
    @Override
    public void afterMethod(Config config, Clazz clazz, SootMethod method, ModuleBuilder moduleBuilder,
            Function function) throws IOException {

        // don't try to generate shadow frames for native or abstract methods
        // or methods that don't have any instructions in them
        if (method.isNative() || method.isAbstract() || !method.hasActiveBody()) {
            return;
        }
        
        BasicBlock entryBlock = function.getBasicBlocks().get(0);
        
        //Method has only a return null statement
        if (entryBlock.getInstructions().size() == 1) {
        	return;
        }

        // get functionsAddress for shadowframe
        String functionSignature = function.getSignature();
        PlainTextInstruction storeFunctionAddress = new PlainTextInstruction(                
                  "%funcAddr = bitcast " + functionSignature + "* @\"" + function.getName() +"\" to i8*\n");        
        entryBlock.getInstructions().add(1, storeFunctionAddress);
        
        // push frame into Env        
        Value env = function.getParameterRef(0);         
        VariableRef funcAddr = new VariableRef("funcAddr", Type.I8_PTR);
        // memory is allocated in runtime on the heap
        entryBlock.getInstructions().add(2, new Call(Functions.PUSH_SHADOW_FRAME, env, funcAddr));
        
        //update line numbers for each new instruction
        int currentLineNumber = 0;
        for (BasicBlock bb : function.getBasicBlocks()) {
            for (int i = 0; i < bb.getInstructions().size(); i++) {
                Instruction instruction = bb.getInstructions().get(i);
                List<Object> units = instruction.getAttachments();
                for (Object object : units) {
                    if (object instanceof Unit) {
                        Unit unit = (Unit) object;
                        LineNumberTag tag = (LineNumberTag) unit.getTag("LineNumberTag");
                        if (tag != null) {
                            if (currentLineNumber == 0 || currentLineNumber != tag.getLineNumber()) {
                                currentLineNumber = tag.getLineNumber();
                                // push new line number
                                bb.getInstructions().add(i, new Call(Functions.PUSH_SHADOW_LINE_NUMBER, env,
                                        new IntegerConstant(currentLineNumber)));
                            }
                        }
                    }
                }
            }
        }

        for(BasicBlock bb: function.getBasicBlocks()) {
            for(int i = 0; i < bb.getInstructions().size(); i++) {
                Instruction inst = bb.getInstructions().get(i);
                if(inst instanceof Ret || (i > 0 && inst instanceof Unreachable && bb.getInstructions().get(i-1) instanceof Call)) {
                    bb.getInstructions().add(i, new Call(Functions.POP_SHADOW_FRAME, env));
                    break;
                }
            }
        }        
    }
}
