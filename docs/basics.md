# Vorbereitung
* Zum Bearbeiten von Dateien empfiehlt sich ein vernünftiger Texteditor wie [Notepad++](https://notepad-plus-plus.org/downloads/) oder [VSC](https://code.visualstudio.com/).
* Die Dateien sind im  Dateien YAML-Format, also mit der Endung ``.yml``. Eine Besonderheit dieses Formats ist, das es großen Wert auf die *Einrückung*, also die Anzahl an Leerzeichen vor einer Zeile, legt. Bereits erwähnte Texteditoren sollten automatisch darauf achten das diese stimmt.
* Die Dateien auf dem Server werden mithilfe von Git verwaltet. Dazu benötigt ihr einen [Github-Account](https://github.com/). **Sobald ihr einen Account erstellt habt, meldet euch bitte bei Malfrador oder Fyreum, um Zugriff auf unsere Github-Organisation zu erhalten.**
* Sobald ihr Zugriff habt, findet ihr sämtliche Dateien [hier](https://github.com/DRE2N/Erethon).
* Sobald ihr etwas geändert habt - ob Quest, Event, NPC oder Item - müsst ihr eure Änderungen mit dem (Test-)Server synchronisieren. Dies geht mit dem Befehl ``/q sync``.

# Grundlagen
QXL ist ein Baukasten-System. Das bedeutet, das es verschiedene konfigurierbare Bausteine gibt, aus denen sich eine Vielzahl von Quests und Events zusammensetzen lassen. Hier ist ein bisschen Kreativität gefragt.

Auf den ersten Blick wirkt es vielleicht ein wenig komplex, aber lasst euch davon nicht erschrecken.

***Soweit nicht extra erwähnt bezieht sich "Quest" im folgenden auch immer auf Events - beides funktioniert sehr ähnlich.***

### Spielen mit Bausteinen

Die folgenden drei Kategorien von Bausteinen gibt es:
* **Actions:** Actions sind *Dinge, die passieren*. Das kann das Senden einer Nachricht sein, der Start eines Dialogs mit einem NPC oder viele weitere Dinge.
* **Conditions:** Conditions sind *Bedingungen*. Bedingungen können entweder erfüllt oder nicht erfüllt sein. Eine Bedingung könnte zum Beispiel sein, das ein Spieler ein bestimmtes Level erreicht haben muss, sich an einer bestimmten Stelle aufhalten etc.
* **Objectives:** Objectives sind *Ziele*, die erreicht werden können. Ein Ziel ist zum Beispiel das Töten einer bestimmten Anzahl von Monstern, das Erreichen eines Punktes, die Herstellung eines Items und so weiter.

### Bausteine stapeln
Eine Quest besteht immer aus mehreren sogenannten **Stages**. Eine *Stage* ist dabei ein *Abschnitt* der Quest. Beispiel:

1. Sprich mit Walter
2. Streichele fünf Kühe
3. Sprich mit Wolfgang über die Weichheit des Fells der Kühe

Jeder dieser drei Schritte hier wäre eine eigene Stage. *Objectives* gehören immer zu einer Stage. Sobald alle Objectives einer Stage abgeschlossen sind, wechselt die Quest automatisch zur nächsten Stage. Wenn es keine weiteren Stages mehr gibt, ist die Quest erfolgreich beendet und der Spieler erhält seine Belohnung (Kuhfelle?).

Bisher klingt es ja noch alles recht einfach und ein wenig nach dem alten Questplugin, oder? Aber jetzt kommt der Clue: Sämtliche Bausteine lassen sich miteinander kombinieren. Jedes Objective (Ziel) kann *selber* noch Actions ausführen. Jede Condition (Bedingung) kann Actions ausführen, wenn sie erfüllt oder nicht erfüllt wird. Und sogar Actions selbst können andere Actions ausführen. Und sowohl Actions als auch Objectives können eigene Conditions haben.

Klingt kompliziert? Keine Sorge. Nehmen wir zum Beispiel wieder unsere Kuhfell-Quest: Wir möchten jetzt den Spieler richtig ärgern, und jedes Mal, wenn er eine der Kühe streichelt, zwei Zombies spawnen. Nichts einfacher als das! Wir fügen einfach dem *Objective* für das Streicheln der Kühe eine *Action* hinzu, die zwei Zombies spawnt. 

Dann veröffentlichen wir die Quest, und stellen fest, dass sie suuuuper beliebt bei neuen Spielern ist, diese allerdings aber immer an den Zombies sterben. Ideal wäre es jetzt natürlich, wenn wir keine Zombies spawnen, wenn der Spieler unter Level 5 ist. Dafür gibt es *Conditions*: Wir fügen einfach unserer "Spawne Zombies"-Action eine *Condition* hinzu, die überprüft ob der Spieler schon über Level 5 ist - und wenn nicht, wird die Action nicht ausgeführt und es spawnen keine Zombies - aber die restliche Quest läuft wie gewohnt.