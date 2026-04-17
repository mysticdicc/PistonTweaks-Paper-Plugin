package dank.net.pistonTweaks;

import org.bukkit.Bukkit;
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

import java.util.ArrayList;
import java.util.List;

public final class PistonTweaks extends JavaPlugin implements Listener {
    private int stickyPistonStrength = 3;
    private int pistonStrength = 3;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Enabled");
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
        var list = new ArrayList<Block>();
        var current = piston.getRelative(face);
        int strength = 1;

        if (piston.getType() == Material.STICKY_PISTON) {
            strength = stickyPistonStrength;
        }
        else if (piston.getType() == Material.PISTON) {
            strength = pistonStrength;
        }

        for (int i = 0; i < strength; i++) {
            if (current.getType().isAir()) {
                return list;
            }

            list.add(current);
            current = current.getRelative(face);
        }

        if (!current.getType().isAir()) {
            return null;
        }

        return list;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!event.isSticky()) return;
        if(event.getBlock().isEmpty()) return;

        var piston = event.getBlock();
        getLogger().info("[RETRACT] block=" + piston.getType());

        BlockFace facing = event.getDirection().getOppositeFace();
        List<Block> blocks = getRelevantPistolBlocksPull(piston, facing);
        getLogger().info("[RETRACT] pull blocks=" + blocks.size() + " facing=" + facing);

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
        var list = new ArrayList<Block>();
        var current = piston.getRelative(face, 2);
        int strength = Math.max(1, stickyPistonStrength);

        for (int i = 0; i < strength; i++) {
            if (current.getType().isAir()) {
                return list;
            }

            list.add(current);
            current = current.getRelative(face);
        }

        return list;
    }

    @Override
    public void onDisable() {
        getLogger().info("[PistonTweaks] Disabled");
    }

    private void loadConfig() {
        var config = getConfig();
        stickyPistonStrength = config.getInt("sticky-piston-strength");
        pistonStrength = config.getInt("piston-strength");
    }
}
