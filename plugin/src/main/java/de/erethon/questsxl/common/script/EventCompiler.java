package de.erethon.questsxl.common.script;

import de.erethon.erethonscript.ast.AssignmentNode;
import de.erethon.erethonscript.ast.BlockNode;
import de.erethon.erethonscript.ast.Statement;
import de.erethon.erethonscript.execution.CompilationContext;
import de.erethon.erethonscript.execution.ComponentCompiler;
import de.erethon.erethonscript.execution.ScriptCompiler;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.livingworld.QEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EventCompiler implements ComponentCompiler {

    private final ActionCompiler actionCompiler;

    public EventCompiler(ActionCompiler actionCompiler) {
        this.actionCompiler = actionCompiler;
    }

    @Override
    public void compile(BlockNode node, ScriptCompiler compiler, CompilationContext context) {
        if (!node.getType().equals("event")) return;

        String eventId = node.getIdentifiers().getFirst();
        QEvent event = new QEvent(eventId);
        context.push(event);

        Map<String, Object> simpleProps = new HashMap<>();
        if (node.getBody() != null) {
            for (Statement statement : node.getBody()) {
                if (statement instanceof AssignmentNode assignment) {
                    simpleProps.put(assignment.getLValue().getIdentifier(),
                            compiler.compileExpression(assignment.getValue(), null).evaluate(null));
                } else if (statement instanceof BlockNode block) {
                    switch (block.getType()) {
                        case "startLocation" -> compileLocation(event, block, compiler);
                        case "onStart" -> {
                            for (Statement actionStmt : block.getBody()) {
                                QAction action = actionCompiler.compileActionBlock((BlockNode) actionStmt, compiler, context);
                                event.addStartAction(action);
                            }
                        }
                        case "onUpdate" -> {
                            for (Statement actionStmt : block.getBody()) {
                                QAction action = actionCompiler.compileActionBlock((BlockNode) actionStmt, compiler, context);
                                event.addUpdateAction(action);
                            }
                        }
                        case "reward" -> {
                            int score = Integer.parseInt(block.getIdentifiers().getFirst());
                            Set<QAction> rewardActions = new HashSet<>();
                            for (Statement actionStmt : block.getBody()) {
                                rewardActions.add(actionCompiler.compileActionBlock((BlockNode) actionStmt, compiler, context));
                            }
                            event.getRewards().put(score, rewardActions);
                        }
                        case "stage", "condition" -> compiler.compileStatement(block, context);
                        default -> throw new IllegalStateException("Unsupported block '" + block.getType() + "' inside an event.");
                    }
                }
            }
        }
        configureEvent(event, simpleProps);
        context.pop();
        QuestsXL.getInstance().getEventManager().addEvent(event);
    }

    private void configureEvent(QEvent event, Map<String, Object> props) {
        event.setDisplayName((String) props.getOrDefault("displayName", event.getId()));
        if (props.containsKey("cooldown")) event.setCooldown(((Number) props.get("cooldown")).intValue());
        if (props.containsKey("range")) event.setRange(((Number) props.get("range")).intValue());
        if (props.containsKey("canActivateRange")) event.setCanActivateRange(((Number) props.get("canActivateRange")).intValue());
        if (props.containsKey("giveAllRewards")) event.setGiveAllRewards((Boolean) props.get("giveAllRewards"));
    }

    private void compileLocation(QEvent event, BlockNode node, ScriptCompiler compiler) {
        Map<String, Object> locProps = new HashMap<>();
        if (node.getBody() != null) {
            for (Statement statement : node.getBody()) {
                if (statement instanceof AssignmentNode a) {
                    locProps.put(a.getLValue().getIdentifier(), compiler.compileExpression(a.getValue(), null).evaluate(null));
                }
            }
        }
        String worldName = (String) locProps.getOrDefault("world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) throw new IllegalStateException("World '" + worldName + "' not found.");

        double x = ((Number) locProps.get("x")).doubleValue();
        double y = ((Number) locProps.get("y")).doubleValue();
        double z = ((Number) locProps.get("z")).doubleValue();
        event.setCenterLocation(new Location(world, x, y, z));
    }
}