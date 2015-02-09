package com.intellectualcrafters.plot.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.World;
import org.bukkit.block.Beacon;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Chest;
import org.bukkit.block.CommandBlock;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Dropper;
import org.bukkit.block.Furnace;
import org.bukkit.block.Hopper;
import org.bukkit.block.Jukebox;
import org.bukkit.block.NoteBlock;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import com.intellectualcrafters.plot.PlotMain;
import com.intellectualcrafters.plot.object.BlockLoc;
import com.intellectualcrafters.plot.object.ChunkLoc;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.RegionWrapper;
import com.intellectualcrafters.plot.object.entity.EntityWrapper;

public class ChunkManager {
    
    public static RegionWrapper CURRENT_PLOT_CLEAR = null;
    public static HashMap<ChunkLoc, HashMap<Short, Short>> GENERATE_BLOCKS = new HashMap<>();
    public static HashMap<ChunkLoc, HashMap<Short, Byte>> GENERATE_DATA = new HashMap<>();
    
    public static ArrayList<ChunkLoc> getChunkChunks(World world) {
        File[] regionFiles = new File(new File(".").getAbsolutePath() + File.separator + world.getName() + File.separator + "region").listFiles();
        ArrayList<ChunkLoc> chunks = new ArrayList<>();
        for (File file : regionFiles) {
            String name = file.getName();
            if (name.endsWith("mca")) {
                String[] split = name.split("\\.");
                try {
                    chunks.add(new ChunkLoc(Integer.parseInt(split[1]), Integer.parseInt(split[2])));
                } catch (Exception e) {  }
            }
        }
        return chunks;
    }
    
    public static void deleteRegionFile(final String world, final ChunkLoc loc) {
        TaskManager.runTask(new Runnable() {
            @Override
            public void run() {
                String directory = new File(".").getAbsolutePath() + File.separator + world + File.separator + "region" + File.separator + "r." + loc.x + "." + loc.z + ".mca";
                File file = new File(directory);
                PlotMain.sendConsoleSenderMessage("&6 - Deleted region "+file.getName()+" (max 256 chunks)");
                if (file.exists()) {
                    file.delete();
                }
            }
        });
    }
    
    public static boolean hasPlot(World world, Chunk chunk) {
        int x1 = chunk.getX() << 4;
        int z1 = chunk.getZ() << 4;
        int x2 = x1 + 15;
        int z2 = z1 + 15;
        
        Location bot = new Location(world, x1, 0, z1);
        Plot plot;
        plot = PlotHelper.getCurrentPlot(bot); 
        if (plot != null && plot.owner != null) {
            return true;
        }
        Location top = new Location(world, x2, 0, z2);
        plot = PlotHelper.getCurrentPlot(top); 
        if (plot != null && plot.owner != null) {
            return true;
        }
        return false;
    }
    
    private static HashMap<BlockLoc, ItemStack[]> chestContents;
    private static HashMap<BlockLoc, ItemStack[]> furnaceContents;
    private static HashMap<BlockLoc, ItemStack[]> dispenserContents;
    private static HashMap<BlockLoc, ItemStack[]> dropperContents;
    private static HashMap<BlockLoc, ItemStack[]> brewingStandContents;
    private static HashMap<BlockLoc, ItemStack[]> beaconContents;
    private static HashMap<BlockLoc, ItemStack[]> hopperContents;
    private static HashMap<BlockLoc, Short[]> furnaceTime;
    private static HashMap<BlockLoc, Object[]> skullData;
    private static HashMap<BlockLoc, Short> jukeDisc;
    private static HashMap<BlockLoc, Short> brewTime;
    private static HashMap<BlockLoc, String> spawnerData;
    private static HashMap<BlockLoc, String> cmdData;
    private static HashMap<BlockLoc, String[]> signContents;
    private static HashMap<BlockLoc, Note> noteBlockContents;
    
    private static HashSet<EntityWrapper> entities;
    
