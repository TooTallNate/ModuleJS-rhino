import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.io.IOException;

import org.mozilla.javascript.*;


public class CLTester implements ActionListener {

  private static ModuleClassLoader l;

  public void actionPerformed(ActionEvent e) {
    System.out.println("Got 'action' event!");
    System.out.println("Source is the ModuleClassLoader instance? " + (l == e.getSource()));

    String moduleClass = null;
    try {
      moduleClass = l.getModuleClassName();
    } catch (IOException ex) {
      ex.printStackTrace();
      return;
    }
    System.out.println("Module-Class: " + moduleClass);

    // Creates and enters a Context. The Context stores information
    // about the execution environment of a script.
    Context cx = Context.enter();
    try {
      // Initialize the standard objects (Object, Function, etc.)
      // This must be done before scripts can be executed. Returns
      // a scope object that we use in later calls.
      Scriptable scope = cx.initStandardObjects();


      Class<?> c = l.loadClass(moduleClass);
      Method m = c.getMethod("Init", new Class[] { Context.class, Scriptable.class });
      m.setAccessible(true);
      int mods = m.getModifiers();
      if (m.getReturnType() != Scriptable.class || !Modifier.isStatic(mods) || !Modifier.isPublic(mods)) {
        //throw new NoSuchMethodException("Init");
        System.out.println("No 'Init' method found!");
        return;
      }
      Scriptable exports = null;
      try {
        exports = (Scriptable)m.invoke(null, new Object[] { cx, scope });
      } catch (Exception ex) {
        // This should not happen, as we have disabled access checks in `setAccessible()`
        ex.printStackTrace();
      }


      // Create the module
      ScriptableObject.putProperty(scope, "module", exports);

      // Now evaluate the string we've colected.
      Object result = cx.evaluateString(scope, "module.hello.call({test:new Date()});", "<cmd>", 1, null);

      // Convert the result to a string and print it.
      System.err.println(Context.toString(result));

    } catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      // Exit from the context.
      Context.exit();
    }


  }

  public static void main (String args[]) throws Exception {
    CLTester test = new CLTester();
    l = new ModuleClassLoader(new URL(args[0]));
    l.addLoadListener(test);
    l.retrieve();
  }
}
