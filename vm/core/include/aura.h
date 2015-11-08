/*
 * Copyright (C) 2012 RoboVM AB
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
#ifndef AURA_H
#define AURA_H

#ifdef AURA_CORE_BUILD
#   ifndef GC_THREADS
#       define GC_THREADS
#   endif
#   include <gc.h>
#endif

#include <stdlib.h>
#include <stdint.h>
#include <stdio.h>
#include <stdarg.h>
#include <assert.h>

// Keep assert() even in release builds but just abort.
#ifdef NDEBUG
#   ifdef assert
#       undef assert
#   endif
#   define assert(e) ((void) ((e) ? 0 : rvmAbort(NULL)))
#endif

#ifdef __cplusplus
extern "C" {
#endif

#include "aura/types.h"
#include "aura/bitvector.h"
#include "aura/access.h"
#include "aura/atomic.h"
#include "aura/init.h"
#include "aura/memory.h"
#include "aura/method.h"
#include "aura/field.h"
#include "aura/class.h"
#include "aura/array.h"
#include "aura/exception.h"
#include "aura/string.h"
#include "aura/thread.h"
#include "aura/attribute.h"
#include "aura/native.h"
#include "aura/proxy.h"
#include "aura/log.h"
#include "aura/trycatch.h"
#include "aura/mutex.h"
#include "aura/monitor.h"
#include "aura/signal.h"
#include "aura/hooks.h"
#include "aura/rt.h"
#include "aura/lazy_helpers.h"

#ifdef __cplusplus
}
#endif

#endif

