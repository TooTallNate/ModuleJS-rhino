import java.lang.reflect.Method;
import org.mozilla.javascript.*;

public class HelloModule {

  // 'Init' sets up the module initially. It creates the 'exports',
  // which is what gets exposes back to the JavaScript script writer.
  public static Scriptable Init (Context cx, Scriptable moduleScope) throws NoSuchMethodException {

    // Set up a new, empty, Object.
    Scriptable exports = cx.newObject(moduleScope);

    // Wrap the 'Hello' function defined below, and expose it on the exports object
    Method helloMethod = HelloModule.class.getMethod("Hello", new Class[] { Context.class, Scriptable.class, Object[].class, Function.class });
    FunctionObject helloFunction = new FunctionObject("hello", helloMethod, moduleScope);
    ScriptableObject.putProperty(exports, "hello", helloFunction);

    // Demonstrating also exporting a String, "hw".
    ScriptableObject.putProperty(exports, "helloStr", "hw");

    return exports;
  }

  // The 'Hello' method is turned into a JavaScript Function instance in our
  // 'Init' method. This makes it so the code in this method is called when the
  // function is invoked.
  public static String Hello (Context cx, Scriptable thisObj, Object[] args, Function funObj) {
    return "Hello World";
  }
}
