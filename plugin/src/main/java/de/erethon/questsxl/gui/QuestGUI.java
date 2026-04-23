package de.erethon.questsxl.gui;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.component.objective.ActiveObjective;
import de.erethon.questsxl.component.objective.QObjective;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.quest.ActiveQuest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class QuestGUI implements InventoryHolder, Listener {

    private static final int SLOTS_PER_PAGE = 45;
    private static final int NAV_PREV       = 45;
    private static final int NAV_INFO       = 49;
    private static final int NAV_NEXT       = 53;

    private static final TextColor COLOR_TRACKED   = TextColor.fromCSSHexString("#3fda52");
    private static final TextColor COLOR_UNTRACKED = TextColor.fromCSSHexString("#d8d8d8");
    private static final TextColor COLOR_DIM       = TextColor.fromCSSHexString("#808080");
    private static final TextColor COLOR_OBJ       = TextColor.fromCSSHexString("#b0b0b0");
    private static final TextColor COLOR_DONE      = TextColor.fromCSSHexString("#4a9e5c");
    private static final TextColor COLOR_HINT      = TextColor.fromCSSHexString("#ffcc44");
    private static final TextColor COLOR_NAV       = TextColor.fromCSSHexString("#cccccc");
    private static final TextColor COLOR_NAV_DIM   = TextColor.fromCSSHexString("#555555");

    private final QPlayer qPlayer;
    private final List<ActiveQuest> quests;
    private int page = 0;
    private Inventory inventory;

    public QuestGUI(QPlayer qPlayer) {
        this.qPlayer = qPlayer;
        this.quests = new ArrayList<>(qPlayer.getActiveQuests().keySet());
        sortQuests();
        QuestsXL.get().getServer().getPluginManager().registerEvents(this, QuestsXL.get());
    }

    private void sortQuests() {
        quests.sort(Comparator
                .<ActiveQuest, Boolean>comparing(q -> q != qPlayer.getTrackedQuest())
                .thenComparing(q -> q.getQuest().displayName() != null
                        ? q.getQuest().displayName().getAsString()
                        : q.getQuest().id()));
    }

    public void open() {
        Component title = Component.translatable("qxl.gui.quest.title")
                .color(COLOR_TRACKED)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false);
        inventory = Bukkit.createInventory(this, 54, title);
        refresh();
        qPlayer.getPlayer().openInventory(inventory);
    }

    public void refresh() {
        inventory.clear();
        fillQuestSlots();
        fillNavigationRow();
    }

    private void fillQuestSlots() {
        if (quests.isEmpty()) {
            inventory.setItem(22, labeled(Material.BARRIER,
                    Component.translatable("qxl.gui.quest.empty")
                            .color(TextColor.fromCSSHexString("#ff5555"))
                            .decoration(TextDecoration.ITALIC, false),
                    List.of()));
            return;
        }
        int start = page * SLOTS_PER_PAGE;
        for (int i = 0; i < SLOTS_PER_PAGE && (start + i) < quests.size(); i++) {
            inventory.setItem(i, buildQuestItem(quests.get(start + i)));
        }
    }

    private ItemStack buildQuestItem(ActiveQuest quest) {
        boolean tracked = quest == qPlayer.getTrackedQuest();

        Component rawName = quest.getQuest().displayName() != null
                ? GlobalTranslator.render(quest.getQuest().displayName().get(), qPlayer.getPlayer().locale())
                : Component.text(quest.getQuest().id());
        Component name = rawName
                .color(tracked ? COLOR_TRACKED : COLOR_UNTRACKED)
                .decoration(TextDecoration.BOLD, tracked)
                .decoration(TextDecoration.ITALIC, false);

        List<Component> lore = new ArrayList<>();
        Component blank = Component.empty().decoration(TextDecoration.ITALIC, false);

        String description = quest.getCurrentStage() != null ? quest.getCurrentStage().getDescription() : null;
        if (description != null && !description.isBlank()) {
            lore.add(Component.text(description).color(COLOR_DIM).decoration(TextDecoration.ITALIC, false));
            lore.add(blank);
        }

        List<Component> objLines = buildObjectiveLines(quest);
        if (!objLines.isEmpty()) {
            lore.addAll(objLines);
            lore.add(blank);
        }

        if (tracked) {
            lore.add(Component.translatable("qxl.gui.quest.tracked").color(COLOR_TRACKED).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.translatable(tracked ? "qxl.gui.quest.click.untrack" : "qxl.gui.quest.click.track")
                .color(COLOR_HINT).decoration(TextDecoration.ITALIC, false));

        return labeled(tracked ? Material.ENCHANTED_BOOK : Material.BOOK, name, lore);
    }

    private List<Component> buildObjectiveLines(ActiveQuest quest) {
        List<Component> lines = new ArrayList<>();
        int shown = 0;
        for (ActiveObjective ao : qPlayer.getCurrentObjectives()) {
            if (ao.getCompletable() != quest.getQuest()) continue;
            QObjective<?> obj = ao.getObjective();
            if (obj.isHidden() || obj.isPersistent()) continue;
            if (shown >= 4) {
                lines.add(Component.translatable("qxl.gui.quest.objective.more").color(COLOR_DIM).decoration(TextDecoration.ITALIC, false));
                break;
            }
            Component objText = GlobalTranslator.render(
                    obj.getDisplayText(qPlayer.getPlayer()).get(), qPlayer.getPlayer().locale());
            String progressSuffix = obj.getProgressGoal() > 1
                    ? " (" + ao.getProgress() + "/" + obj.getProgressGoal() + ")" : "";
            boolean done = ao.isCompleted();
            lines.add(
                    Component.text(done ? "✔ " : "• ").color(done ? COLOR_DONE : COLOR_DIM)
                            .decoration(TextDecoration.ITALIC, false)
                            .append(objText.color(done ? COLOR_DONE : COLOR_OBJ).decoration(TextDecoration.ITALIC, false))
                            .append(Component.text(progressSuffix).color(COLOR_DIM).decoration(TextDecoration.ITALIC, false)));
            shown++;
        }
        return lines;
    }

    private void fillNavigationRow() {
        ItemStack filler = labeled(Material.GRAY_STAINED_GLASS_PANE,
                Component.empty().decoration(TextDecoration.ITALIC, false), List.of());
        for (int i = 45; i < 54; i++) inventory.setItem(i, filler);

        int totalPages = Math.max(1, (int) Math.ceil((double) quests.size() / SLOTS_PER_PAGE));

        inventory.setItem(NAV_PREV, page > 0
                ? labeled(Material.ARROW, Component.translatable("qxl.gui.quest.nav.prev").color(COLOR_NAV).decoration(TextDecoration.ITALIC, false), List.of())
                : labeled(Material.GRAY_STAINED_GLASS_PANE, Component.translatable("qxl.gui.quest.nav.prev").color(COLOR_NAV_DIM).decoration(TextDecoration.ITALIC, false), List.of()));

        inventory.setItem(NAV_INFO, labeled(Material.PAPER,
                Component.translatable("qxl.gui.quest.nav.page", Component.text(page + 1), Component.text(totalPages))
                        .color(COLOR_NAV).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, false),
                List.of(Component.translatable("qxl.gui.quest.nav.count", Component.text(quests.size()))
                        .color(COLOR_DIM).decoration(TextDecoration.ITALIC, false))));

        inventory.setItem(NAV_NEXT, (page + 1) * SLOTS_PER_PAGE < quests.size()
                ? labeled(Material.ARROW, Component.translatable("qxl.gui.quest.nav.next").color(COLOR_NAV).decoration(TextDecoration.ITALIC, false), List.of())
                : labeled(Material.GRAY_STAINED_GLASS_PANE, Component.translatable("qxl.gui.quest.nav.next").color(COLOR_NAV_DIM).decoration(TextDecoration.ITALIC, false), List.of()));
    }

    private ItemStack labeled(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        if (!lore.isEmpty()) meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) return;
        event.setCancelled(true);
        if (event.getClickedInventory() != inventory) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        handleClick(event.getSlot());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() == this) event.setCancelled(true);
    }

    private void handleClick(int slot) {
        if (slot == NAV_PREV && page > 0) {
            page--;
            refresh();
            return;
        }
        if (slot == NAV_NEXT && (page + 1) * SLOTS_PER_PAGE < quests.size()) {
            page++;
            refresh();
            return;
        }
        if (slot >= SLOTS_PER_PAGE) return;

        int index = page * SLOTS_PER_PAGE + slot;
        if (index >= quests.size()) return;

        ActiveQuest clicked = quests.get(index);
        if (qPlayer.getTrackedQuest() == clicked) {
            qPlayer.setTrackedQuest(null, 99);
        } else {
            qPlayer.setTrackedQuest(clicked.getQuest(), 99);
        }
        sortQuests();
        refresh();
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
