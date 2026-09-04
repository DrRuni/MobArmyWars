package runi.myddns.mobarmywars.Managers.World;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import java.util.Map;
import runi.myddns.mobarmywars.MobArmyMain;

import java.io.File;

public class LanguageManager {

    private final MobArmyMain plugin;
    private final MiniMessage miniMessage;

    private YamlConfiguration languageConfig;

    public LanguageManager(MobArmyMain plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();

        reload();
    }

    public void reload() {

        String language = plugin.getConfig()
                .getString("language");

        if (language == null
                || language.isBlank()
                || (!language.equalsIgnoreCase("de")
                && !language.equalsIgnoreCase("en"))) {

            language = "en";
        }

        File file = new File(
                plugin.getDataFolder(),
                "languages/" + language + ".yml"
        );

        languageConfig =
                YamlConfiguration.loadConfiguration(file);
    }

    public String get(String path) {

        String text = languageConfig.getString(path);

        if (text == null) {
            return "Missing language entry: " + path;
        }

        return text;
    }

    public Component getComponent(String path) {

        String text = languageConfig.getString(path);

        if (text == null) {
            return Component.text(
                    "Missing language entry: " + path
            );
        }

        return miniMessage.deserialize(text);
    }

    public Component getComponent(
            String path,
            String placeholder,
            Component value
    ) {
        return miniMessage.deserialize(
                get(path),
                Placeholder.component(
                        placeholder,
                        value
                )
        );
    }

    public Component getComponent(
            String path,
            Map<String, Component> placeholders
    ) {
        TagResolver.Builder resolver =
                TagResolver.builder();

        for (Map.Entry<String, Component> entry : placeholders.entrySet()) {
            resolver.resolver(
                    Placeholder.component(
                            entry.getKey(),
                            entry.getValue()
                    )
            );
        }

        return miniMessage.deserialize(
                get(path),
                resolver.build()
        );
    }

    public Component getComponent(
            String path,
            String placeholder,
            Object value
    ) {

        String text = languageConfig.getString(path);

        if (text == null) {
            return Component.text(
                    "Missing language entry: " + path
            );
        }

        text = text.replace(
                "%" + placeholder + "%",
                String.valueOf(value)
        );

        return miniMessage.deserialize(text);
    }

    public String get(
            String path,
            String placeholder,
            Object value
    ) {

        return get(path).replace(
                "%" + placeholder + "%",
                String.valueOf(value)
        );
    }
}