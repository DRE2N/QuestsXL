package de.erethon.questsxl.objective;

import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.event.CommandTriggerEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Event;

@QLoadableDoc(
        value = "server_command",
        description = "This objective is completed when a command is executed on the server, for example by a command block",
        shortExample = "server_command: identifier=q trigger 33333",
        longExample = {
                "server_command:",
                "  identifier: q trigger 3333"
        }
)
public class ServerCommandObjective extends QBaseObjective<CommandTriggerEvent> {

    @QParamDoc(name = "identifier", description = "The command that has to be executed", required = true)
    String identifier;

    @Override
    public void check(ActiveObjective objective, CommandTriggerEvent e) {
        if (!conditions(e.getPlayer())) return;
        if (identifier.equals(e.getID())) {
            checkCompletion(objective, this, plugin.getPlayerCache().getByPlayer(e.getPlayer()));
        }

    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        identifier = cfg.getString("identifier");
    }

    @Override
    public Class<CommandTriggerEvent> getEventType() {
        return CommandTriggerEvent.class;
    }
}