    public static boolean regenerateRegion(Location pos1, Location pos2) {
        World world = pos1.getWorld();
        Chunk c1 = world.getChunkAt(pos1);
        Chunk c2 = world.getChunkAt(pos2);

        CURRENT_PLOT_CLEAR = new RegionWrapper(pos1.getBlockX(), pos2.getBlockX(), pos1.getBlockZ(), pos2.getBlockZ());
        
        int sx = pos1.getBlockX();
        int sz = pos1.getBlockZ();
        int ex = pos2.getBlockX();
        int ez = pos2.getBlockZ();
        
        int c1x = c1.getX();
        int c1z = c1.getZ();
        int c2x = c2.getX();
        int c2z = c2.getZ();
        
        int maxY = world.getMaxHeight();
        for (int x = c1x; x <= c2x; x ++) {
            for (int z = c1z; z <= c2z; z ++) {
                Chunk chunk = world.getChunkAt(x, z);
                boolean loaded = true;
                if (!chunk.isLoaded()) {
                    boolean result = chunk.load(false);
                    if (!result) {
                        loaded = false;;
                    }
                    if (!chunk.isLoaded()) {
                        loaded = false;
                    }
                }
                if (loaded) {
                    initMaps();
                    int absX = x << 4;
                    int absZ = z << 4;
                    if (x == c1x || z == c1z) {
                        for (int X = 0; X < 16; X++) {
                            for (int Z = 0; Z < 16; Z++) {
                                if ((X + absX < sx || Z + absZ < sz) || (X + absX > ex || Z + absZ > ez)) {
                                    saveBlock(world, maxY, X + absX, Z + absZ);
                                }
                            }
                        }
                    }
                    else if (x == c2x || z == c2z) {
                        for (int X = 0; X < 16; X++) {
                            for (int Z = 0; Z < 16; Z++) {
                                if ((X + absX > ex || Z + absZ > ez) || (X + absX < sx || Z + absZ < sz)) {
                                    saveBlock(world, maxY, X + absX, Z + absZ);
                                }
                            }
                        }
                    }
                    saveEntities(chunk, CURRENT_PLOT_CLEAR);
                    world.regenerateChunk(x, z);
                    restoreBlocks(world, 0, 0);
                    restoreEntities(world, 0, 0);
                    chunk.unload();
                    chunk.load();
                }
            }
        }
        CURRENT_PLOT_CLEAR = null;
        initMaps();
        return true;
    }
    
    public static void initMaps() {
        GENERATE_BLOCKS = new HashMap<>();
        GENERATE_DATA = new HashMap<>();
        
        chestContents = new HashMap<>();
        furnaceContents = new HashMap<>();
        dispenserContents = new HashMap<>();
        dropperContents = new HashMap<>();
        brewingStandContents = new HashMap<>();
        beaconContents = new HashMap<>();
        hopperContents = new HashMap<>();
        furnaceTime = new HashMap<>();
        skullData = new HashMap<>();
        brewTime = new HashMap<>();
        jukeDisc = new HashMap<>();
        spawnerData= new HashMap<>();
        noteBlockContents = new HashMap<>();
        signContents = new HashMap<>();
        cmdData = new HashMap<>();
        
        entities = new HashSet<>();
    }
    
    public static boolean isIn(RegionWrapper region, int x, int z) {
        return (x >= region.minX && x <= region.maxX && z >= region.minZ && z <= region.maxZ);
    }
    
    public static void saveEntities(Chunk chunk, RegionWrapper region) {
        for (Entity entity : chunk.getEntities()) {
            Location loc = entity.getLocation();
            int x = loc.getBlockX();
            int z = loc.getBlockZ();
            if (isIn(region, x, z)) {
                continue;
            }
            if (entity.getVehicle() != null) {
                continue;
            }
            EntityWrapper wrap = new EntityWrapper(entity, (short) 2);
            entities.add(wrap);
            
            
            
//            int y = loc.getBlockY();
//            BlockLoc bl = new BlockLoc(x, y, z);
//            EntityWrapper wrap = new EntityWrapper(entity.getType().getTypeId());
//            entities.put(wrap, bl);
//            System.out.print(entity.isDead());
//            entity.teleport(new Location(chunk.getWorld(), 0, 65, 0));
        }
    }
    
