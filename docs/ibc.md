Eine **Instanced Block Collection** (**IBC**) ist eine Sammlung von Blöcken und Blockzuständen, die nur für einen bestimmten Spieler sichtbar ist. Mit IBCs lassen sich Blockänderungen für einen Spieler anzeigen, ohne den wirklichen Zustand der Welt zu ändern. Für den Spieler, für den eine IBC angezeigt wird, ist diese jedoch vollständige Realität - er kollidiert mit den Blöcken, kann auf ihnen Laufen, etc.

![](https://puu.sh/HF2OG/1a3a0f9bed.png)

IBCs sind ein sehr mächtiges Werkzeug, jedoch vielleicht nicht ganz einfach zu verstehen. Neben simplen Dingen wie Türen und Tore, die sich nur für einen bestimmten Spieler öffnen lassen sich viele weitere Dinge damit realisieren: Jump And Runs mit beweglichen Plattformen, Zerstörungen im Rahmen einer Belagerungs-Quests und viel mehr.

### Zustände
Jede IBC besitzt drei Zustände, von denen sich zwei speichern lassen. Der dritte Zustand ist die Realität der Welt auf dem Server.
* **Shown:** Dieser Zustand ist für den Spieler sichtbar, nachdem die [Show IBC](https://github.com/DRE2N/QuestsXL/wiki/Actions#show-ibc)-Action genutzt wurde.
* **Hidden:** Dieser Zustand ist für den Spieler sichtbar, nachdem die [Hide IBC](https://github.com/DRE2N/QuestsXL/wiki/Actions#hide-ibc)-Action genutzt wurde.
* **Reset:** Dies ist der reale Zustand der Welt und gleichzeitig auch der Zustand, den alle anderen Spieler sehen. Dieser kann auch mit der [Reset IBC](https://github.com/DRE2N/QuestsXL/wiki/Actions#reset-ibc)-Action erzwungen werden.

# Erstellung
IBCs werden inGame erstellt. IBCs haben eine ID und können von mehreren Quests genutzt werden. Sollten mehrere IBCs auf einmal angezeigt werden, wird der Spieler immer die IBC sehen, die er zuletzt gesendet bekommen hat. 

Die Erstellung erfolgt folgendermaßen:
1. Baue den ersten Zustand (z.B. Tor zu)
2. Gebe **/q ibc create <Name>** ein. Mit dem Namen wird die IBC später bei der Questerstellung aufgerufen.
3. Setze mit **/q ibc pos1** und **/q ibc pos2** die beiden Ecken der IBC. (wie WorldEdit)
4. Speichere den ersten Zustand mit **/q ibc saveShown**
5. Baue den zweiten Zustand (z.B. Tor offen)
6. Speichere den zweiten Zustand mit **/q ibc saveHidden**
7. Speichere die gesamte IBC mit **/q ibc save**

Bis zum eingeben von /q ibc save können noch alle Änderungen vorgenommen werden - die Ecken versetzt, die Zustände überschrieben, etc.

**Versuche, die Markierung möglichst klein zu halten!** _Alle_ Blöcke innerhalb der IBC werden an den Spieler gesendet bzw. gespeichert - auch Luft. Große Blockmengen führen dazu, das sie nacheinander gesendet werden, was eventuell den Effekt zerstört. Am besten ist es _nur_ den Teil zu markieren der sich auch wirklich ändert - z.B. nur die Torflügel, nicht das gesamte Torhaus. 
### Weitere Befehle:
* **/q ibc show <Name>** - Zeigt den "Shown"-Zustand einer IBC an.
* **/q ibc hide <Name>** - Zeigt den "Hidden"-Zustand einer IBC an.
* **/q ibc reset <Name>** - Zeigt den "Reset"-Zustand einer IBC an.

Solltest du eine IBC löschen wollen wende dich bitte an einen Admin. 
