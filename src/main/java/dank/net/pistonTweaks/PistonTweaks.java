package dank.net.pistonTweaks;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Piston;
import org.bukkit.block.data.type.PistonHead;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.block.data.type.TechnicalPiston;

import java.util.*;

public final class PistonTweaks extends JavaPlugin implements Listener {
    private int stickyPistonPullStrength = 3;
    private int stickyPistonPushStrength = 3;
    private int pistonPushStrength = 3;
    private List<Material> stickyBlocks = List.of(
            Material.SLIME_BLOCK,
            Material.HONEY_BLOCK
    );

    List<BlockFace> faces = new ArrayList<>(List.of(
            BlockFace.UP,
            BlockFace.DOWN,
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.EAST,
            BlockFace.WEST
    ));

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);
        registerCommands();
        getLogger().info("Enabled");
    }

    private void registerCommands() {
        registerCommand("reload", new BasicCommand() {
            @Override
            public void execute(CommandSourceStack commandSourceStack, String[] args) {
                loadConfig();
                getLogger().info("Configuration reloaded.");

                var player = commandSourceStack.getExecutor();
                if (player == null) return;
                player.sendMessage("Configuration reloaded.");
                player.sendMessage("sticky-piston-push-strength: " + stickyPistonPushStrength);
                player.sendMessage("sticky-piston-pull-strength: " + stickyPistonPullStrength);
                player.sendMessage("piston-push-strength: " + pistonPushStrength);
                player.sendMessage("sticky-blocks: ");

                for (var material : stickyBlocks) {
                    player.sendMessage(material.name());
                }
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        event.setCancelled(true);

        var direction = event.getDirection();
        var piston = event.getBlock();
        var blocks = getRelevantPistonBlocksPush(piston, direction);
        getLogger().info("[EXTEND] piston chunk=" + piston.getChunk().getChunkKey() + " facing=" + direction);

        if (blocks == null) return;
        getLogger().info("[EXTEND] push blocks=" + blocks.size());
        if (piston.getType() == Material.STICKY_PISTON && blocks.size() > stickyPistonPushStrength) return;
        if (piston.getType() == Material.PISTON && blocks.size() > pistonPushStrength) return;

        Bukkit.getScheduler().runTask(this, () -> {
            for (int i = blocks.size() - 1; i >= 0; i--) {
                var from =  blocks.get(i);
                var to = from.getRelative(direction);
                to.setBlockData(from.getBlockData(), false);
                from.setType(Material.AIR, false);
            }

            Piston pistonData = (Piston) piston.getBlockData();
            pistonData.setExtended(true);
            piston.setBlockData(pistonData, false);

            var headBlock = piston.getRelative(event.getDirection());
            PistonHead pistonHead = (PistonHead) Bukkit.createBlockData(Material.PISTON_HEAD);
            pistonHead.setFacing(event.getDirection());
            pistonHead.setShort(false);
            pistonHead.setType(
              piston.getType() == Material.STICKY_PISTON
                ? TechnicalPiston.Type.STICKY
                : TechnicalPiston.Type.NORMAL
            );
            headBlock.setBlockData(pistonHead, false);
        });
    }

    private List<Block> getRelevantPistonBlocksPush(Block piston, BlockFace face) {
        var current = piston.getRelative(face);
        if (current.getType().isAir()) return null;
        int strength = 1;

        if (piston.getType() == Material.STICKY_PISTON) {
            strength = stickyPistonPushStrength;
        }
        else if (piston.getType() == Material.PISTON) {
            strength = pistonPushStrength;
        }

        Set<Block> toMove = new HashSet<>();
        ArrayDeque<Block> queue = new ArrayDeque<>();
        queue.add(current);

        while (!queue.isEmpty()) {
            Block block = queue.removeFirst();

            if (!toMove.add(block)) continue;
            if (toMove.size() > strength) return null;

            if (stickyBlocks.contains(block.getType())) {
                Set<Block> visited = new HashSet<>();
                Set<Block> results = new HashSet<>();

                var stickyBlocks = handleStickyBlocks(block, visited, results);
                if (!stickyBlocks.isEmpty()) toMove.addAll(stickyBlocks);
                if (toMove.size() > strength) return null;
            }

            var forward = block.getRelative(face);
            if (!forward.getType().isAir()) queue.add(forward);
        }

        return new ArrayList<Block>(toMove);
    }

    private boolean canStick(Block block) {
        List<Material> cantStick = List.of(
                Material.STICKY_PISTON,
                Material.PISTON
        );

        var type = block.getType();
        if (type.isAir()) return false;
        if (cantStick.contains(type)) return false;

        return true;
    }

    private List<Block> handleStickyBlocks(Block source, Set<Block> visited, Set<Block> result) {
        if (!visited.add(source)) {
            return new ArrayList<>(result);
        }

        getLogger().info("[STICKY] source=" + source.getType());
        if (canStick(source)) result.add(source);

        for (BlockFace face : faces) {
            Block neighbour = source.getRelative(face);

            if (neighbour.getType().isAir()) continue;
            if (neighbour.equals(source)) continue;

            if (stickyBlocks.contains(neighbour.getType())) {
                handleStickyBlocks(neighbour, visited, result);
            } else {
                if (canStick(neighbour)) result.add(neighbour);
            }
        }

        return new ArrayList<>(result);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!event.isSticky()) return;
        if(event.getBlocks().isEmpty()) return;

        var piston = event.getBlock();
        getLogger().info("[RETRACT] block=" + piston.getType());

        BlockFace facing = event.getDirection().getOppositeFace();
        List<Block> blocks = getRelevantPistolBlocksPull(piston, facing);
        getLogger().info("[RETRACT] pull blocks=" + blocks.size() + " facing=" + facing);

        if (blocks.size() > stickyPistonPullStrength) return;
        event.setCancelled(true);

        Bukkit.getScheduler().runTask(this, () -> {
            Material material = event.isSticky() ? Material.STICKY_PISTON : Material.PISTON;
            Piston pistonData = (Piston) Bukkit.createBlockData(material);

            pistonData.setExtended(false);
            pistonData.setFacing(facing);
            event.getBlock().setType(material, false);
            event.getBlock().setBlockData(pistonData, false);

            for (int i = 0; i < blocks.size(); i++) {
                Block from = blocks.get(i);
                Block to = from.getRelative(facing.getOppositeFace());
                to.setBlockData(from.getBlockData(), false);
                from.setType(Material.AIR, false);
            }
        });
    }

    private List<Block> getRelevantPistolBlocksPull(Block piston, BlockFace face) {
        var current = piston.getRelative(face, 2);
        int strength = Math.max(1, stickyPistonPullStrength);

        Set<Block> toMove = new HashSet<>();
        ArrayDeque<Block> queue = new ArrayDeque<>();
        queue.add(current);

        while (!queue.isEmpty()) {
            Block block = queue.removeFirst();

            if (!toMove.add(block)) continue;
            if (toMove.size() > strength) return null;

            if (stickyBlocks.contains(block.getType())) {
                Set<Block> visited = new HashSet<>();
                Set<Block> results = new HashSet<>();

                var stickyBlocks = handleStickyBlocks(block, visited, results);
                if (!stickyBlocks.isEmpty()) toMove.addAll(stickyBlocks);
                if (toMove.size() > strength) return null;
            }

            var forward = block.getRelative(face);
            if (!forward.getType().isAir()) queue.add(forward);
        }

        return new ArrayList<Block>(toMove);
    }

    @Override
    public void onDisable() {
        getLogger().info("[PistonTweaks] Disabled");
    }

    private void loadConfig() {
        reloadConfig();
        var config = getConfig();
        stickyPistonPullStrength = config.getInt("sticky-piston-pull-strength");
        stickyPistonPushStrength = config.getInt("sticky-piston-push-strength");
        pistonPushStrength = config.getInt("piston-push-strength");

        stickyBlocks = new ArrayList<>();
        var blocks = config.getStringList("sticky-blocks");
        if (blocks.isEmpty()) return;

        List<Material> materials = blocks.stream()
                .map(String::toUpperCase)
                .map(Material::valueOf)
                .toList();

        if (materials.isEmpty()) return;
        stickyBlocks.addAll(materials);
    }
}