    public static void restoreEntities(World world, int x_offset, int z_offset) {
        for (EntityWrapper entity : entities) {
            try {
                entity.spawn(world, x_offset, z_offset);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void restoreBlocks(World world, int x_offset, int z_offset) {
        for (BlockLoc loc: chestContents.keySet()) {
            Block block = world.getBlockAt(loc.x + x_offset, loc.y, loc.z + z_offset);
            BlockState state = block.getState();
            if (state instanceof Chest) {
                Chest chest = (Chest) state;
                chest.getInventory().setContents(chestContents.get(loc));
                state.update(true);
            }
            else { PlotMain.sendConsoleSenderMessage("&c[WARN] Plot clear failed to regenerate chest: "+loc.x + x_offset+","+loc.y+","+loc.z + z_offset); }
        }
        
        for (BlockLoc loc: signContents.keySet()) {
            Block block = world.getBlockAt(loc.x + x_offset, loc.y, loc.z + z_offset);
            BlockState state = block.getState();
            if (state instanceof Sign) {
                Sign sign = (Sign) state;
                int i = 0;
                for (String line : signContents.get(loc)) {
                    sign.setLine(i, line);
                    i++;
                }
                state.update(true);
            }
            else { PlotMain.sendConsoleSenderMessage("&c[WARN] Plot clear failed to regenerate sign: "+loc.x + x_offset+","+loc.y+","+loc.z + z_offset); }
        }
        
        for (BlockLoc loc: dispenserContents.keySet()) {
            Block block = world.getBlockAt(loc.x + x_offset, loc.y, loc.z + z_offset);
            BlockState state = block.getState();
            if (state instanceof Dispenser) {
                ((Dispenser) (state)).getInventory().setContents(dispenserContents.get(loc));
                state.update(true);
            }
            else { PlotMain.sendConsoleSenderMessage("&c[WARN] Plot clear failed to regenerate dispenser: "+loc.x + x_offset+","+loc.y+","+loc.z + z_offset); }
        }
        for (BlockLoc loc: dropperContents.keySet()) {
            Block block = world.getBlockAt(loc.x + x_offset, loc.y, loc.z + z_offset);
            BlockState state = block.getState();
            if (state instanceof Dropper) {
                ((Dropper) (state)).getInventory().setContents(dropperContents.get(loc));
                state.update(true);
            }
            else { PlotMain.sendConsoleSenderMessage("&c[WARN] Plot clear failed to regenerate dispenser: "+loc.x + x_offset+","+loc.y+","+loc.z + z_offset); }
        }
        for (BlockLoc loc: beaconContents.keySet()) {
            Block block = world.getBlockAt(loc.x + x_offset, loc.y, loc.z + z_offset);
            BlockState state = block.getState();
            if (state instanceof Beacon) {
                ((Beacon) (state)).getInventory().setContents(beaconContents.get(loc));
                state.update(true);
            }
            else { PlotMain.sendConsoleSenderMessage("&c[WARN] Plot clear failed to regenerate beacon: "+loc.x + x_offset+","+loc.y+","+loc.z + z_offset); }
        }
        for (BlockLoc loc: jukeDisc.keySet()) {
            Block block = world.getBlockAt(loc.x + x_offset, loc.y, loc.z + z_offset);
            BlockState state = block.getState();
            if (state instanceof Jukebox) {
                ((Jukebox) (state)).setPlaying(Material.getMaterial(jukeDisc.get(loc)));
                state.update(true);
            }
            else { PlotMain.sendConsoleSenderMessage("&c[WARN] Plot clear failed to restore jukebox: "+loc.x + x_offset+","+loc.y+","+loc.z + z_offset); }
        }
        for (BlockLoc loc: skullData.keySet()) {
            Block block = world.getBlockAt(loc.x + x_offset, loc.y, loc.z + z_offset);
            BlockState state = block.getState();
            if (state instanceof Skull) {
                Object[] data = skullData.get(loc);
                if (data[0] != null) {
                    System.out.print("SET OWNER");
                    ((Skull) (state)).setOwner((String) data[0]);
                }
                if (((Integer) data[1]) != 0) {
                    ((Skull) (state)).setRotation(getRotation((Integer) data[1]));
                }
                state.update(true);
            }
            else { PlotMain.sendConsoleSenderMessage("&c[WARN] Plot clear failed to restore jukebox: "+loc.x + x_offset+","+loc.y+","+loc.z + z_offset); }
        }
        for (BlockLoc loc: hopperContents.keySet()) {
            Block block = world.getBlockAt(loc.x + x_offset, loc.y, loc.z + z_offset);
            BlockState state = block.getState();
            if (state instanceof Hopper) {
                ((Hopper) (state)).getInventory().setContents(hopperContents.get(loc));
                state.update(true);
            }
            else { PlotMain.sendConsoleSenderMessage("&c[WARN] Plot clear failed to regenerate hopper: "+loc.x + x_offset+","+loc.y+","+loc.z + z_offset); }
        }
        
        for (BlockLoc loc: noteBlockContents.keySet()) {
            Block block = world.getBlockAt(loc.x + x_offset, loc.y, loc.z + z_offset);
            BlockState state = block.getState();
            if (state instanceof NoteBlock) {
                ((NoteBlock) (state)).setNote(noteBlockContents.get(loc));
                state.update(true);
            }
            else { PlotMain.sendConsoleSenderMessage("&c[WARN] Plot clear failed to regenerate note block: "+loc.x + x_offset+","+loc.y+","+loc.z + z_offset); }
        }
        for (BlockLoc loc: brewTime.keySet()) {
            Block block = world.getBlockAt(loc.x + x_offset, loc.y, loc.z + z_offset);
            BlockState state = block.getState();
            if (state instanceof BrewingStand) {
                ((BrewingStand) (state)).setBrewingTime(brewTime.get(loc));
            }
            else { PlotMain.sendConsoleSenderMessage("&c[WARN] Plot clear failed to restore brewing stand cooking: "+loc.x + x_offset+","+loc.y+","+loc.z + z_offset); }
        }
        
        for (BlockLoc loc: spawnerData.keySet()) {
            Block block = world.getBlockAt(loc.x + x_offset, loc.y, loc.z + z_offset);
            BlockState state = block.getState();
            if (state instanceof CreatureSpawner) {
                ((CreatureSpawner) (state)).setCreatureTypeId(spawnerData.get(loc));
                state.update(true);
            }
            else { PlotMain.sendConsoleSenderMessage("&c[WARN] Plot clear failed to restore spawner type: "+loc.x + x_offset+","+loc.y+","+loc.z + z_offset); }
        }
        for (BlockLoc loc: cmdData.keySet()) {
            Block block = world.getBlockAt(loc.x + x_offset, loc.y, loc.z + z_offset);
            BlockState state = block.getState();
            if (state instanceof CommandBlock) {
                ((CommandBlock) (state)).setCommand(cmdData.get(loc));
                state.update(true);
            }
            else { PlotMain.sendConsoleSenderMessage("&c[WARN] Plot clear failed to restore command block: "+loc.x + x_offset+","+loc.y+","+loc.z + z_offset); }
        }
        for (BlockLoc loc: brewingStandContents.keySet()) {
            Block block = world.getBlockAt(loc.x + x_offset, loc.y, loc.z + z_offset);
            BlockState state = block.getState();
            if (state instanceof BrewingStand) {
                ((BrewingStand) (state)).getInventory().setContents(brewingStandContents.get(loc));
                state.update(true);
            }
            else { PlotMain.sendConsoleSenderMessage("&c[WARN] Plot clear failed to regenerate brewing stand: "+loc.x + x_offset+","+loc.y+","+loc.z + z_offset); }
        }
        for (BlockLoc loc: furnaceTime.keySet()) {
            Block block = world.getBlockAt(loc.x + x_offset, loc.y, loc.z + z_offset);
            BlockState state = block.getState();
            if (state instanceof Furnace) {
                Short[] time = furnaceTime.get(loc);
                ((Furnace) (state)).setBurnTime(time[0]);
                ((Furnace) (state)).setCookTime(time[1]);
            }
            else { PlotMain.sendConsoleSenderMessage("&c[WARN] Plot clear failed to restore furnace cooking: "+loc.x + x_offset+","+loc.y+","+loc.z + z_offset); }
        }
        for (BlockLoc loc: furnaceContents.keySet()) {
            Block block = world.getBlockAt(loc.x + x_offset, loc.y, loc.z + z_offset);
            BlockState state = block.getState();
            if (state instanceof Furnace) {
                ((Furnace) (state)).getInventory().setContents(furnaceContents.get(loc));
                state.update(true);
            }
            else { PlotMain.sendConsoleSenderMessage("&c[WARN] Plot clear failed to regenerate furnace: "+loc.x + x_offset+","+loc.y+","+loc.z + z_offset); }
        }
    }
    
    public static void saveBlock(World world, int maxY, int x, int z) {
        HashMap<Short, Short> ids = new HashMap<>();
        HashMap<Short, Byte> datas = new HashMap<>();
        for (short y = 1; y < maxY; y++) {
            Block block = world.getBlockAt(x, y, z);
            short id = (short) block.getTypeId();
            if (id != 0) {
                ids.put(y, id);
                byte data = block.getData();
                if (data != 0) {
                    datas.put(y, data);
                }
                BlockLoc bl;
                switch (id) {
                    case 54:
                        bl = new BlockLoc(x, y, z);
                        Chest chest = (Chest) block.getState();
                        ItemStack[] inventory = chest.getBlockInventory().getContents().clone();
                        chestContents.put(bl, inventory);
                        break;
                    case 52:
                        bl = new BlockLoc(x, y, z);
                        CreatureSpawner spawner = (CreatureSpawner) block.getState();
                        String type = spawner.getCreatureTypeId();
                        if (type != null && type.length() != 0) {
                            spawnerData.put(bl, type);
                        }
                        break;
                    case 137:
                        bl = new BlockLoc(x, y, z);
                        CommandBlock cmd = (CommandBlock) block.getState();
                        String string = cmd.getCommand();
                        if (string != null && string.length() > 0) {
                            cmdData.put(bl, string);
                        }
                        break;
                    case 63: case 68: case 323:
                        bl = new BlockLoc(x, y, z);
                        Sign sign = (Sign) block.getState();
                        sign.getLines();
                        signContents.put(bl, sign.getLines().clone());
                        break;
                    case 61: case 62:
                        bl = new BlockLoc(x, y, z);
                        Furnace furnace = (Furnace) block.getState();
                        short burn = furnace.getBurnTime();
                        short cook = furnace.getCookTime();
                        ItemStack[] invFur = furnace.getInventory().getContents().clone();
                        furnaceContents.put(bl, invFur);
                        if (cook != 0) {
                            furnaceTime.put(bl, new Short[] {burn, cook});
                        }
                        break;
                    case 23:
                        bl = new BlockLoc(x, y, z);
                        Dispenser dispenser = (Dispenser) block.getState();
                        ItemStack[] invDis = dispenser.getInventory().getContents().clone();
                        dispenserContents.put(bl, invDis);
                        break;
                    case 158:
                        bl = new BlockLoc(x, y, z);
                        Dropper dropper = (Dropper) block.getState();
                        ItemStack[] invDro = dropper.getInventory().getContents().clone();
                        dropperContents.put(bl, invDro);
                        break;
                    case 117:
                        bl = new BlockLoc(x, y, z);
                        BrewingStand brewingStand = (BrewingStand) block.getState();
                        short time = (short) brewingStand.getBrewingTime();
                        if (time > 0) {
                            brewTime.put(bl, time);
                        }
                        ItemStack[] invBre = brewingStand.getInventory().getContents().clone();
                        brewingStandContents.put(bl, invBre);
                        break;
                    case 25:
                        bl = new BlockLoc(x, y, z);
                        NoteBlock noteBlock = (NoteBlock) block.getState();
                        Note note = noteBlock.getNote();
                        noteBlockContents.put(bl, note);
                        break;
                    case 138:
                        bl = new BlockLoc(x, y, z);
                        Beacon beacon = (Beacon) block.getState();
                        ItemStack[] invBea = beacon.getInventory().getContents().clone();
                        beaconContents.put(bl, invBea);
                        break;
                    case 84:
                        bl = new BlockLoc(x, y, z);
                        Jukebox jukebox = (Jukebox) block.getState();
                        Material playing = jukebox.getPlaying();
                        if (playing != null) {
                            jukeDisc.put(bl, (short) playing.getId());
                        }
                        break;
                    case 154:
                        bl = new BlockLoc(x, y, z);
                        Hopper hopper = (Hopper) block.getState();
                        ItemStack[] invHop = hopper.getInventory().getContents().clone();
                        hopperContents.put(bl, invHop);
                        break;
                    case 397:
                        System.out.print("SAVING SKULL");
                        bl = new BlockLoc(x, y, z);
                        Skull skull = (Skull) block.getState();
                        String o = skull.getOwner();
                        short rot = (short) skull.getRotation().ordinal();
                        skullData.put(bl, new Object[] {o, rot});
                        break;
                }
            }
        }
        ChunkLoc loc = new ChunkLoc(x, z);
        GENERATE_BLOCKS.put(loc, ids);
        GENERATE_DATA.put(loc, datas);
    }
    
    public static BlockFace getRotation(int ordinal) {
        switch (ordinal) {
            case 0: {
                return BlockFace.NORTH;
            } 
            case 1: {
                return BlockFace.NORTH_NORTH_EAST;
            } 
            case 2: {
                return BlockFace.NORTH_EAST;
            } 
            case 3: {
                return BlockFace.EAST_NORTH_EAST;
            } 
            case 4: {
                return BlockFace.EAST;
            } 
            case 5: {
                return BlockFace.EAST_SOUTH_EAST;
            } 
            case 6: {
                return BlockFace.SOUTH_EAST;
            } 
            case 7: {
                return BlockFace.SOUTH_SOUTH_EAST;
            } 
            case 8: {
                return BlockFace.SOUTH;
            } 
            case 9: {
                return BlockFace.SOUTH_SOUTH_WEST;
            } 
            case 10: {
                return BlockFace.SOUTH_WEST;
            } 
            case 11: {
                return BlockFace.WEST_SOUTH_WEST;
            } 
            case 12: {
                return BlockFace.WEST;
            } 
            case 13: {
                return BlockFace.WEST_NORTH_WEST;
            } 
            case 14: {
                return BlockFace.NORTH_WEST;
            } 
            case 15: {
                return BlockFace.NORTH_NORTH_WEST;
            }
            default: {
                return BlockFace.NORTH;
            }
        }
    }
}
