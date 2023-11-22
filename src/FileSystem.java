import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Stack;

public class FileSystem {

    public static String loadFile(String filepath) {
        try {
            return Files.readString(Path.of(filepath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveFile(String file, String data) {
        try {
            Path path = Path.of(file);
            if (Files.notExists(path.getParent())) Files.createDirectories(path.getParent());
            Files.writeString(path, data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String[] getDirectoryFiles(String dir) {
        // TODO: rewrite using `File` class
        try {
            Stack<String> list = new Stack<>();
            Files.list(Path.of(dir))
                 .filter(f -> {
                     if (Files.isDirectory(f)) return false;
                     String temp = String.valueOf(f.getFileName());
                     return temp.endsWith(".bf") || temp.endsWith(".bfn");
                 })
                 .forEach(f -> list.push(String.valueOf(f.getFileName())));
            return list.toArray(new String[0]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean delete(String path) {
        boolean success = true;
        File    file    = new File(path);
        if (file.isDirectory()) for (File f: file.listFiles()) success &= recursiveDelete(f);
        return file.delete() && success;
    }

    public static boolean recursiveDelete(File file) {
        boolean success = true;
        if (file.isDirectory()) for (File f: file.listFiles()) success &= recursiveDelete(f);
        return file.delete() && success;
    }

}
