package de.erethon.questsxl.gui;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.quest.ActiveQuest;
import de.erethon.questsxl.quest.QQuest;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;

public class QuestBook {

    public static void write(Player player) {
        Component bookTitle = Component.text("Quests");
        Component bookAuthor = Component.text("ffsg");
        Collection<Component> bookPages = new ArrayList<>();
        Component page = Component.empty();
        for (QQuest quest : QuestsXL.get().getQuestManager().getQuests()) {
            Component questEntry = Component.text(" - ").color(TextColor.color(20, 20, 20));
            questEntry = questEntry.append(Component.text(quest.getName()));
            Component hover = Component.empty();
            hover = hover.append(Component.text(quest.getDisplayName()).color(TextColor.color(0, 0, 255)));
            hover = hover.append(Component.newline());
            hover = hover.append(Component.text(quest.getDescription())).color(TextColor.color(255, 255, 255)).decorate(TextDecoration.ITALIC).append(Component.newline());
            hover = hover.append(Component.text(stageDesc(player, quest)));
            questEntry = questEntry.hoverEvent(HoverEvent.showText(hover));
            questEntry = questEntry.append(Component.newline());
            page = page.append(questEntry);
        }
        bookPages.add(page);

        Book myBook = Book.book(bookTitle, bookAuthor, bookPages);
        //player.openBook(myBook);
    }

    public static String stageDesc(Player player, QQuest quest) {
        ActiveQuest active = null;
        for (ActiveQuest activeQuest : QuestsXL.get().getPlayerCache().getByPlayer(player).getActiveQuests().keySet()) {
            if (activeQuest.getQuest() == quest) {
                active = activeQuest;
            }
        }
        if (active == null) {
            return "";
        }
        return active.getCurrentStage().getDescription();
    }
}
