package util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class FileReader {

    private static FileReader instance;

    private FileReader() {
    }

    public static FileReader getInstance() {
        if (instance == null) {
            instance = new FileReader();
        }
        return instance;
    }

    public List<String> readLines(String path) throws IOException {
        final InputStreamReader reader = new InputStreamReader(new FileInputStream(path));
        return reader.readAllLines();
    }
}
