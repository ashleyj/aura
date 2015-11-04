/*
 * Copyright (C) 2008 The Android Open Source Project
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

package aura.compiler.util.generic;


public final class ImplForArray implements GenericArrayType {
    private final Type componentType;

    public ImplForArray(Type componentType) {
        this.componentType = componentType;
    }

    public Type getGenericComponentType() {
        try {
            return ((ImplForType)componentType).getResolvedType();
        } catch (ClassCastException e) {
            return componentType;
        }
    }

    public String toString() {
        return componentType.toString() + "[]";
    }
    
    @Override
    public String toGenericSignature() {
        return "[" + componentType.toGenericSignature();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ImplForArray)) {
            return false;
        }
        ImplForArray that = (ImplForArray) obj;
        return this.componentType.equals(that.componentType);
    }
}
