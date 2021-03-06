package fr.az.perfimpact.commands;

import fr.az.perfimpact.Main;
import fr.az.perfimpact.util.Node;
import fr.az.perfimpact.util.TimerTask;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;

public class PerfImpactCommandExecutor implements CommandExecutor {

	private Main main;
	
	public PerfImpactCommandExecutor(Main main) {
		this.main = main;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equals("perfimpact")) {
			if (args.length == 0) {
				cmd.execute(sender, label, new String[] {"help"});
				return true;
			}
			
			StringBuilder path = new StringBuilder();
			
			for (int i = 1; i < args.length; i++) {
				path.append(args[i]);
			}
		
			if (args[0].equals("capture")) {
				if (args.length == 1) {
					sender.sendMessage(ChatColor.GOLD + "Capture enabled: " + ChatColor.DARK_GREEN + main.isCapturing());
					return true;
				}
				
				if (args[1].equals("start")) {
					main.setCapturing(true);
				}
				else if (args[1].equals("stop")) {
					main.setCapturing(false);
				}
				else cmd.execute(sender, label, new String[] {"help","capture"});
				
				return true;
			}
		
			if (args[0].equals("start")) {
				if (args.length == 1) {
					cmd.execute(sender, label, new String[] {"help","start"});
					return true;
				}
				
				main.timeCalled.put(path.toString(), main.timeCalled.getOrDefault(path.toString(), 0L) +1);
				
				main.timing.put(path.toString(), new Timer());
				main.timer.put(path.toString(), new TimerTask());
				
				main.timing.get(path.toString()).schedule(main.timer.get(path.toString()), 1L);
				
				sender.sendMessage(ChatColor.GOLD + "Timer successfully started");
				return true;
			}
		
			if (args[0].equals("stop")) {
				if (args.length == 1) {
					cmd.execute(sender,label,new String[] {"help","start"});
					return true;
				}
			
				main.timing.getOrDefault(path.toString(), new Timer()).cancel();
				main.timer.getOrDefault(path.toString(), new TimerTask()).cancel();
			
				long exeTime = main.timer.getOrDefault(path.toString(), new TimerTask()).getTime();
			
				ArrayList<Long> timeImpact = main.timeImpact.getOrDefault(path.toString(), new ArrayList<Long>());
				timeImpact.add(exeTime);
				main.timeImpact.put(path.toString(), timeImpact);
			
				main.timing.remove(path.toString());
				main.timer.remove(path.toString());
			
				sender.sendMessage(ChatColor.GOLD + "The function "+ path +" was executed in " + ChatColor.DARK_GREEN + exeTime +"s");
				return true;
			}
		
			if (args[0].equals("resume")) {
				boolean isCapturing = main.isCapturing();
				main.setCapturing(false);
			
				if (args.length == 1) {
			
					long totalExeTime = 0L;
				
					for (ArrayList<Long> temp : main.timeImpact.values()) {
						for (long impact : temp) {
							totalExeTime += impact;
						}
					}
				
					Node base = new Node("");
				
					for (String pathway : main.timeCalled.keySet()) {
					
						Node current = base;
						for (String tag : pathway.split("/")) {
						
							Recursive : while (true) {
								Node previous = current;
							
								for (Node child : (Node[]) current.getChildren().toArray()) {
									if (child.getPath().equals(tag)) {
										current = child;
									
										if (child.getPath().endsWith(".mcfunction")) {
											child.setCallCount(main.timeCalled.get(pathway));
										
											for (int i = 0; i < child.getCallCount(); i++) {
												child.setExecutionTime(child.getExecutionTime() +main.timeImpact.get(pathway).get(i));
											}
										
											child.setImpact(child.getExecutionTime() / totalExeTime);
											child.setExecutionTime(child.getExecutionTime() / child.getCallCount());
										}
									
										break Recursive;
									}
								}
								
								if (previous.equals(current)) {
									current.addChild(new Node(tag));
								}
							}
						}
					}
					
					ArrayList<String> Tree = base.getTree();
				
				
				
					try {
						FileWriter fw = new FileWriter("perfimpact-log.txt");
						for (String line : Tree) fw.write(line);
						fw.close();
					
					} catch (IOException e) {e.printStackTrace();}
				
					return true;
				}
			
				else {
					double mean = 0;
					for (long exeTime : main.timeImpact.get(path.toString())) {
						mean += exeTime;
					}
					mean /= main.timeImpact.get(path.toString()).size();
					
					long callCount = main.timeCalled.get(path.toString());
					ArrayList<Long> exeTime = main.timeImpact.get(path.toString());
					
					sender.sendMessage(ChatColor.DARK_PURPLE + "Function "+ path + "\n" + ChatColor.GOLD + "Mean of execution time: "+ ChatColor.DARK_RED + mean + ChatColor.GOLD + "; Total Calls: " + ChatColor.DARK_RED + callCount);
					sender.sendMessage(ChatColor.GOLD + "Execution Times: ");
				
					for (long i : exeTime) {
						sender.sendMessage("" + ChatColor.DARK_RED + i + ChatColor.GOLD + "; ");
					}
				
					main.setCapturing(isCapturing);
					return true;
				}
			}
		
			if (args[0] == "help") {
				sender.sendMessage(ChatColor.DARK_GREEN + "The help of '" + ChatColor.GOLD + path + ChatColor.DARK_GREEN +"' is coming soon !");
			return true;
			}
		}

		return false;
	}

}
