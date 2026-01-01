public class diary {
    import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.*;

    public class DiaryManager {

        private static final Path ENTRY_DIR = Paths.get("entries");
        private static final Path CONFIG_FILE = Paths.get("diary_config.ser");
        private static Scanner scanner = new Scanner(System.in);

        public static void main(String[] args) {

            try {
                if (!Files.exists(ENTRY_DIR)) {
                    Files.createDirectory(ENTRY_DIR);
                }
            } catch (IOException e) {
                System.out.println("Error creating entries folder.");
            }

            loadConfig();

            while (true) {
                System.out.println("\n=== PERSONAL DIARY MANAGER ===");
                System.out.println("1. Write Entry");
                System.out.println("2. Read Entry");
                System.out.println("3. Search Entry");
                System.out.println("4. Backup Entries (ZIP)");
                System.out.println("5. Exit");
                System.out.print("Choose option: ");

                int choice = scanner.nextInt();
                scanner.nextLine();

                switch (choice) {
                    case 1 -> writeEntry();
                    case 2 -> readEntry();
                    case 3 -> searchEntry();
                    case 4 -> backupEntries();
                    case 5 -> {
                        saveConfig();
                        System.out.println("Goodbye!");
                        return;
                    }
                    default -> System.out.println("Invalid option.");
                }
            }
        }

        // ================= WRITE MODE =================
        private static void writeEntry() {
            try {
                System.out.println("Write your diary entry:");
                String text = scanner.nextLine();

                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter format =
                        DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");

                String fileName = "diary_" + now.format(format) + ".txt";
                Path filePath = ENTRY_DIR.resolve(fileName);

                BufferedWriter writer = Files.newBufferedWriter(filePath);
                writer.write(text);
                writer.close();

                System.out.println("Entry saved: " + fileName);

            } catch (IOException e) {
                System.out.println("Error saving diary entry.");
            }
        }

        // ================= READ MODE =================
        private static void readEntry() {
            try {
                List<Path> files = Files.list(ENTRY_DIR).toList();

                if (files.isEmpty()) {
                    System.out.println("No diary entries found.");
                    return;
                }

                for (int i = 0; i < files.size(); i++) {
                    System.out.println((i + 1) + ". " + files.get(i).getFileName());
                }

                System.out.print("Select entry number: ");
                int choice = scanner.nextInt();
                scanner.nextLine();

                Path selected = files.get(choice - 1);
                BufferedReader reader = Files.newBufferedReader(selected);

                System.out.println("\n--- Diary Entry ---");
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
                reader.close();

            } catch (Exception e) {
                System.out.println("Error reading entry.");
            }
        }

        // ================= SEARCH =================
        private static void searchEntry() {
            try {
                System.out.print("Enter keyword to search: ");
                String keyword = scanner.nextLine().toLowerCase();

                boolean found = false;

                for (Path file : Files.list(ENTRY_DIR).toList()) {
                    BufferedReader reader = Files.newBufferedReader(file);
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.toLowerCase().contains(keyword)) {
                            System.out.println("Found in: " + file.getFileName());
                            found = true;
                            break;
                        }
                    }
                    reader.close();
                }

                if (!found) {
                    System.out.println("No entries match your search.");
                }

            } catch (IOException e) {
                System.out.println("Search error.");
            }
        }

        // ================= BACKUP ZIP =================
        private static void backupEntries() {
            try {
                ZipOutputStream zip =
                        new ZipOutputStream(new FileOutputStream("diary_backup.zip"));

                for (Path file : Files.list(ENTRY_DIR).toList()) {
                    ZipEntry entry = new ZipEntry(file.getFileName().toString());
                    zip.putNextEntry(entry);
                    Files.copy(file, zip);
                    zip.closeEntry();
                }

                zip.close();
                System.out.println("Backup created: diary_backup.zip");

            } catch (IOException e) {
                System.out.println("Backup failed.");
            }
        }

        // ================= SERIALIZATION =================
        private static void saveConfig() {
            try (ObjectOutputStream out =
                         new ObjectOutputStream(new FileOutputStream(CONFIG_FILE.toFile()))) {
                out.writeObject("Diary Config Saved");
            } catch (IOException ignored) {}
        }

        private static void loadConfig() {
            if (!Files.exists(CONFIG_FILE)) return;
            try (ObjectInputStream in =
                         new ObjectInputStream(new FileInputStream(CONFIG_FILE.toFile()))) {
                in.readObject();
            } catch (Exception ignored) {}
        }
    }

}
