/*
 * Copyright (C) 2014 RoboVM AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package aura.rt.bro.ptr;

import java.lang.reflect.Method;

import aura.rt.VM;
import aura.rt.bro.Struct;
import aura.rt.bro.annotation.Callback;
import aura.rt.bro.annotation.StructMember;
import aura.rt.bro.annotation.Pointer;

/**
 * Points to a function. There's no way to dereference a {@link FunctionPtr}. 
 */
public final class FunctionPtr extends Struct<FunctionPtr> {

    /**
     * Pointer to {@link FunctionPtr}.
     */
    public static class FunctionPtrPtr extends aura.rt.bro.ptr.Ptr<FunctionPtr, FunctionPtrPtr> {}
    
    /**
     * Creates a new {@link FunctionPtr} which points to the specified
     * {@link Callback} {@link Method}.
     * 
     * @param callbackMethod the {@link Callback} {@link Method} this
     *        {@link FunctionPtr} will point at.
     * @throws IllegalArgumentException if the specified {@link Method} isn't
     *         a {@link Callback}.
     */
    public FunctionPtr(Method callbackMethod) {
        super(0);
        if (callbackMethod == null) {
            throw new NullPointerException("callbackMethod");
        }
        if (!callbackMethod.isAnnotationPresent(Callback.class)) {
            throw new IllegalArgumentException("The method " + callbackMethod
                    + " is not a @Callback method");
        }
        setHandle(VM.getCallbackMethodImpl(callbackMethod));
    }

    /**
     * Hidden.
     */
    @StructMember(0)
    private native @Pointer long get();
    
    /**
     * Hidden.
     */
    @StructMember(0)
    private native void set(@Pointer long value);
}
