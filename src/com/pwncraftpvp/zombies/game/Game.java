package com.pwncraftpvp.zombies.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;

import com.pwncraftpvp.zombies.core.Main;
import com.pwncraftpvp.zombies.core.ZPlayer;
import com.pwncraftpvp.zombies.tasks.CountdownTask;
import com.pwncraftpvp.zombies.tasks.PlayerDeathTask;
import com.pwncraftpvp.zombies.tasks.PlayerHealTask;
import com.pwncraftpvp.zombies.tasks.ReloadTask;
import com.pwncraftpvp.zombies.tasks.SpawnTask;
import com.pwncraftpvp.zombies.tasks.WindowRepairTask;
import com.pwncraftpvp.zombies.tasks.WindowTask;
import com.pwncraftpvp.zombies.utils.Utils;

public class Game {
	
	private Main main = Main.getInstance();
	private String gray = ChatColor.GRAY + "";
	private String red = ChatColor.RED + "";
	
	private int round = 0;
	private int health = 0;
	private Map map = null;
	private boolean power = false;
	private Status status = Status.WAITING;
	
	private List<Door> doors = null;
	private List<Window> windows = null;
	private List<Area> unlockedareas = new ArrayList<Area>();
	
	public int killed = 0;
	public boolean ending = false;
	public SpawnTask spawntask = null;
	public WindowTask windowtask = null;
	public CountdownTask votingtask = null;
	
	public List<Map> voteables = new ArrayList<Map>();
	public List<String> voted = new ArrayList<String>();
	public List<String> shooting = new ArrayList<String>();
	public List<String> speedcola = new ArrayList<String>();
	public List<Integer> nodamage = new ArrayList<Integer>();
	public List<String> deadplayers = new ArrayList<String>();
	
	public HashMap<String, Ammo> primary = new HashMap<String, Ammo>();
	public HashMap<String, Ammo> secondary = new HashMap<String, Ammo>();
	public HashMap<String, Integer> votes = new HashMap<String, Integer>();
	public HashMap<String, Integer> brains = new HashMap<String, Integer>();
	public HashMap<String, Integer> scores = new HashMap<String, Integer>();
	public HashMap<String, Location> dead = new HashMap<String, Location>();
	public HashMap<String, ChatColor> colors = new HashMap<String, ChatColor>();
	public HashMap<String, ReloadTask> reload = new HashMap<String, ReloadTask>();
	public HashMap<Integer, Integer> windowhealth = new HashMap<Integer, Integer>();
	public HashMap<String, PlayerHealTask> heal = new HashMap<String, PlayerHealTask>();
	public HashMap<String, PlayerDeathTask> death = new HashMap<String, PlayerDeathTask>();
	public HashMap<String, WindowRepairTask> repair = new HashMap<String, WindowRepairTask>();
	
	/**
	 * Get the game's status
	 * @return The game's status
	 */
	public Status getStatus(){
		return status;
	}
	
	/**
	 * Set the game's status
	 * @param status - The game's status
	 */
	public void setStatus(Status status){
		this.status = status;
		Utils.sendStatus();
	}
	
	/**
	 * Set the voteables
	 */
	public void setVoteables(){
		voteables.clear();
		votes.clear();
		Random rand = new Random();
		while(voteables.size() < main.maps.size() && voteables.size() < 5){
			Map map = main.maps.get(rand.nextInt(main.maps.size()));
			if(this.isVoteable(map) == false){
				voteables.add(map);
				votes.put(map.getName(), 0);
			}
		}
	}
	
	/**
	 * Check if a map is a voteable
	 * @param map - The map to check
	 * @return True if the map is a voteable, false if not
	 */
	public boolean isVoteable(Map map){
		boolean voteable = false;
		for(Map m : voteables){
			if(m.getName().equalsIgnoreCase(map.getName()) == true){
				voteable = true;
				break;
			}
		}
		return voteable;
	}
	
	/**
	 * Get the game's current map
	 * @return The game's current map
	 */
	public Map getMap(){
		if(map == null){
			String name = null;
			int value = -1;
			for(Entry<String, Integer> entry : votes.entrySet()){
			    String k = entry.getKey();
			    Integer v = entry.getValue();
			    if(v > value){
			    	name = k;
			    	value = v;
			    }
			}
			votes.clear();
			map = new Map(name);
		}
		return map;
	}
	
