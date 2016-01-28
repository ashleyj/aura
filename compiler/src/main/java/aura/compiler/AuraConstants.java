package aura.compiler;

/*
 * Copyright (C) 2016 Aura Project
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
public class AuraConstants {

    /**
     * Names of root classes. These classes will always be linked in. These are
     * here because they are either required by the RoboVM specific native VM
     * libraries or by the Android's libcore native code.
     */
    public static final String[] ROOT_CLASSES = {
            "java/io/FileDescriptor",
            "java/io/PrintWriter",
            "java/io/Serializable",
            "java/io/StringWriter",
            "java/lang/AbstractMethodError",
            "java/lang/annotation/Annotation",
            "java/lang/annotation/AnnotationFormatError",
            "java/lang/ArithmeticException",
            "java/lang/ArrayIndexOutOfBoundsException",
            "java/lang/ArrayStoreException",
            "java/lang/Boolean",
            "java/lang/Byte",
            "java/lang/Character",
            "java/lang/Class",
            "java/lang/ClassCastException",
            "java/lang/ClassLoader",
            "java/lang/ClassLoader$SystemClassLoader",
            "java/lang/ClassNotFoundException",
            "java/lang/Cloneable",
            "java/lang/Daemons",
            "java/lang/Double",
            "java/lang/Enum",
            "java/lang/Error",
            "java/lang/ExceptionInInitializerError",
            "java/lang/Float",
            "java/lang/IllegalAccessError",
            "java/lang/IllegalArgumentException",
            "java/lang/IllegalMonitorStateException",
            "java/lang/IllegalStateException",
            "java/lang/IncompatibleClassChangeError",
            "java/lang/IndexOutOfBoundsException",
            "java/lang/InstantiationError",
            "java/lang/InstantiationException",
            "java/lang/Integer",
            "java/lang/InternalError",
            "java/lang/InterruptedException",
            "java/lang/LinkageError",
            "java/lang/Long",
            "java/lang/NegativeArraySizeException",
            "java/lang/NoClassDefFoundError",
            "java/lang/NoSuchFieldError",
            "java/lang/NoSuchMethodError",
            "java/lang/NullPointerException",
            "java/lang/Object",
            "java/lang/OutOfMemoryError",
            "java/lang/RealToString",
            "java/lang/ref/FinalizerReference",
            "java/lang/ref/PhantomReference",
            "java/lang/ref/Reference",
            "java/lang/ref/ReferenceQueue",
            "java/lang/ref/SoftReference",
            "java/lang/ref/WeakReference",
            "java/lang/reflect/AccessibleObject",
            "java/lang/reflect/Constructor",
            "java/lang/reflect/Field",
            "java/lang/reflect/InvocationHandler",
            "java/lang/reflect/InvocationTargetException",
            "java/lang/reflect/Method",
            "java/lang/reflect/Proxy",
            "java/lang/reflect/UndeclaredThrowableException",
            "java/lang/Runtime",
            "java/lang/RuntimeException",
            "java/lang/Short",
            "java/lang/StackOverflowError",
            "java/lang/StackTraceElement",
            "java/lang/String",
            "java/lang/System",
            "java/lang/Thread",
            "java/lang/Thread$UncaughtExceptionHandler",
            "java/lang/ThreadGroup",
            "java/lang/Throwable",
            "java/lang/TypeNotPresentException",
            "java/lang/UnsatisfiedLinkError",
            "java/lang/UnsupportedOperationException",
            "java/lang/VerifyError",
            "java/lang/VMClassLoader",
            "java/math/BigDecimal",
            "java/net/Inet6Address",
            "java/net/InetAddress",
            "java/net/InetSocketAddress",
            "java/net/InetUnixAddress",
            "java/net/Socket",
            "java/net/SocketImpl",
            "java/nio/charset/CharsetICU",
            "java/nio/DirectByteBuffer",
            "java/text/Bidi$Run",
            "java/text/ParsePosition",
            "java/util/Calendar",
            "java/util/regex/PatternSyntaxException",
            "java/util/zip/Deflater",
            "java/util/zip/Inflater",
            "libcore/icu/LocaleData",
            "libcore/icu/NativeDecimalFormat$FieldPositionIterator",
            "libcore/io/ErrnoException",
            "libcore/io/GaiException",
            "libcore/io/StructAddrinfo",
            "libcore/io/StructFlock",
            "libcore/io/StructGroupReq",
            "libcore/io/StructLinger",
            "libcore/io/StructPasswd",
            "libcore/io/StructPollfd",
            "libcore/io/StructStat",
            "libcore/io/StructStatVfs",
            "libcore/io/StructTimeval",
            "libcore/io/StructUcred",
            "libcore/io/StructUtsname",
            "libcore/util/MutableInt",
            "libcore/util/MutableLong",
            "aura/rt/bro/Struct"
    };


    public static final String TRUSTED_CERTIFICATE_STORE_CLASS =
            "com/android/org/conscrypt/TrustedCertificateStore";

}
