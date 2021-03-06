package com.pwncraftpvp.zombies.core;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import com.pwncraftpvp.zombies.events.PlayerTargetBlockEvent;
import com.pwncraftpvp.zombies.game.Game;
import com.pwncraftpvp.zombies.game.Map;
import com.pwncraftpvp.zombies.game.Status;
import com.pwncraftpvp.zombies.game.Weapon;
import com.pwncraftpvp.zombies.utils.MySQL;
import com.pwncraftpvp.zombies.utils.Utils;

public class Main extends JavaPlugin {
	
	private static Main instance;
	private String gray = ChatColor.GRAY + "";
	private String red = ChatColor.RED + "";
	
	public Game game;
	public MySQL mysql;
	public String nmsver = null;
	
	public BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
	
	public List<Map> maps = new ArrayList<Map>();
	
	public HashMap<String, Block> targetblock = new HashMap<String, Block>();
	public HashMap<String, Statistics> stats = new HashMap<String, Statistics>();
	
	/**
	 * Get the instance of this class
	 * @return The instance of this class
	 */
	public static final Main getInstance(){
		return instance;
	}
	
	public void onEnable(){
		instance = this;
		
		this.getServer().getPluginManager().registerEvents(new Events(), this);
		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		
		mysql = new MySQL();
		mysql.connect();
		game = new Game();
		nmsver = Bukkit.getServer().getClass().getPackage().getName();
		nmsver = nmsver.substring(nmsver.lastIndexOf(".") + 1);
		
		File file = new File(this.getDataFolder(), "config.yml");
		if(file.exists() == false){
			this.getConfig().set("mysql.host", "pwncraftpvp.com");
			this.getConfig().set("mysql.database", "zombies");
			this.getConfig().set("mysql.user", "username");
			this.getConfig().set("mysql.pass", "password");
			this.saveConfig();
		}
		
		for(String s : this.getConfig().getConfigurationSection("maps").getKeys(false)){
			maps.add(new Map(s));
		}
		
		this.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable(){
			public void run(){
				Utils.removeEntities();
			}
		}, 20);
		
