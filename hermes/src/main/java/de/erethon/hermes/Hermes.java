package de.erethon.hermes;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class Hermes extends JavaPlugin {

    private HermesRoleService roleService;
    private HermesGitExportService gitExportService;
    private HermesWebServer webServer;

    @Override
    public void onEnable() {
        ensureConfigDefaults();
        roleService = new HermesRoleService(this);
        roleService.ensureSchema().join();
        gitExportService = new HermesGitExportService(this);

        FileConfiguration config = getConfig();
        if (!config.getBoolean("enabled", false)) {
            getLogger().info("Hermes web authoring panel is disabled.");
            return;
        }
        startWebServer(config);
    }

    @Override
    public void onDisable() {
        if (webServer != null) {
            webServer.stop();
            webServer = null;
        }
    }

    public HermesRoleService getRoleService() {
        return roleService;
    }

    public HermesGitExportService getGitExportService() {
        return gitExportService;
    }

    private void startWebServer(FileConfiguration config) {
        try {
            if (webServer != null) {
                webServer.stop();
            }
            String host = config.getString("host", "0.0.0.0");
            int port = config.getInt("port", 8081);
            webServer = new HermesWebServer(this, config);
            webServer.start(host, port);
        } catch (Exception e) {
            getLogger().warning("Failed to initialize Hermes web authoring panel: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void ensureConfigDefaults() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        config.addDefault("enabled", false);
        config.addDefault("host", "0.0.0.0");
        config.addDefault("port", 8081);
        config.addDefault("staticPath", "");
        config.addDefault("maxModelUploadBytes", 26214400);
        config.addDefault("git.enabled", false);
        config.addDefault("git.repoUrl", "");
        config.addDefault("git.branch", "dev");
        config.addDefault("git.token", "");
        config.addDefault("git.authorName", "Hermes");
        config.addDefault("git.authorEmail", "hermes@erethon.de");
        config.addDefault("git.clonePath", "git-export");
        config.options().copyDefaults(true);
        saveConfig();
        reloadConfig();
    }
}
