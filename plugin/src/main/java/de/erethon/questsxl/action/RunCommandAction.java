package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

@QLoadableDoc(
        value = "run_command",
        description = "Runs a command, optionally with full permissions (op) or as console. By default, the command is run as the player. Use %player% in the command to refer to the player's name.",
        shortExample = "run_command: command=stop; console=true # Shut down the server",
        longExample = {
                "run_command:",
                "  command: example_command",
                "  op: false",
                "  console: false"
        }
)
public class RunCommandAction extends QBaseAction {

    @QParamDoc(name = "command", description = "The command to run, without the /", required = true)
    private String command;
    @QParamDoc(name = "op", description = "Whether to run the command as an op", def = "false")
    private boolean op = false;
    @QParamDoc(name = "console", description = "Whether to run the command as console", def = "false")
    private boolean console = false;

    @Override
    public void play(Quester quester) {
        if (!conditions(quester)) return;
        execute(quester, this::runCommand);
        onFinish(quester);
    }

    private void runCommand(QPlayer player) {
        Player p = player.getPlayer();
        String commandToRun = command.replace("%player%", p.getName());
        if (console) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandToRun);
        } else {
            if (op) {
                boolean isOp = p.isOp();
                p.setOp(true);
                p.performCommand(commandToRun);
                p.setOp(isOp);
            } else {
                p.performCommand(commandToRun);
            }
        }
    }

    @Override
    public Material getIcon() {
        return Material.COMMAND_BLOCK;
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        command = cfg.getString("command");
        op = cfg.getBoolean("op", false);
        console = cfg.getBoolean("console", false);
    }
}