		this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable(){
			@SuppressWarnings("deprecation")
			public void run(){
				for(Player p : Bukkit.getOnlinePlayers()){
					Block oldblock = null;
					if(targetblock.containsKey(p.getName()) == true){
						oldblock = targetblock.get(p.getName());
					}
					Block newblock = p.getTargetBlock(null, 4);
					
					if((oldblock == null && newblock != null) || oldblock.getType() != newblock.getType()){
						getServer().getPluginManager().callEvent(new PlayerTargetBlockEvent(p, newblock, oldblock));
						if(targetblock.containsKey(p.getName()) == true){
							targetblock.remove(p.getName());
						}
						targetblock.put(p.getName(), newblock);
					}
				}
				Utils.getWorld().setTime(14000);
			}
		}, 0, 10);
	}
	
	public void onDisable(){
		mysql.close();
	}
	
	@SuppressWarnings("deprecation")
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		if(sender instanceof Player){
			Player player = (Player) sender;
			ZPlayer zplayer = new ZPlayer(player);
			if(cmd.getName().equalsIgnoreCase("vote")){
				if(game.getStatus() == Status.VOTING){
					if(args.length > 0){
						if(game.voted.contains(player.getName()) == false){
							if(Utils.isInteger(args[0]) == true){
								int number = Integer.parseInt(args[0]);
								if(number >= 1 && number <= maps.size() && number <= 5){
									Map map = game.voteables.get((number - 1));
									int current = game.votes.get(map.getName());
									game.votes.remove(map.getName());
									game.votes.put(map.getName(), current + 1);
									game.voted.add(player.getName());
									zplayer.sendMessage("You have voted for " + red + map.getName() + gray + ".");
								}else{
									zplayer.sendError("You must enter a number 1-5.");
								}
							}else{
								zplayer.sendError("You must enter a number.");
							}
						}else{
							zplayer.sendError("You have already voted.");
						}
					}else{
						zplayer.sendVote();
					}
				}else{
					zplayer.sendError("The voting period has ended.");
				}
			}else if(cmd.getName().equalsIgnoreCase("zombies")){
				if(player.isOp() == true){
					if(args.length > 0){
						if(args[0].equalsIgnoreCase("adddoorblock")){
							if(args.length == 5){
								if(Utils.isInteger(args[1]) == true && Utils.isInteger(args[3]) == true && Utils.isInteger(args[4]) == true){
									Location loc = player.getTargetBlock(null, 7).getLocation();
									int area = Integer.parseInt(args[1]);
									String map = args[2];
									int door = Integer.parseInt(args[3]);
									int price = Integer.parseInt(args[4]);
									
									this.getConfig().set("maps." + map + ".areas." + area + ".doors." + door + ".price", price);
									List<String> blocks = this.getConfig().getStringList("maps." + map + ".areas." + area + ".doors." + door + ".blocks");
									blocks.add(loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
									this.getConfig().set("maps." + map + ".areas." + area + ".doors." + door + ".blocks", blocks);
									this.saveConfig();
									
									zplayer.sendMessage("You have added a door block to door #" + red + door + gray + ".");
								}else{
									zplayer.sendError("You have entered an invalid id.");
								}
							}else{
								zplayer.sendError("Usage: /" + cmd.getName() + " adddoorblock <area id> <map name> <door id> <price>");
							}
						}else if(args[0].equalsIgnoreCase("setzombiespawn")){
							if(args.length == 4){
								if(Utils.isInteger(args[1]) == true && Utils.isInteger(args[3]) == true){
									int area = Integer.parseInt(args[1]);
									String map = args[2];
									int spawn = Integer.parseInt(args[3]);
									
									this.getConfig().set("maps." + map + ".areas." + area + ".spawns." + spawn + ".x", player.getLocation().getX());
									this.getConfig().set("maps." + map + ".areas." + area + ".spawns." + spawn + ".y", player.getLocation().getY());
									this.getConfig().set("maps." + map + ".areas." + area + ".spawns." + spawn + ".z", player.getLocation().getZ());
									this.saveConfig();
									
									zplayer.sendMessage("You have set zombie spawn #" + red + spawn + gray + ".");
								}else{
									zplayer.sendError("You have entered an invalid id.");
								}
							}else{
								zplayer.sendError("Usage: /" + cmd.getName() + " setzombiespawn <area id> <map name> <spawn id>");
							}
						}else if(args[0].equalsIgnoreCase("setspawn")){
							if(args.length == 2){
								String map = args[1];
								
								this.getConfig().set("maps." + map + ".spawn.x", player.getLocation().getX());
								this.getConfig().set("maps." + map + ".spawn.y", player.getLocation().getY());
								this.getConfig().set("maps." + map + ".spawn.z", player.getLocation().getZ());
								this.getConfig().set("maps." + map + ".spawn.yaw", player.getLocation().getYaw());
								this.getConfig().set("maps." + map + ".spawn.pitch", player.getLocation().getPitch());
								this.saveConfig();
								
								zplayer.sendMessage("You have set the spawn.");
							}else{
								zplayer.sendError("Usage: /" + cmd.getName() + " setspawn <map name>");
							}
						}else if(args[0].equalsIgnoreCase("setwindow")){
							if(args.length == 4){
								if(Utils.isInteger(args[1]) == true && Utils.isInteger(args[3]) == true){
									Location loc = player.getTargetBlock(null, 7).getLocation();
									int area = Integer.parseInt(args[1]);
									String map = args[2];
									int window = Integer.parseInt(args[3]);
									
									this.getConfig().set("maps." + map + ".areas." + area + ".windows." + window + ".x", loc.getBlockX());
									this.getConfig().set("maps." + map + ".areas." + area + ".windows." + window + ".y", loc.getBlockY());
									this.getConfig().set("maps." + map + ".areas." + area + ".windows." + window + ".z", loc.getBlockZ());
									this.saveConfig();
									
									zplayer.sendMessage("You have set window #" + red + window + gray + ".");
								}else{
									zplayer.sendError("You have entered an invalid id.");
								}
							}else{
								zplayer.sendError("Usage: /" + cmd.getName() + " setwindow <area id> <map name> <window id>");
							}
						}else if(args[0].equalsIgnoreCase("weapons")){
							Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY + "Weapons");
							for(Weapon w : Weapon.values()){
								inv.addItem(w.getItemStack());
							}
							player.openInventory(inv);
						}else if(args[0].equalsIgnoreCase("resetvotes")){
							game.setVoteables();
							zplayer.sendMessage("You have reset the voting maps.");
						}else if(args[0].equalsIgnoreCase("givebrains")){
							zplayer.setBrains(zplayer.getBrains() + 5);
							zplayer.sendMessage("You were given " + red + 5 + gray + " brains.");
						}else if(args[0].equalsIgnoreCase("givepoints")){
							zplayer.addScore(500);
							zplayer.sendMessage("You were given " + red + 500 + gray + " points.");
						}else if(args[0].equalsIgnoreCase("sendstatus")){
							Utils.sendStatus();
							zplayer.sendMessage("Sent the status.");
						}else{
							zplayer.sendError("Invalid arguments.");
						}
					}else{
						zplayer.sendError("Invalid arguments.");
					}
				}
			}
		}
		return false;
	}

}
