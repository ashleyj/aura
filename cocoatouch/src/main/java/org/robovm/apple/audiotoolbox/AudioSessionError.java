/*
 * Copyright (C) 2014 Trillian Mobile AB
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
package org.robovm.apple.audiotoolbox;

/*<imports>*/
import java.io.*;
import java.nio.*;
import java.util.*;
import org.robovm.objc.*;
import org.robovm.objc.annotation.*;
import org.robovm.objc.block.*;
import org.robovm.rt.*;
import org.robovm.rt.bro.*;
import org.robovm.rt.bro.annotation.*;
import org.robovm.rt.bro.ptr.*;
import org.robovm.apple.foundation.*;
import org.robovm.apple.corefoundation.*;
import org.robovm.apple.coregraphics.*;
import org.robovm.apple.opengles.*;
import org.robovm.apple.coreaudio.*;
import org.robovm.apple.coremedia.*;
/*</imports>*/

/*<javadoc>*/

/*</javadoc>*/
/*<annotations>*/@Marshaler(ValuedEnum.AsMachineSizedUIntMarshaler.class)/*</annotations>*/
public enum /*<name>*/AudioSessionError/*</name>*/ implements ValuedEnum {
    /*<values>*/
    ssionNo(0L),
    ssionNotInitialized(560557673L),
    ssionAlreadyInitialized(1768843636L),
    ssionInitialization(1768843583L),
    ssionUnsupportedProperty(1886681407L),
    ssionBadPropertySize(561211770L),
    ssionNotActive(560030580L),
    rvicesNoHardware(1852794999L),
    ssionNoCategorySet(1063477620L),
    ssionIncompatibleCategory(560161140L),
    ssionUnspecified(2003329396L);
    /*</values>*/

    /*<bind>*/
    /*</bind>*/
    /*<constants>*//*</constants>*/
    /*<methods>*//*</methods>*/

    private final long n;

    private /*<name>*/AudioSessionError/*</name>*/(long n) { this.n = n; }
    public long value() { return n; }
    public static /*<name>*/AudioSessionError/*</name>*/ valueOf(long n) {
        for (/*<name>*/AudioSessionError/*</name>*/ v : values()) {
            if (v.n == n) {
                return v;
            }
        }
        throw new IllegalArgumentException("No constant with value " + n + " found in " 
            + /*<name>*/AudioSessionError/*</name>*/.class.getName());
    }
}