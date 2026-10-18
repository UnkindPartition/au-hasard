package info.ro_che.auhasard;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * List items and saved lists. A list is entered as text, one item per line. Saved lists are
 * stored one item per line, separated by blank lines; items are trimmed and never empty, so
 * this is unambiguous.
 */
final class Lists {
    private Lists() {}

    /** The non-empty, trimmed lines of {@code text}. */
    static List<String> items(String text) {
        List<String> result = new ArrayList<>();
        for (String line : text.split("\n")) {
            String item = line.trim();
            if (!item.isEmpty()) {
                result.add(item);
            }
        }
        return result;
    }

    static List<List<String>> load(File file) throws IOException {
        List<List<String>> lists = new ArrayList<>();
        if (!file.exists()) {
            return lists;
        }
        String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        for (String block : text.split("\n\n")) {
            List<String> items = items(block);
            if (!items.isEmpty()) {
                lists.add(items);
            }
        }
        return lists;
    }

    static void store(File file, List<List<String>> lists) throws IOException {
        StringBuilder text = new StringBuilder();
        for (List<String> items : lists) {
            for (String item : items) {
                text.append(item).append('\n');
            }
            text.append('\n');
        }
        File tmp = new File(file.getPath() + ".tmp");
        Files.write(tmp.toPath(), text.toString().getBytes(StandardCharsets.UTF_8));
        if (!tmp.renameTo(file)) {
            throw new IOException("cannot replace " + file);
        }
    }
}
