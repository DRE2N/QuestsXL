**Actions** können, wie alle anderen QXL-Funktionen, in einer Lang- und Kurzschreibweise geschrieben werden. Die Kurzschreibweise nutzt dabei nur eine Zeile.
Für einige komplexe Actions steht die Kurzschreibweise nicht zur Verfügung.

In dieser Dokumentation wird die Kurzschreibweise zu erst genannt und dann die Langschreibweise.

## Animation
Spielt eine Animation ab.

`animation: <name>;<instanced>`
```
key:
  type: animation
  name: <name>
  instanced: <true/false | true>
```
* **name:** Der Name einer vorher erstellten Animation
* **instanced:** true/false - Ob die Animation nur für den Spieler (oder seine Gruppe) sichtbar ist (true) oder für alle (false). Default = true.
* **runAfter:** Nachdem die Animation vollständig abgespielt wurde.
## Command
Führt einen Befehl aus. 

`command: <command>;<op>;<console>`
```
key:
  type: command
  command: <command>
  op: <true/false | false>
  console: <true/false | false>
```
* **command:** Der Befehl. Der Platzhalter `%player%` kann benutzt werden & wird durch den Namen des Spielers ersetzt.
* **op:** true/false -  Ob der Befehl als OP (mit *-Permissions) ausgeführt wird oder nicht. Sollte vorsichtig genutzt werden. Default = false.
* **console:** true/false - Ob der Befehl vom Server ausgeführt werden soll. Default = false.
## Cutscene
Spielt eine vorher erstellte Cutscene ab.

`cutscene: <name>;<teleportBack>`
```
key:
  type: cutscene
  name: <name>
  teleportBack: <true/false | false>
```
* **name:** Der Name einer vorher ingame erstellten Cutscene
* **instanced:** true/false - Ob der Spieler nach Ende der Cutscene wieder an den Start zurückteleportiert wird (true), oder da bleibt wo die Cutscene endet (false). Default = false.
* **runAfter:** Nachdem die Cutscene fertig abgespielt wurde.
## Delay
Führt eine oder mehrere Actions verzögert aus. Keine Kurzschreibweise möglich. 
```
key:
  type: delay
  delay: <delay>
  actions:
    action1:
    action2:
```
* **delay:** Die Verzögerung, gemessen in Ticks. Eine Sekunde -> 20 Ticks. 
* **actions:** Eine Aufzählung der Actions, die nach dem Delay ausgeführt werden. Kurz- und Langform sind möglich.
* **runAfter:** Nach Ausführung der verzögerten Action.
## Give Item
Vergibt ein oder mehrere Items an den Spieler.

`give_item: <item>;<amount>;<chance>`
```
key:
  type: give_item
  item: <item>
  amount: <amount | 1>
  chance: <chance | 100>
```
* **item:** Das Item. Kann ein Vanilla-Item-Name sein oder ein Custom-Item. 
* **amount:** Die Menge des Items. Default = 1.
* **chance:** Ein Wert von 0 bis 100, der die Chance angibt mit der dieses Item erhalten werden kann. Default = 100.
## Hide IBC
## Message
Sendet eine Nachricht an den Spieler. 

`message: <message>`
```
key:
  type: message
  message: <message>
```
* **message:** Die Nachricht.
## Mob Follow
Lässt einen Mob/NPC einem Spieler folgen.

`mob_follow_player: <mob-id>;<instanced>;<distance>;<stopOnTeleport>`
```
key:
  type: mob_follow_player
  id: <mob-id>
  instanced: <true/false | true>
  distance: <distance>
  stopOnTeleport: <true/false | true>
```
* **id:** Die ID des Mobs. 
* **instanced:** true/false - Ob ein neuer InstancedMob erstellt werden soll, der nur für den Spieler sichtbar ist (true) oder ob der echte Mob, für alle sichtbar, dem Spieler folgen soll (false). Default = true.
* **distance:** Die Entfernung, die der Mob versucht zum Spieler einzuhalten. Werte, die oberhalb der Server-Sichtweite liegen, funktionieren wahrscheinlich nicht.
* **stopOnTeleport:** true/false - Ob der Mob sich mit dem Spieler teleportieren soll (false) oder das Folgen abbricht, sobald der Spieler sich teleportiert (true). Default = true.
## Permission
Verändert die Permission oder Gruppen eines Spielers.

`permission: <permission>;<action>`
```
key:
  type: permission
  permission: <permission>
  action: <action>
```
* **permission:** Die Permission oder der Gruppen-Name.
* **action:** `add`, `remove`, `add_group`, `remove_group` - Was passieren soll.
## Reset IBC
## Show IBC 

## Spawn Mob
Spawnt einen Mob oder NPC.

`spawn_mob: <mob-id>;<x>;<y>;<z>;<relative>;<instanced>;<ignoreConditions>;<adjust>`
```
key:
  type: spawn_mob
  id: <mob-id>
  x: <x>
  y: <y>
  z: <z>
  relative: <true/false | false>
  instanced: <true/false | false>
  ignoreConditions: <true/false | false>
  adjust: <true/false | false>
```
* **id:** Die ID des Mobs/NPCs.
* **x/y/z:** Koordinaten
* **relative:** Ob die angegebenen Koordinaten relativ zur aktuellen Position des Spielers sind (true) oder normale Koordinaten (false). Default = false
* **instanced:** Ob es sich um einen InstancedMob handelt, der nur für den Spieler bzw. seine Gruppe sichtbar ist (true) oder einen normalen Mob, der für alle sichtbar ist (false). Default = false.
* **ignoreConditions:** Ob der Mob mögliche Spawn-Bedingungen ignorieren soll. Default = false
* **adjust:** Nur sinnvoll, wenn `relative: true`. Ob versucht werden soll den Mob an einer anderen Stelle zu spawnen, sollte das Spawnen an den angegebenen Koordinaten nicht möglich sein (z.B. weil in einem Block).
## Stage change
Ändert die Stage des Spielers.

`stage: <stage-id>;<complete>`
```
key:
  type: stage
  id: <stage-id>
  complete: <true/false | false>
```
* **id:** Die ID der neuen Stage.
* **complete:** Ob die aktuelle Stage als abgeschlossen markiert werden soll (true) oder nicht (false). Führt dazu, das eventuelle `onFinish`-Actions ausgeführt werden. Default = false.
## Teleport
Teleportiert den Spieler. 
## Title
