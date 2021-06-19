package dev.benndorf.dumper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.net.ssl.HttpsURLConnection;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public class Dumper {

  public static void main(final String[] args) throws IOException {
    final String parent = "D:\\IntellijProjects\\Paper117\\Paper-Server\\build\\libs\\";
    final String paperInput = parent + "Paper-Server-reobf.jar";
    final String spigotInput = parent + "spigot-1.17.jar";
    final String paperOutput = parent + "Paper.dump.txt";
    final String spigotOutput = parent + "Spigot.dump.txt";
    final String packageName = "net/minecraft";
    final String mojangMappings = "https://launcher.mojang.com/v1/objects/84d80036e14bc5c7894a4fad9dd9f367d3000334/server.txt";

//    filterMojangMappings(mojangMappings);

    Files.writeString(Path.of(paperOutput), dump(paperInput, packageName).collect(Collectors.joining()));
    Files.writeString(Path.of(spigotOutput), dump(spigotInput, packageName).collect(Collectors.joining()));
  }

  private static Stream<String> dump(final String path, final String packageName) throws IOException {
    final JarFile jarFile = new JarFile(path);
    return jarFile.stream()
      .filter(e -> e.getName().endsWith(".class"))
      .filter(e -> e.getName().startsWith(packageName))
      .filter(e -> !e.getName().contains("package-info") && !e.getName().contains("FieldsAreNonnullByDefault") && !e.getName().contains("MethodsReturnNonnullByDefault"))
      .map(e -> {
        final ClassNode classNode = new ClassNode();
        try(final InputStream classFileInputStream = jarFile.getInputStream(e)) {
          final ClassReader classReader = new ClassReader(classFileInputStream);
          classReader.accept(classNode, 0);
          return classNode;
        } catch(final Exception ex) {
          ex.printStackTrace();
          return null;
        }
      }).filter(Objects::nonNull)
      .sorted(Comparator.comparing(c -> c.name))
      .map(Dumper::dump);
  }

  private static String dump(final ClassNode node) {
    final Type objectType = Type.getObjectType(node.name);
    final String className = objectType.getInternalName();

    final StringBuilder sb = new StringBuilder();
    sb.append("Class: ").append(className).append("\n");

    sb.append("\tFields:\n");
    node.fields.stream()
      .filter(f -> !f.name.startsWith("this$"))
      .filter(f -> !f.name.startsWith("ENUM$VALUES"))
      .filter(f -> !f.name.startsWith("val$"))
      .filter(f -> !f.name.startsWith("$SWITCH_TABLE$"))
      .filter(f -> !f.name.startsWith("$SwitchMap$"))
      .sorted(Comparator.comparing(f -> f.name))
      .forEach(f -> sb.append("\t\t").append(dump(f)).append("\n"));

    sb.append("\tMethods:\n");
    node.methods.stream()
//      .filter(m -> !m.name.startsWith("lambda$"))
//      .filter(m -> m.signature != null) // TODO try to filter out lambdas
      .filter(m -> !m.name.equals("<init>") && !m.name.equals("<clinit>"))
      .sorted(Comparator.comparing(m -> m.name))
      .forEach(m -> sb.append("\t\t").append(dump(m)).append("\n"));

    return sb.toString();
  }

  private static String dump(final FieldNode node) {
    final Type type = Type.getType(node.desc);
    return node.name + "\t" + cleanType(type.getClassName());
  }

  private static String dump(final MethodNode node) {
    final Type returnType = Type.getReturnType(node.desc);
    final Type[] argumentTypes = Type.getArgumentTypes(node.desc);
    final String formattedArgs = Arrays.stream(argumentTypes).map(Type::getClassName).map(Dumper::cleanType).collect(Collectors.joining(", ", "[", "]"));
    return node.name + "\t" + cleanType(returnType.getClassName()) + "\t" + formattedArgs;
  }

  private static String cleanType(final String type) {
    return type.replace("org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil", "it.unimi.dsi.fastutil");
  }

//  private static String filterMojangMappings(String url) throws {
//    URLConnection urlConnection = new URL(url).openConnection();
//    urlConnection.connect();
//    InputStream responseStream = connection.getInputStream();
//  }
}
