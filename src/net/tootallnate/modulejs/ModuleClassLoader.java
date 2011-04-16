import java.awt.event.*;
import java.io.*;
import java.lang.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;

/**
 * The 'ModuleClassLoader' is an asynchronous JAR class loader. It can
 * retrieve and load classes from a JAR file over any valid java.net.URL
 * instance that is compatible with the `openConnection()` method.
 *
 * It's async, so you must first add a "Load Listener", which can be any
 * ActionListener instance, in order to be notified when it's ready to be
 * used. Then call `retrieve()`, it returns immediately and begins
 * asynchronously getting the JAR file contents in a new Thread.
 *
 * After the ActionListeners have been notified (i.e. `isLoaded()` returns
 * true), it is safe to call the `loadClass()` method and do your bidding.
 *
 * For ModuleJS, there's a `getModuleClassName()` method, which returns
 * the "Module-Class" attribute from the JAR's manifest file. This class
 * name will be the entry point for a native Java ModuleJS module. The
 * class must contain an initialization method with the signature:
 *  
 */
public class ModuleClassLoader extends ClassLoader implements Runnable {

  private final URL jarUrl;
  private final ArrayList<ActionListener> loadListeners;
  private byte[] jarData;
  private boolean loaded;

  public ModuleClassLoader(URL jarUrl) {
    if (jarUrl == null) {
      throw new IllegalArgumentException ("A URL pointing to a ModuleJS JAR file is required");
    }
    this.jarUrl = jarUrl;
    this.loadListeners = new ArrayList<ActionListener>();
    this.loaded = false;
  }

  public void addLoadListener(ActionListener l) {
    this.loadListeners.add(l);
  }

  public void removeLoadListener(ActionListener l) {
    this.loadListeners.remove(l);
  }

  public boolean isLoaded() {
    return this.loaded;
  }

  // Begins asynchronously getting the JAR file's byte data.
  // Use `addLoadListener()` to be notified when the file has
  // finished loading, and classes inside the JAR may begin being
  // loaded and used.
  public void retrieve() {
    new Thread(this).start();
  }

  // The ModuleClassLoader implements Runnable in order to asynchronously
  // retrieve the JAR file contents. ActionListeners may be attached via
  // 'addLoadListener()' to be notified when the 'loadClass()' method may
  // be safely called.
  public void run() {

    byte[] data = null;
    try {
      // First we have to open a connection to retrieve the file
      URLConnection connection = this.jarUrl.openConnection();
      // Since you get a URLConnection, use it to get the InputStream
      InputStream in = connection.getInputStream();
      // Now that the InputStream is open, get the content length
      int contentLength = connection.getContentLength();

      // To avoid having to resize the array over and over and over as
      // bytes are written to the array, provide an accurate estimate of
      // the ultimate size of the byte array
      ByteArrayOutputStream tmpOut;
      if (contentLength != -1) {
        System.out.println("Content Length: " + contentLength);
        tmpOut = new ByteArrayOutputStream(contentLength);
      } else {
        // If the server provided no Content-Length, allocate a
        // byte array of 1 megabyte. Any bigger of a JAR file would be
        // kind of ridiculous for a ModuleJS native module.
        System.out.println("No content length!");
        tmpOut = new ByteArrayOutputStream(1048576);
      }

      byte[] buf = new byte[512];
      while (true) {
        int len = in.read(buf);
        if (len == -1) {
          break;
        }
        tmpOut.write(buf, 0, len);
      }
      in.close();
      tmpOut.close(); // No effect, but good to do anyway to keep the metaphor alive

      data = tmpOut.toByteArray();
      System.out.println("'data.length': " + data.length);
    } catch (IOException ex) {
      ex.printStackTrace();
      return; // TODO: Error reporting and handling
    }

    synchronized(this) {
      this.loaded = true;
      this.jarData = data;
      for (ActionListener l : this.loadListeners) {
        ActionEvent e = new ActionEvent(this, 1, "load");
        l.actionPerformed(e);
      }
    }
  }

  /**
   * Returns the "Module-Class" manifest attributes from the
   * retrieved JAR file, or null if none was defined.
   */
  public String getModuleClassName() throws IOException {
    System.out.println("Attempting to get 'Module-Class' manifest attribute");

    JarInputStream jar = new JarInputStream(new ByteArrayInputStream(this.jarData));
    Manifest manifest = jar.getManifest();
    Attributes attr = manifest.getMainAttributes();
    String rtn = attr != null ? attr.getValue("Module-Class") : null;
    jar.close();
    return rtn;
  }

  protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
    //System.out.println("Loading class: " + name + ", resolve: " + resolve);

    // Since all support classes of loaded class use same class loader
    // must check subclass cache of classes for things like Object
    Class c = findLoadedClass(name);
    if (c == null) {
      try {
        c = findSystemClass(name);
      } catch (ClassNotFoundException e) {
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    if (c == null) {
      try {
        byte data[] = loadClassData(name);
        c = defineClass (name, data, 0, data.length);
        if (c == null)
          throw new ClassNotFoundException (name);
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    }
    if (resolve)
      resolveClass (c);
    return c;
  }

  private byte[] loadClassData (String classname) throws IOException {

    JarInputStream jar = new JarInputStream(new ByteArrayInputStream(this.jarData));

    // Convert class name argument to filename
    // Convert package names into subdirectories
    String filename = classname.replace ('.', File.separatorChar) + ".class";
    System.out.println("Attempting to find: " + filename);

    JarEntry entry = null;
    ByteArrayOutputStream bos = null;
    while ((entry = jar.getNextJarEntry()) != null) {
      System.out.println("Next Jar entry: " + entry.getName());
      if (!filename.equals(entry.getName())) continue;

      bos = new ByteArrayOutputStream();
      byte[] buff = new byte[8124];
      while (true) {
        int read = jar.read(buff, 0, buff.length);
        if (read == -1) {
          break;
        }
        bos.write(buff, 0, read);
      }
      break;

    }
    jar.close();

    if (bos == null) throw new IOException("File not found in JAR: " + filename);

    return bos.toByteArray();
  }
}
