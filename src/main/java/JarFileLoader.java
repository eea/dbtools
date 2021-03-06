import java.net.URI;
import java.net.URL;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.net.MalformedURLException;

public final class JarFileLoader {

    /**
     * Constructor. This is a utility class.
     */
    private JarFileLoader() {
    }

    /**
     * Add on JAR file to the Class loader.
     *
     * @param s - the file name of the jar file.
     */
    public static void addPath(String s) throws Exception {
        File f = new File(s);
        URI u = f.toURI();
        URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class<URLClassLoader> urlClass = URLClassLoader.class;
        Method method = urlClass.getDeclaredMethod("addURL", new Class[]{URL.class});
        method.setAccessible(true);
        method.invoke(urlClassLoader, new Object[]{u.toURL()});
    }

    /**
     * Parse a class path string with colon or semicolon delimiters.
     *
     * @param pathLine - The unsplit references to jar files.
     */
    public static void addPaths(String pathLine) {
        if (pathLine == null || "".equals(pathLine)) {
            return;
        }
        try {
            String[] paths = pathLine.split("[:;]");
            for (String path : paths) {
                if (!"".equals(path)) {
                    addPath(path);
                }
            }
        } catch (MalformedURLException e) {
            System.out.println("File not found");
        } catch (Exception e) {
            System.out.println("File not found");
        }
    }
}
