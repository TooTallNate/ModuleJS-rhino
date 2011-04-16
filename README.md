ModuleJS-rhino
==============
### Asynchronous [CommonJS][] Module Loader for [Rhino][] JavaScript.

This library allows the use of "modules" within your Rhino-embedded Java programs.
It's the Java version of the web-browser [ModuleJS][] library.

Any top-level JavaScript module that provides direct JavaScript functionality
may be loaded (like Underscore, for example). Additionally, this Java version
allows for "native" Java modules to be loaded, and extend the functionality of the
JavaScript environment.


Writing a JavaScript Module
---------------------------

JavaScript modules are the same as the web-browser [ModuleJS][]. More than likely,
your Java program is going to execute user-written JavaScript files, and they should
be in the standard ModuleJS module format. For reference, here's a JavaScript
module that exports a single function, 'hello', that returns the "Hello World!" string:

    exports.hello = function() {
      return "Hello World!";
    }


Writing a Java Module
---------------------

A "native" Java module works a little differently. Knowledge of [Rhino][]'s
embedding APIs is a plus. A native module simply needs to define a public static "Init"
method, which will be called when the class is loaded. The method should return
a _Scriptable_ that will be the native module's _exports_ object.

Let's see a Java module that exports a 'native' function called 'hello' that will
return the "Hello World!" string:

    import java.lang.reflect.Method;
    import org.mozilla.javascript.*;

    public class HelloModule {

      // 'Init' sets up the module initially. It creates the 'exports' object
      // and attaches the 'hello' function to the object.
      public static Scriptable Init (Context cx, Scriptable thisObj, Object[] args, Function funObj) {

        // Set up a new, empty, Object.
        Scriptable exports = new ScriptableObject();
        
        // Wrap the 'Hello' function defined below, and expose it on the exports object
        Method helloMethod = HelloModule.class.getMethod("Hello", new Class[] { Context.class, Scriptable.class, Object[].class, Function.class });
        Function helloFunction = new FunctionObject("hello", helloMethod, funObj);
        ScriptableObject.putProperty(exports, "hello", helloFunction);

        return exports;
      }

      public static String Hello (Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        return "Hello World!";
      }
    }



[ModuleJS]: https://github.com/TooTallNate/ModuleJS
[CommonJS]: http://wiki.commonjs.org/wiki/Modules
[Rhino]:    http://www.mozilla.org/rhino
