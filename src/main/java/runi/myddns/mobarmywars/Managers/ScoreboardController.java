package runi.myddns.mobarmywars.Managers;

import org.bukkit.entity.Player;
import runi.myddns.mobarmywars.MobArmyMain;

import java.util.HashMap;
import java.util.Map;

public class ScoreboardController {

    public enum BoardType {
        NONE,
        TEAM,
        ARENA
    }

    private final MobArmyMain plugin;

    private final Map<Player, BoardType> activeBoards = new HashMap<>();

    public ScoreboardController(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    public void setBoard(Player player, BoardType type) {
        activeBoards.put(player, type);
    }

    public BoardType getActiveBoard(Player player) {
        return activeBoards.getOrDefault(player, BoardType.NONE);
    }

    public boolean isArenaBoardActive() {
        return activeBoards.containsValue(BoardType.ARENA);
    }

    public void setArenaBoard(Player player) {
        setBoard(player, BoardType.ARENA);
    }

    public void setTeamBoard(Player player) {
        setBoard(player, BoardType.TEAM);
    }
}