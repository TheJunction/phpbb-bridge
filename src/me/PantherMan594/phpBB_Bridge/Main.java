package me.PantherMan594.phpBB_Bridge;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {
	
	public final Logger logger = Logger.getLogger("Minecraft");
	public static Main plugin;
	
	private static Connection connection;
	
	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		if (!new File(getDataFolder(), "config.yml").exists()) {
		     saveDefaultConfig();
		}
		setupMysql();
		PluginDescriptionFile pdfFile = getDescription();
		this.logger.info(pdfFile.getName() + " Version " + pdfFile.getVersion() + " Has Been Enabled!");
	}
	
	@Override
	public void onDisable() {
		try {
			if (connection != null && !connection.isClosed())
				connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			PluginDescriptionFile pdfFile = getDescription();
			this.reloadConfig();
			this.logger.info(pdfFile.getName() + " Has Been Disabled!");
		}
	}

	public synchronized void openConnection() {
		try {
			connection = DriverManager.getConnection("jdbc:mysql://" + getConfig().getString("ip") + ":" + getConfig().getInt("port") + "/" + getConfig().getString("tablename"),getConfig().getString("user"),getConfig().getString("pass"));
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized void setupMysql() {
		openConnection();
		try {
			int fieldId = getConfig().getInt("numcustfield") + 1;
			PreparedStatement sqlPrepA = connection.prepareStatement("ALTER TABLE `" + getConfig().getString("prefix") + "profile_fields_data` ADD COLUMN `command_register` INT(1) UNSIGNED NOT NULL DEFAULT 0;");
			PreparedStatement sqlPrepB = connection.prepareStatement("ALTER TABLE `" + getConfig().getString("prefix") + "profile_fields_data` ADD COLUMN `pf_username` VARCHAR(255) NULL;");
			PreparedStatement sqlPrepC = connection.prepareStatement("INSERT IGNORE INTO `" + getConfig().getString("prefix") + "profile_fields` SET `field_id` = '" + fieldId + "', `field_name` = 'username', `field_type` = 'profilefields.type.string', `field_ident` = 'username', `field_length` = '16', `field_minlen` = '3', `field_maxlen` = '17', `field_validation` = '[\\w]+', `field_required` = '1', `field_show_novalue` = '0', `field_show_on_reg` = '1', `field_show_on_vt` = '1', `field_active` = '1', `field_order` = '" + fieldId + "';");
			PreparedStatement sqlPrepD = connection.prepareStatement("INSERT IGNORE INTO `" + getConfig().getString("prefix") + "profile_lang` SET `field_id` = '" + fieldId + "', `lang_id` = '1', `lang_name` = 'Minecraft Username', `lang_explain` = 'Type in your Minecraft Username WITH THE RIGHT CASE!';");
			sqlPrepA.execute();
			sqlPrepB.execute();
			sqlPrepC.execute();
			sqlPrepD.execute();
			sqlPrepA.close();
			sqlPrepB.close();
			sqlPrepC.close();
			sqlPrepD.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized static void closeConnection() {
		try {
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized boolean playerDataContainsPlayer(Player player) {
		try {
			PreparedStatement sql = connection.prepareStatement("SELECT * FROM `" + getConfig().getString("prefix") + "profile_fields_data` WHERE `pf_username`=?;");
			sql.setString(1, player.getName());
			ResultSet resultSet = sql.executeQuery();
			boolean containsPlayer = resultSet.next();
			
			sql.close();
			resultSet.close();
			
			return containsPlayer;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	@EventHandler
	public void onPlayerLogin(PlayerLoginEvent event) {
		openConnection();
		try {
			if (playerDataContainsPlayer(event.getPlayer())) {
				int isFirst = 0;
				PreparedStatement sql = connection.prepareStatement("SELECT `command_register` FROM `" + getConfig().getString("prefix") + "profile_fields_data` WHERE `pf_username`=?;");
				sql.setString(1, event.getPlayer().getName());
				ResultSet result = sql.executeQuery();
				result.next();
				isFirst = result.getInt("command_register");
				event.getPlayer().sendMessage("isFirst" + isFirst);
				if (isFirst == 0) {
					List<String> regComs = new ArrayList<String>();
					regComs = getConfig().getStringList("register");
					for (String regCom : regComs) {
						Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), regCom.replaceAll("%player%", "" + event.getPlayer().getName() + "").replaceAll("\"", "\""));
					}
					PreparedStatement firstUpdate = connection.prepareStatement("UPDATE `" + getConfig().getString("prefix") + "profile_fields_data` SET `command_register`=1 WHERE `pf_username`=?;");
					firstUpdate.setString(1, event.getPlayer().getName());
					firstUpdate.executeUpdate();
					firstUpdate.close();
					sql.close();
					result.close();
				} else {
					sql.close();
					result.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			closeConnection();
		}
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		Player player = (Player)sender;
		if (commandLabel.equalsIgnoreCase("phpbb")) {
			if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
				if (player.isOp()) {
					this.reloadConfig();
					player.sendMessage("Config Reloaded!");
				} else {
					player.sendMessage(ChatColor.DARK_RED + "You do not have permission for this command.");
					
				}
			} else {
				player.sendMessage(ChatColor.RED + "Usage: /register reload");
			}
		}
		return false;
	}
}