package runi.myddns.mobarmywars.Commands;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import runi.myddns.mobarmywars.Managers.World.ResumeManager;
import runi.myddns.mobarmywars.MobArmyMain;

import java.util.ArrayList;
import java.util.List;

public class SetPhaseCommand implements CommandExecutor, TabCompleter {

    private final MobArmyMain plugin;

    public SetPhaseCommand(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String @NotNull [] args
    ) {

        if (!sender.isOp()) {
            sender.sendMessage(
                    lang("commands.set-phase.op-only")
            );
            return true;
        }

        if (args.length != 1) {
            sendUsage(sender, label);
            return true;
        }

        int phase = parsePhase(args[0]);

        if (phase == -1) {

            sender.sendMessage(
                    plugin.getLanguageManager()
                            .getComponent(
                                    "commands.set-phase.invalid-phase",
                                    "phase",
                                    args[0]
                            )
            );

            sendUsage(sender, label);
            return true;
        }

        plugin.getEventResume().savePhase(phase);

        sender.sendMessage(
                plugin.getLanguageManager()
                        .getComponent(
                                "commands.set-phase.success",
                                "phase",
                                getPhaseName(phase)
                        )
        );

        return true;
    }

    private void sendUsage(
            CommandSender sender,
            String label
    ) {

        sender.sendMessage(
                plugin.getLanguageManager()
                        .getComponent(
                                "commands.set-phase.usage.title",
                                "label",
                                label
                        )
        );

        sender.sendMessage(lang("commands.set-phase.usage.phases"));
        sender.sendMessage(lang("commands.set-phase.usage.lobby"));
        sender.sendMessage(lang("commands.set-phase.usage.teamworld"));
        sender.sendMessage(lang("commands.set-phase.usage.wave-selection"));
        sender.sendMessage(lang("commands.set-phase.usage.arena"));
    }

    private int parsePhase(String input) {

        return switch (input.toLowerCase()) {

            case "0", "lobby" ->
                    ResumeManager.PHASE_LOBBY;

            case "1", "teamwelt" ->
                    ResumeManager.PHASE_TEAMWELT;

            case "2", "wave", "waveauswahl" ->
                    ResumeManager.PHASE_WAVEAUSWAHL;

            case "3", "arena" ->
                    ResumeManager.PHASE_ARENA;

            default -> -1;
        };
    }

    private String getPhaseName(int phase) {

        return switch (phase) {

            case ResumeManager.PHASE_LOBBY ->
                    plugin.getLanguageManager()
                            .get("commands.set-phase.phase-names.lobby");

            case ResumeManager.PHASE_TEAMWELT ->
                    plugin.getLanguageManager()
                            .get("commands.set-phase.phase-names.teamworld");

            case ResumeManager.PHASE_WAVEAUSWAHL ->
                    plugin.getLanguageManager()
                            .get("commands.set-phase.phase-names.wave-selection");

            case ResumeManager.PHASE_ARENA ->
                    plugin.getLanguageManager()
                            .get("commands.set-phase.phase-names.arena");

            default ->
                    plugin.getLanguageManager()
                            .get("commands.set-phase.phase-names.unknown");
        };
    }

    @Override
    public List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String @NotNull [] args
    ) {

        if (!sender.isOp()) {
            return new ArrayList<>();
        }

        if (args.length == 1) {

            List<String> options = List.of(
                    "lobby",
                    "teamwelt",
                    "waveauswahl",
                    "arena",
                    "0",
                    "1",
                    "2",
                    "3"
            );

            String input = args[0].toLowerCase();

            return options.stream()
                    .filter(option ->
                            option.startsWith(input)
                    )
                    .toList();
        }

        return new ArrayList<>();
    }

    private Component lang(String path) {

        return plugin.getLanguageManager()
                .getComponent(path);
    }
}