	/**
	 * Get the game's round
	 * @return The game's round
	 */
	public int getRound(){
		return round;
	}
	
	/**
	 * Set the game's round
	 * @param value - The game's round
	 */
	public void setRound(int value){
		round = value;
	}
	
	/**
	 * Check if the power is on
	 * @return True if the power is on, false if not
	 */
	public boolean isPowerOn(){
		return power;
	}
	
	/**
	 * Set the power on
	 */
	public void setPowerOn(){
		power = true;
	}
	
	/**
	 * Get the zombie health
	 */
	public int getZombieHealth(){
		return health;
	}
	
	/**
	 * Increase the zombie health
	 */
	public void increaseHealth(){
		if(round == 1){
			health = 150;
		}else if(round < 10){
			health += 100;
		}else{
			health = (int) (health * 0.1);
		}
	}
	
	/**
	 * Get a random spawn
	 * @return A random spawn
	 */
	public Location getRandomSpawn(){
		Random rand = new Random();
		Location loc = null;
		while(loc == null){
			for(Area a : unlockedareas){
				for(Location l : a.getSpawns()){
					if(rand.nextDouble() <= 0.1){
						loc = l;
						break;
					}
				}
			}
		}
		return loc;
	}
	
	/**
	 * Get a random zombie speed
	 * @return A random zombie speed
	 */
	public int getRandomSpeed(){
		int speed = 0;
		if(round > 2){
			Random rand = new Random();
			double chance = 0.1;
			if(round >= 10 && round < 20){
				chance = 0.2;
			}else if(round >= 20 && round < 30){
				chance = 0.3;
			}else if(round >= 30 && round < 40){
				chance = 0.4;
			}else if(round >= 40 && round < 50){
				chance = 0.5;
			}
			if(rand.nextDouble() <= chance){
				speed = 1;
			}else if(rand.nextDouble() <= (chance / 2)){
				speed = 2;
			}
		}
		return speed;
	}
	
	/**
	 * Get the amount of living zombies
	 * @return The amount of living zombies
	 */
	public int getAliveZombies(){
		int alive = 0;
		for(Entity e : Utils.getWorld().getEntities()){
			if(e instanceof Zombie){
				alive++;
			}
		}
		return alive;
	}
	
	/**
	 * Get all doors in the map
	 * @return All doors in the map
	 */
	public List<Door> getAllDoors(){
		if(doors == null){
			doors = new ArrayList<Door>();
			for(Area a : map.getAreas()){
				for(Door d : a.getDoors()){
					doors.add(d);
				}
			}
		}
		return doors;
	}
	
	/**
	 * Get all windows in the map
	 * @return All windows in the map
	 */
	public List<Window> getAllWindows(){
		if(windows == null){
			windows = new ArrayList<Window>();
			for(Area a : map.getAreas()){
				for(Window w : a.getWindows()){
					windows.add(w);
				}
			}
		}
		return windows;
	}
	
	/**
	 * Get the window at a location
	 * @param loc - The location
	 * @return The window at the location (null if none)
	 */
	public Window getWindow(Location loc){
		Window window = null;
		for(Window w : this.getAllWindows()){
			if(w.getLocation().getBlockX() == loc.getBlockX() && w.getLocation().getBlockY() == loc.getBlockY() && w.getLocation().getBlockZ() == loc.getBlockZ()){
				window = w;
				break;
			}
		}
		return window;
	}
	
	/**
	 * Reset the map's doors
	 */
	public void resetDoors(){
		for(Area a : map.getAreas()){
			for(Door d : a.getDoors()){
				for(Block b : d.getBlocks()){
					b.setType(Material.IRON_FENCE);
				}
			}
		}
	}
	
	/**
	 * Reset the map's windows
	 */
	public void resetWindows(){
		for(Area a : map.getAreas()){
			for(Window w : a.getWindows()){
				w.getLocation().getBlock().setType(Material.IRON_FENCE);
			}
		}
	}
	
	/**
	 * Get an area from its id
	 * @param id - The id of the area
	 * @return The area
	 */
	public Area getArea(int id){
		Area area = null;
		for(Area a : map.getAreas()){
			if(a.getID() == id){
				area = a;
				break;
			}
		}
		return area;
	}
	
