# Actions

## delay
Delays the execution of a list of actions by a certain amount of time.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `actions` | The list of actions to execute after the delay |  | true |
| `duration` | The duration in seconds | 0 | true |

```yaml
<no short syntax>
```

```yaml
delay:
  duration: 10
  actions:
    - 'message: message=Hello world'
```

## display_marker
**Currently not implemented**

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|

```yaml

```

```yaml

```

## dummy
This action does nothing. It is used for testing and debugging.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|

```yaml
dummy:
```

```yaml

```

## event_participation
Add participation to an event. Always executed as a player

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `amount` | The amount of participation to add | 1 | false |
| `id` | The ID of the event to participate in |  | true |

```yaml
event_participation: id=example_event; amount=1
```

```yaml
event_participation:
  id: example_event
  amount: 1
```

## give_item
Give an item to the player. The item needs to be defined in the Hephaestus item library. <br>You can check if an item exists by using `/give` in Minecraft.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `amount` | The amount of items to give | 1 | false |
| `chance` | The chance of the item being given. 100 is 100% chance, 0 is 0% chance | 100 | false |
| `item` | The ID of the item to give |  | true |

```yaml
give_item: item=minecraft:stone; amount=420; chance=69
```

```yaml
give_item:
  item: 'minecraft:stone' # The ID needs to be quoted due to the colon
  amount: 420
  chance: 69
```

## hide_ibc
Hides an InstancedBlockCollection from a player.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `id` | The ID of the IBC to hide |  | true |

```yaml
hide_ibc: id=example_collection
```

```yaml
hide_ibc:
  id: example_collection
```

## job_exp
Gives the player job experience.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `chance` | The chance of the action being executed. 1 is 100% chance, 0 is 0% chance | 1 | false |
| `max` | The maximum amount of experience to give |  | true |
| `min` | The minimum amount of experience to give |  | true |

```yaml
job_exp: min=1; max=10; chance=0.5
```

```yaml
job_exp:
  min: 1
  max: 10
  chance: 0.5
```

## message
Sends a message to the player or all event participants.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `message` | The message to send |  | true |

```yaml
message: message=Hello, world!
```

```yaml
message:
  message: Hello, world!
```

## objective_display
Sets the display text of an objective for a player. Shown in the sidebar (right side of the screen).

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `id` | The ID of the quest or event to set the objective for |  | true |
| `text` | The text to display, empty to clear |   | false |

```yaml
objective_display: id=example_quest; text=Hello!
```

```yaml
objective_display:
  id: example_quest
  text: Hello again!
```

## paste_schematic
Pastes a schematic at a location and undoes it after a certain amount of time, optionally.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `location` | The location to paste the schematic at. QLocation |  | true |
| `schematic` | The name of the schematic file in the FAWE schematics folder |  | true |
| `time` | The time in ticks after which the schematic will be undone | 60 | false |

```yaml
paste_schematic: x=~5; y=~0; z=0; schematic=example.schematic; time=60
```

```yaml
paste_schematic:
  location:
    x: 123
    y: 64
    z: 321
  schematic: example.schematic
  time: 60
```

## permission
Adds or removes a permission or group from a player.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `action` | The action to perform. Can be `add`, `remove`, `add_group` or `remove_group` | `add` | false |
| `permission` | The permission or group to add or remove |  | true |

```yaml
permission: permission=example_permission; action=ADD
```

```yaml
permission:
  permission: example_group # Yes, its called permission, but it can also be a group
  action: add_group
```

## play_animation
**Currently not implemented**

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|

```yaml

```

```yaml

```

## play_cutscene
Plays a cutscene. The cutscene needs to be created first.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `cutscene` | The ID of the cutscene to play |  | true |

```yaml
play_cutscene: cutscene=example_cutscene
```

```yaml
play_cutscene:
  cutscene: example_cutscene
```

## play_dialogue
Starts playing a dialogue. The dialogue needs to be defined in a separate file in. 
If the dialogue is not found, the action will fail on load.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `dialogue` | The ID of the dialogue to play |  | true |

```yaml
play_dialogue: dialogue=example_dialogue
```

```yaml
dialogue:
  id: example_dialogue
```

## remove_mob
Removes one or multiple mobs from the world.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `doDamage` | Whether the mob should take damage and die, or just be removed | false | false |
| `mob` | The ID of the mob to remove |  | true |
| `radius` | The radius in which to remove the mob | 32 | false |

```yaml
remove_mob: mob=example_mob; radius=32
```

```yaml
remove_mob:
  mob: example_mob
  radius: 32
  doDamage: true
```

## repeat
Repeats a set of actions a specified amount of times with a delay between each repetition.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `actions` | The list of actions to repeat |  | true |
| `delay` | The delay between each repetition in seconds | 0 | false |
| `repetitions` | The amount of repetitions | 1 | false |

