package psp.std;

import java.net.*;
import java.io.*;
import java.util.jar.*;
import java.util.Enumeration;
import java.util.Set;
import java.util.HashSet;

public class Resources {
  public static String[] getResourceNames(ClassLoader cl, java.nio.file.Path p) throws URISyntaxException, IOException {
    String path = p + "/";
    URL dirURL = cl.getResource(path);
    if (dirURL != null && dirURL.getProtocol().equals("file")) {
      /* A file path: easy enough */
      return new File(dirURL.toURI()).list();
    }
    if (dirURL != null && dirURL.getProtocol().equals("jar")) {
      return getResourcesFromJar(path, dirURL);
    }

    throw new UnsupportedOperationException("Cannot list files for URL "+dirURL);
  }

  /**
   * List directory contents for a resource folder. Not recursive.
   * This is basically a brute-force implementation.
   * Works for regular files and also JARs.
   *
   * @author Greg Briggs
   * @param clazz Any java class that lives in the same place as the resources you want.
   * @param path Should end with "/", but not start with one.
   * @return Just the name of each member item, not the full paths.
   * @throws URISyntaxException
   * @throws IOException
   */
  public static String[] getResourceNames(Class<?> clazz, String path) throws URISyntaxException, IOException {
    URL dirURL = clazz.getClassLoader().getResource(path);
    if (dirURL != null && dirURL.getProtocol().equals("file")) {
      /* A file path: easy enough */
      return new File(dirURL.toURI()).list();
    }

    if (dirURL == null) {
      /*
       * In case of a jar file, we can't actually find a directory.
       * Have to assume the same jar as clazz.
       */
      String me = clazz.getName().replace(".", "/")+".class";
      dirURL = clazz.getClassLoader().getResource(me);
    }

    if (dirURL.getProtocol().equals("jar")) {
      return getResourcesFromJar(path, dirURL);
    }

    throw new UnsupportedOperationException("Cannot list files for URL "+dirURL);
  }

  private static String[] getResourcesFromJar(String path, URL dirURL) throws IOException, UnsupportedEncodingException {
    /* A JAR path */
    String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!")); //strip out only the JAR file
    JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
    try {
      Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
      Set<String> result = new HashSet<String>(); //avoid duplicates in case it is a subdirectory
      while(entries.hasMoreElements()) {
        String name = entries.nextElement().getName();
        if (name.startsWith(path)) { //filter according to the path
          String entry = name.substring(path.length());
          int checkSubdir = entry.indexOf("/");
          if (checkSubdir >= 0) {
            // if it is a subdirectory, we just return the directory name
            entry = entry.substring(0, checkSubdir);
          }
          result.add(entry);
        }
      }
      return result.toArray(new String[result.size()]);
    } finally {
      jar.close();
    }
  }
}
