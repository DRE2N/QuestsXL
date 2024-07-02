**Events** sind ein wichtiger Bestandteil von QXL und ein guter Weg, Erethon lebendig wirken zu lassen. Events können so gut wie alle Actions, Objectives und Conditions nutzen. Im Gegensatz zu Quests sind Events nicht an einen Spieler (oder eine Gruppe) gebunden. Tatsächlich brauchen sie gar keinen Spieler und können theoretisch ganz von alleine ablaufen und abgeschlossen werden.

Events werden erstellt, in dem eine Datei im Ordner `events` angelegt wird. Der Datei-Name ist dabei - wie bei Quests auch - die ID des Events. Dateien im `events`-Ordner können wie gewohnt in Unterordnern sortiert werden.

## Grundlagen & Beteiligung
Events haben eine Position in der Welt und einen Radius. Spieler, die sich innerhalb des Radius des Events befinden, bekommen das Event angezeigt. Standardmäßig werden alle Actions des Events für alle Spieler innerhalb des Radius ausgeführt.

Durch Teilnehmen am Event sammeln Spieler Teilnahmepunkte. Diese können mithilfe der ``event_participation``-Action einem Spieler gegeben werden:
`event_participation: <event ID>;<Wert>`

In vielen Fällen ist es wahrscheinlich sinnvoller, Actions nur für Spieler auszuführen, die aktiv am Event teilnehmen. Dies kann mithilfe der ``run_as``-Action getan werden:
```yaml
sendMsgToGreatPlayers:
  type: run_as
  mode: event_participants
  value: 100 # Minimum-Participation-Wert
  actions:
    message: "<green>Du machst das toll!"
```
Events sind - genauso wie Quests - abgeschlossen, sobald sämtliche Objectives in der letzten Stage erfüllt sind. Genau wie bei Quests kann die `stage`-Action genutzt werden, um die aktuelle Stage zu ändern. 

Event-Belohnungen können über die ``rewards``-Sektion der Eventdatei vergeben werden, wobei sich diese auch nach der erreichten Beteiligung eines Spielers richten. Die Reward-Actions werden für alle Spieler ausgeführt, die sich je am Event beteiligt haben - sie müssen nicht in der Nähe sein. 

Es ist möglich, mit der ``start_event``-Action ein Event zu starten. Damit lassen sich Event-Ketten erstellen, in dem man diese einfach bei Abschluss eines Events aufruft. Genauso ist es so möglich, das das Abschließen einer Quest ein Event triggered. 

Mithilfe der ``stage``-Action ist es auch möglich, die Stage eines *anderen* Events zu ändern. So lassen sich z.B. große Meta-Events erstellen, die fortschreiten, wenn mehrere kleine Sub-Events abgeschlossen werden.

# Beispiel-Datei

Im Folgen eine sehr einfache Beispieldatei für ein Event:
```yaml
startLocation: # Position des Events in der Welt
  world: Erethon
  x: 1337
  y: 69
  z: 420
cooldown: 600 # Cooldown in Sekunden, bis das Event wieder spawnen kann. 
range: 32 # Größe des Events. Siehe oben bei der Begriffserklärung.
startConditions: # Diese Conditions müssen erfüllt sein, um das Event zu starten.
state: # Diese Sektion dient zum Speichern des Event-Status und sollte nicht geändert werden.
  state: NOT_STARTED
rewards:
  0: # Benötigte Event-Participation, um diese Actions auszuführen.
    # Actions
onUpdate:
 # Actions hier werden bei jedem Update (standardmäßig 5 Sekunden) des Events ausgeführt.
stages:
 # Wie Quests
```