```yaml
<no short syntax>
```

```yaml
repeat:
  delay: 10
  repetitions: 5
  actions:
    - 'message: message=Hello world'
```

## reset_ibc
Resets an InstancedBlockCollection for a player (restores real, shared world state).

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `ibc` | The ID of the IBC to reset |  | true |

```yaml
reset_ibc: ibc=example_collection
```

```yaml
reset_ibc:
  ibc: example_collection
```

## run_as
Runs a list of actions as a player. Useful for running actions on all players in an event. Optionally filters players by range or participation count.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `actions` | The list of actions to execute |  | true |
| `mode` | The mode in which the action should be run. One of `event_in_range`, `event_participants` or `online` | `online` | false |
| `value` | The value to filter players by. For `event_participants`, this is the min. participation. For `event_in_range`, this is the range in blocks *added* to the event range. | 0 | false |

```yaml
<no short example>
```

```yaml
run_as:
  mode: event_in_range
  value: 5
  actions:
    - 'message: message=Yeet'
```

## run_command
Runs a command, optionally with full permissions (op) or as console. By default, the command is run as the player.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `command` | The command to run, without the / |  | true |
| `console` | Whether to run the command as console | false | false |
| `op` | Whether to run the command as an op | false | false |

```yaml
run_command: command=stop; console=true # Shut down the server
```

```yaml
run_command:
  command: example_command
  op: false
  console: false
```

## score
Modifies a score value. The score can be global, player-specific or event-specific. 
Scores are a powerful tool to track player progress and can be used in conditions and actions.
For example, you could add 1 to a score called enemy_threat every time a player kills a mob during an event, and if the score reaches a certain value, you could spawn a boss.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `operation` | The operation to perform. One of `add`, `remove`, `set` or `reset` | `add` | false |
| `scope` | The scope of the score. One of `global`, `player` or `event` | `player` | false |
| `score` | The score to modify |  | true |
| `value` | The value to add, remove, set or reset | 1 | false |

```yaml
score: score=enemy_threat; value=1; operation=add scope=event
```

```yaml
score:
  score: enemy_threat
  value: 1
  operation: add
  scope: event
```

## show_ibc
Shows an Instanced Block Collection to the player.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `ibc` | The ID of the collection to show |  | true |

```yaml
show_ibc: ibc=example_collection
```

```yaml
show_ibc:
  ibc: example_collection
```

## spawn_mob
Spawns a mob at a location.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `location` | The location to spawn the mob at. QLocation |  | true |
| `mob` | The ID of the mob to spawn |  | true |

```yaml
spawn_mob: mob=example_mob; x=5; y=0; z=0
```

```yaml
spawn_mob:
  mob: example_mob
  location: 
    x: ~40
    y: ~0
    z: ~5
```

## spawner
Currently only triggers a spawner. Spawners can be set-up in Aether. 
This will be expanded in the future.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `spawner` | The ID of the spawner to trigger |  | true |

```yaml
spawner: spawner=example_spawner
```

```yaml
spawner:
  spawner: example_spawner
```

## stage
Changes the current stage of a quest or event. This is a powerful actin that can be used for branching quests or events. 
For example, you could create a dialogue that gives the player a choice, and depending on the choice, you could set a different stage.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `id` | The quest or event to change the stage of |  | true |
| `stage` | The stage to set |  | true |

```yaml
stage: id=example_quest; stage=1
```

```yaml
stage:
  id: example_event
  stage: 99
```

## start_event
Starts an event.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `event` | The ID of the event to start |  | true |
| `skipConditions` | Whether to skip the event's conditions | false | false |

```yaml
start_event: event=example_event
```

```yaml
start_event:
  event: example_event
  skipConditions: true
```

## start_quest
Starts a quest for the player. If the player already has the quest, the action will be skipped.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `quest` | The ID of the quest to start |  | true |

```yaml
start_quest: quest=example_quest
```

```yaml
start_quest:
  quest: example_quest
```

## teleport
Teleports the player to a location

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `target` | The location to teleport the player to |  | true |

```yaml
teleport: x=~0; y=~1; z=~0 # Teleports the player one block up
```

```yaml
teleport:
  target:
    x: 0
    y: 64
    z: 0
```

## title
Sends a title to the player or all event participants, optionally with specified behaviour.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `fadeIn` | The time in ticks the title takes to fade in | 10 | false |
| `fadeOut` | The time in ticks the title takes to fade out | 10 | false |
| `stay` | The time in ticks the title stays on screen | 20 | false |
| `subtitle` | The subtitle to send |   | false |
| `title` | The title to send |  | true |

```yaml
title: title=Hello, world!
```

```yaml
title:
  title: Hello, world!
  subtitle: This is a subtitle
  fadeIn: 1
  stay: 5
  fadeOut: 1
```