	/**
	 * Add an area to the unlocked list
	 * @param area - The area
	 */
	public void addUnlockedArea(Area area){
		unlockedareas.add(area);
	}
	
	/**
	 * Check if an area is unlocked
	 * @param area - The area
	 * @return True if the area is unlocked, false if not
	 */
	public boolean isUnlocked(int area){
		boolean unlocked = false;
		for(Area a : unlockedareas){
			if(a.getID() == area){
				unlocked = true;
				break;
			}
		}
		return unlocked;
	}
	
	/**
	 * Start the next round
	 */
	public void startRound(){
		round++;
		killed = 0;
		this.increaseHealth();
		
		Utils.broadcastSubtitle("Round " + red + this.round, 60);
		for(Player p : Bukkit.getOnlinePlayers()){
			p.playSound(p.getLocation(), Sound.WITHER_DEATH, 10, 1.5F);
		}
		
		SpawnTask spawntask = new SpawnTask();
		spawntask.runTaskTimer(main, 0, Utils.getDelayForRound(round));
		this.spawntask = spawntask;
		
		WindowTask windowtask = new WindowTask();
		windowtask.runTaskTimer(main, 0, 5);
		this.windowtask = windowtask;
	}
	
	/**
	 * End the current round
	 */
	public void endRound(){
		if(spawntask != null){
			spawntask.cancel();
		}
		if(windowtask != null){
			windowtask.cancel();
		}
		Utils.broadcastSubtitle("Round over", 60);
		for(Player p : Bukkit.getOnlinePlayers()){
			ZPlayer zp = new ZPlayer(p);
			if(deadplayers.contains(p.getName()) == false){
				zp.giveBrains(5);
			}else{
				zp.toggleSpectating(false);
				p.teleport(map.getSpawn());
			}
		}
		deadplayers.clear();
		main.getServer().getScheduler().scheduleSyncDelayedTask(main, new Runnable(){
			public void run(){
				startRound();
			}
		}, 200);
	}
	
	/**
	 * Start the game
	 */
	public void start(){
		Map map = this.getMap();
		Utils.removeEntities();
		this.resetDoors();
		this.resetWindows();
		this.setStatus(Status.STARTED);
		
		for(Area a : map.getAreas()){
			if(a.getDoors().size() == 0){
				unlockedareas.add(a);
			}
		}
		
		for(Window w : this.getAllWindows()){
			windowhealth.put(w.getID(), 6);
		}
		
		Utils.broadcastMessage("The highest voted map was " + red + map.getName() + gray + ".");
		Utils.broadcastMessage("Prepare for the first round.");
		
		int count = 1;
		for(Player p : Bukkit.getOnlinePlayers()){
			colors.put(p.getName(), Utils.getChatColor(count));
			brains.put(p.getName(), 0);
			ZPlayer zp = new ZPlayer(p);
			zp.setInventory(status);
			scores.put(p.getName(), 500);
			p.teleport(map.getSpawn());
			p.setHealth(p.getMaxHealth());
			count++;
		}
		
		Utils.updateScoreboards();
		
		main.getServer().getScheduler().scheduleSyncDelayedTask(main, new Runnable(){
			public void run(){
				startRound();
			}
		}, 100);
	}
	
	/**
	 * End the game
	 */
	public void end(){
		ending = true;
		
		if(spawntask != null){
			spawntask.cancel();
		}
		if(windowtask != null){
			windowtask.cancel();
		}
		
		Utils.removeEntities();
		Utils.broadcastTitle("Game over", "Survived " + red + round + gray + " rounds.", 90);
		for(Player p : Bukkit.getOnlinePlayers()){
			ZPlayer zp = new ZPlayer(p);
			zp.sendMessage("You earned a total of " + red + brains.get(p.getName()) + gray + " brains this game.");
		}
		
		main.getServer().getScheduler().scheduleSyncDelayedTask(main, new Runnable(){
			public void run(){
				for(Player p : Bukkit.getOnlinePlayers()){
					ZPlayer zp = new ZPlayer(p);
					zp.logout();
					Utils.connect(p, "hub");
				}
				setStatus(Status.WAITING);
				main.stats.clear();
				if(spawntask != null){
					spawntask.cancel();
				}
				resetDoors();
				resetWindows();
				main.game = new Game();
			}
		}, 100);
	}

}
