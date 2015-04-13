#if defined(WIN32) || defined(_WIN32)

#ifndef WINVER
#define WINVER 0x0502
#endif

#ifndef _WIN32_WINNT
#define _WIN32_WINNT 0x0502
#endif

#if defined(DEBUG) || defined(_DEBUG)
//Please, read more at "Finding Memory Leaks Using the CRT Library":
// http://msdn.microsoft.com/en-us/library/x98tx3cf.aspx
#include <crtdbg.h>
#define CRASH _CrtDbgBreak
#endif //WIN32-debug

#endif //WIN32

#include <jni.h>
#include <config.h>

#ifdef __cplusplus

#include <runtime/JSExportMacros.h>

#include <wtf/Platform.h>
#include <wtf/Assertions.h>
//#include <wtf/CurrentTime.h>
//#include <wtf/DateMath.h>
#include <wtf/Deque.h>
#include <wtf/DisallowCType.h>
#include <wtf/Forward.h>
#include <wtf/GetPtr.h>
#include <wtf/HashCountedSet.h>
#include <wtf/HashFunctions.h>
#include <wtf/HashMap.h>
#include <wtf/HashSet.h>
#include <wtf/HashTable.h>
#include <wtf/HashTraits.h>
#include <wtf/ListHashSet.h>
#include <wtf/MathExtras.h>
#include <wtf/Noncopyable.h>
#include <wtf/OwnPtr.h>
#include <wtf/OwnPtrCommon.h>
#include <wtf/PassOwnPtr.h>
#include <wtf/PassRefPtr.h>
#include <wtf/RefCounted.h>
#include <wtf/RefPtr.h>
#include <wtf/StringExtras.h>
#include <wtf/TCPageMap.h>
#include <wtf/TCSystemAlloc.h>
#include <wtf/Vector.h>
#include <wtf/VectorTraits.h>

#include <wtf/unicode/Collator.h>
#include <wtf/unicode/UTF8.h>

#if USE(JAVA_UNICODE)
#include <wtf/unicode/java/UnicodeJava.h>
#endif

#ifndef SKIP_STATIC_CONSTRUCTORS_ON_GCC
#include <JavaScriptCore/APICast.h>
#include <JavaScriptCore/JavaScript.h>
#include <JavaScriptCore/JSBase.h>
#include <JavaScriptCore/JSContextRef.h>
#include <JavaScriptCore/JSObjectRef.h>
#include <JavaScriptCore/JSRetainPtr.h>
#include <JavaScriptCore/JSStringRef.h>
#include <JavaScriptCore/JSValueRef.h>
#include <JavaScriptCore/OpaqueJSString.h>

#include <bytecode/CodeBlock.h>
#include <bytecode/Opcode.h>

#include <debugger/Debugger.h>

#include <interpreter/CallFrame.h>
#include <interpreter/Interpreter.h>
#include <interpreter/Register.h>

#include <jit/JIT.h>
#include <jit/JITCode.h>

#include <parser/Lexer.h>
#include <parser/NodeInfo.h>
#include <parser/Nodes.h>
#include <parser/Parser.h>
#include <parser/ResultType.h>
#include <parser/SourceCode.h>
#include <parser/SourceProvider.h>

#include <runtime/ArgList.h>
#include <runtime/Arguments.h>
#include <runtime/ArrayPrototype.h>
#include <runtime/BooleanConstructor.h>
#include <runtime/BooleanObject.h>
#include <runtime/BooleanPrototype.h>
#include <runtime/CallData.h>
#include <runtime/Completion.h>
#include <runtime/DateInstance.h>
#include <runtime/DatePrototype.h>
#include <runtime/Error.h>
#include <runtime/ErrorConstructor.h>
#include <runtime/ErrorInstance.h>
#include <runtime/ErrorPrototype.h>
#include <runtime/FunctionConstructor.h>
#include <runtime/FunctionPrototype.h>
#include <runtime/GetterSetter.h>
//#include <runtime/GlobalEvalFunction.h>
#include <runtime/InternalFunction.h>
#include <runtime/JSActivation.h>
#include <runtime/JSArray.h>
//#include <runtime/JSByteArray.h>
#include <runtime/JSCell.h>
#include <runtime/JSFunction.h>
#include <runtime/VM.h>
#include <runtime/JSGlobalObject.h>
#include <runtime/JSGlobalObjectFunctions.h>
//#include <runtime/JSImmediate.h>
#include <runtime/JSLock.h>
#include <runtime/JSNotAnObject.h>
//#include <runtime/JSNumberCell.h>
#include <runtime/JSONObject.h>
#include <runtime/JSObject.h>
#include <runtime/JSPropertyNameIterator.h>
#include <runtime/JSString.h>
#include <runtime/JSType.h>
#include <runtime/JSObject.h>
#include <runtime/JSVariableObject.h>
#include <runtime/JSWrapperObject.h>
#include <runtime/Lookup.h>
#include <runtime/MathObject.h>
#include <runtime/NumberConstructor.h>
#include <runtime/NumberObject.h>
#include <runtime/NumberPrototype.h>
#include <runtime/ObjectConstructor.h>
#include <runtime/ObjectPrototype.h>
#include <runtime/RegExp.h>
#include <runtime/StringConstructor.h>
#include <runtime/StringObject.h>
#include <runtime/StringPrototype.h>
#include <runtime/Structure.h>
#endif


#endif
