package runi.myddns.mobarmywars.Utils;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import runi.myddns.mobarmywars.MobArmyMain;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;

public class PluginFileManager {

    private final MobArmyMain plugin;

    public PluginFileManager(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    public void checkFilesOnStartup() {
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(
                ConsoleColor.COPPER + "        Prüfe Plugin-Dateien..." + ConsoleColor.RESET);
        Bukkit.getConsoleSender().sendMessage("");

        String newestTemplateZip = findNewestTemplateZipInJar();

        if (newestTemplateZip != null) {
            checkTemplateZipByVersion(newestTemplateZip);
        } else {
            plugin.getLogger().warning("Keine world_mobarmy V*.zip in der Plugin-JAR gefunden!");
        }

        Bukkit.getConsoleSender().sendMessage("");

        checkYamlFile("arena-koordinaten.yml");
        checkYamlFile("config.yml");
        checkYamlFile("eventdaten.yml");
        createEmptyFileIfMissing("mobData.yml");
        createEmptyFileIfMissing("scoreboard.yml");
        checkYamlFile("spawns.yml");
        createEmptyFileIfMissing("teams.yml");
        checkYamlFile("team-equipment.yml");
        checkYamlFile("waves.yml");
        checkYamlFile("worldsettings.yml");
        checkYamlFile("languages/de.yml");
        checkYamlFile("languages/en.yml");

        extractServerIconIfMissing();

        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage("");
    }

    private String findNewestTemplateZipInJar() {
        try {
            File jarFile = new File(plugin.getClass()
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());

            try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile)) {
                String newestFileName = null;
                String newestVersion = null;

                java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();

                while (entries.hasMoreElements()) {
                    java.util.jar.JarEntry entry = entries.nextElement();
                    String name = entry.getName();

                    if (!name.startsWith("world_mobarmy V") || !name.endsWith(".zip")) {
                        continue;
                    }

                    String version = extractVersionFromZipName(name, "world_mobarmy");
                    if (version == null) continue;

                    if (newestVersion == null || compareVersions(version, newestVersion) > 0) {
                        newestVersion = version;
                        newestFileName = name;
                    }
                }

                return newestFileName;
            }

        } catch (Exception e) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Failed to find the template ZIP in the plugin JAR.",
                    e
            );
            return null;
        }
    }

    private void checkTemplateZipByVersion(String resourceFileName) {
        String baseName = "world_mobarmy";
        File dataFolder = plugin.getDataFolder();

        String resourceVersion = extractVersionFromZipName(resourceFileName, baseName);
        if (resourceVersion == null) {
            plugin.getLogger().warning("Konnte Version aus ZIP nicht lesen: " + resourceFileName);
            return;
        }

        if (plugin.getResource(resourceFileName) == null) {
            plugin.getLogger().warning("Resource nicht gefunden: " + resourceFileName);
            return;
        }

        File[] existingZips = dataFolder.listFiles((_, name) ->
                name.startsWith(baseName + " V") && name.endsWith(".zip")
        );

        File newestExistingFile = null;
        String newestExistingVersion = null;

        if (existingZips != null) {
            for (File file : existingZips) {
                String version = extractVersionFromZipName(file.getName(), baseName);
                if (version == null) continue;

                if (newestExistingVersion == null || compareVersions(version, newestExistingVersion) > 0) {
                    newestExistingVersion = version;
                    newestExistingFile = file;
                }
            }
        }

        File targetFile = new File(dataFolder, resourceFileName);

        if (newestExistingFile == null) {
            copyResource(resourceFileName, targetFile);
            printFileStatus(resourceFileName, "erstellt.");
            return;
        }

        if (compareVersions(resourceVersion, newestExistingVersion) > 0) {
            backupFile(newestExistingFile, newestExistingFile.getName());

            copyResource(resourceFileName, targetFile);

            if (!newestExistingFile.equals(targetFile) && newestExistingFile.exists()) {
                if (!newestExistingFile.delete()) {
                    plugin.getLogger().warning("⚠ Alte Template-ZIP konnte nicht gelöscht werden: " + newestExistingFile.getName());
                }
            }

            printFileStatus(resourceFileName, "aktualisiert.");
            return;
        }

        printFileStatus(newestExistingFile.getName(), "I.O.");
    }

    private int compareVersions(String v1, String v2) {
        v1 = v1.replace("V", "").replace("v", "");
        v2 = v2.replace("V", "").replace("v", "");

        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int length = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < length; i++) {
            int num1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int num2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;

            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }

        return 0;
    }

    private int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String extractVersionFromZipName(String fileName, String baseName) {
        String prefix = baseName + " V";
        String suffix = ".zip";

        if (!fileName.startsWith(prefix) || !fileName.endsWith(suffix)) {
            return null;
        }

        return fileName.substring(prefix.length(), fileName.length() - suffix.length());
    }

    private void checkYamlFile(String fileName) {
        File targetFile = new File(plugin.getDataFolder(), fileName);

        if (plugin.getResource(fileName) == null) {
            plugin.getLogger().warning("Resource nicht gefunden: " + fileName);
            return;
        }

        if (!targetFile.exists()) {
            plugin.saveResource(fileName, false);
            printFileStatus(fileName,  "erstellt.");
            return;
        }

        int currentVersion = getFileVersion(targetFile);
        int newestVersion = getResourceVersion(fileName);

        if (newestVersion <= 0) {
            plugin.getLogger().warning(fileName + " hat in der Plugin-JAR keine gültige file-version.");
            return;
        }

        if (currentVersion < newestVersion) {
            backupFile(targetFile, fileName);
            overwriteResource(fileName, targetFile);

            printFileStatus(fileName,  "aktualisiert.");
            return;
        }

        printFileStatus(fileName,  "I.O.");
    }

    private void createEmptyFileIfMissing(String fileName) {
        try {
            File file = new File(plugin.getDataFolder(), fileName);

            if (!file.exists()) {

                if (!plugin.getDataFolder().exists()
                        && !plugin.getDataFolder().mkdirs()) {

                    plugin.getLogger().warning(
                            "Plugin data folder could not be created."
                    );
                    return;
                }

                if (!file.createNewFile()) {
                    plugin.getLogger().warning(
                            "File could not be created: " + fileName
                    );
                    return;
                }

                printFileStatus(fileName,  "erstellt.");
                return;
            }

            printFileStatus(fileName,  "I.O.");

        } catch (IOException e) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Failed to create " + fileName + ".",
                    e
            );
        }
    }

    private int getFileVersion(File file) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        if (config.contains("file-version")) {
            return config.getInt("file-version");
        }

        if (config.contains("config-version")) {
            return config.getInt("config-version");
        }

        return 0;
    }

    private int getResourceVersion(String fileName) {
        try (InputStream inputStream = plugin.getResource(fileName)) {
            if (inputStream == null) {
                return 0;
            }

            InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            FileConfiguration resourceConfig = YamlConfiguration.loadConfiguration(reader);

            if (resourceConfig.contains("file-version")) {
                return resourceConfig.getInt("file-version");
            }

            if (resourceConfig.contains("config-version")) {
                return resourceConfig.getInt("config-version");
            }

            return 0;

        } catch (IOException e) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Failed to read resource version of " + fileName + ".",
                    e
            );
            return 0;
        }
    }

    private void overwriteResource(String resourcePath, File targetFile) {
        try {
            File parent = targetFile.getParentFile();

            if (parent != null
                    && !parent.exists()
                    && !parent.mkdirs()) {
                plugin.getLogger().warning(
                        "Directory could not be created: " + parent.getPath()
                );
                return;
            }

            try (InputStream inputStream = plugin.getResource(resourcePath)) {
                if (inputStream == null) {
                    plugin.getLogger().warning("Resource nicht gefunden: " + resourcePath);
                    return;
                }

                Files.copy(
                        inputStream,
                        targetFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

        } catch (IOException e) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Failed to overwrite resource " + resourcePath + ".",
                    e
            );
        }
    }

    private File createBackupFile(
            File backupFolder,
            String fileName,
            long timestamp
    ) {
        String baseName = fileName;
        String extension = "";

        int dotIndex = fileName.lastIndexOf(".");

        if (dotIndex > 0) {
            baseName = fileName.substring(0, dotIndex);
            extension = fileName.substring(dotIndex);
        }

        String backupName =
                baseName
                        + "-backup-"
                        + timestamp
                        + extension;

        return new File(
                backupFolder,
                backupName
        );
    }

    private void backupFile(File file, String fileName) {
        try {
            File backupFolder = new File(
                    plugin.getDataFolder(),
                    "backups"
            );

            if (!backupFolder.exists()
                    && !backupFolder.mkdirs()) {

                plugin.getLogger().warning(
                        "Backup directory could not be created."
                );
                return;
            }

            long timestamp = System.currentTimeMillis();

            File backupFile = createBackupFile(
                    backupFolder,
                    fileName,
                    timestamp
            );

            File parent = backupFile.getParentFile();

            if (parent != null
                    && !parent.exists()
                    && !parent.mkdirs()) {

                plugin.getLogger().warning(
                        "Could not create backup directory: "
                                + parent.getPath()
                );
                return;
            }

            Files.copy(
                    file.toPath(),
                    backupFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );

        } catch (IOException e) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Failed to create backup for "
                            + fileName
                            + ".",
                    e
            );
        }
    }

    private void extractServerIconIfMissing() {
        File serverIcon = new File("server-icon.png");

        if (serverIcon.exists()) {
            return;
        }

        if (plugin.getResource("server-icon.png") == null) {
            plugin.getLogger().warning("⚠️ Keine server-icon.png im Plugin gefunden!");
            return;
        }

        try (InputStream inputStream = plugin.getResource("server-icon.png")) {
            if (inputStream == null) {
                plugin.getLogger().warning("⚠️ Keine server-icon.png im Plugin gefunden!");
                return;
            }

            Files.copy(
                    inputStream,
                    serverIcon.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );

            Bukkit.getConsoleSender().sendMessage(
                    ConsoleColor.LIME + "   MobArmyWars-ServerIcon wurde erstellt!" + ConsoleColor.RESET
            );
            Bukkit.getConsoleSender().sendMessage(
                    ConsoleColor.GRAY + "   Wird erst nach erneutem Serverstart geladen." + ConsoleColor.RESET
            );

        } catch (IOException e) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Failed to copy server-icon.png.",
                    e
            );
        }
    }

    private void copyResource(String resourcePath, File targetFile) {
        try {
            File parent = targetFile.getParentFile();

            if (parent != null
                    && !parent.exists()
                    && !parent.mkdirs()) {
                plugin.getLogger().warning(
                        "Directory could not be created: " + parent.getPath()
                );
                return;
            }

            try (InputStream inputStream = plugin.getResource(resourcePath)) {
                if (inputStream == null) {
                    plugin.getLogger().warning("Resource nicht gefunden: " + resourcePath);
                    return;
                }

                Files.copy(
                        inputStream,
                        targetFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

        } catch (IOException e) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Failed to copy resource " + resourcePath + ".",
                    e
            );
        }
    }

    private void printFileStatus(
            String fileName,
            String status
    ) {
        String versionPart = "  ";
        String namePart = "Datei - '" + fileName + "'";

        String padding1 =
                " ".repeat(Math.max(1, 5 - versionPart.length()));

        String padding2 =
                " ".repeat(Math.max(1, 34 - namePart.length()));

        Bukkit.getConsoleSender().sendMessage(
                ConsoleColor.COPPER + "   " + versionPart +
                        padding1 +
                        ConsoleColor.GRAY + namePart +
                        padding2 +
                        ConsoleColor.DARK_GREY + "...   " +
                        ConsoleColor.DARK_GOLDEN_LIME + status +
                        ConsoleColor.RESET
        );
    }
}
