---
title: Objectives
sidebar_position: 5
---

## block_interact
This objective is completed when a player interacts with a block at the specified location. Can be cancelled.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `location` | The location of the block that the player must interact with. QLocation |  | true |

```yaml
block_interact: x=64; y=64; z=64; world=world
```

```yaml
block_interact:
  x: 64
  y: 64
  z: 64
  world: world
```

## breed
This objective is completed when the player breeds two entities.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `entityType` | The entity type that has to be bred |  | true |

```yaml
breed: entity_type=pig
```

```yaml
breed:
  entity_type: cow
```

## chat
This objective is completed when the player sends a message.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `exactMatch` | Whether the message has to be an exact match. | false | false |
| `message` | The message that the player has to send. |  | false |

```yaml
chat:
```

```yaml
chat:
  message: 'Hello!'
  exactMatch: true
```

## consume_item
An item needs to be consumed to complete this objective. Hephaestus item keys are used. Can be cancelled.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `item` | The key of the item that needs to be consumed. Same as in /give |  | true |

```yaml
consume_item: item=minecraft:apple
```

```yaml
consume_item:
  item: 'minecraft:apple' # Needs to be quoted due to the colon.
```

## craft
An item needs to be crafted to complete this objective. Supports both vanilla crafting as well as JobsXL crafting. If both are set, both can be used to complete the objective.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `item` | The key of the item that needs to be crafted. Hephaestus. Same as in /give |  | false |
| `jxl_item` | The id of the job item that needs to be crafted. |  | false |

```yaml
craft: jxl_item=fancy_sword
```

```yaml
craft:
  item: 'erethon:fancy_sword' # Needs to be quoted due to the colon.
```

## death
This objective is completed when the player dies.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|

```yaml
death:
```

```yaml
death:
```

## drop_item
Drop an item to complete this objective. Can be cancelled.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `item` | The key of the item that needs to be dropped. Same as in /give |  | true |

```yaml
drop_item: item=erethon:fancy_sword
```

```yaml
drop_item:
  item: 'erethon:fancy_sword' # Needs to be quoted due to the colon.
```

## enter_region
This objective is completed when a player enters a specific region. QuestsXL regions are used, not Factions. `/q region`

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `region` | The ID of the region that the player must enter. |  | true |

```yaml
enter_region: region=region_id
```

```yaml
enter_region:
  region: region_id
```

## impossible
This objective is impossible to complete. It can be used to block progression, for example to only manually progress a stage with the set_stage action

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|

```yaml
impossible:
```

```yaml
impossible:
```

## instant
The opposite of the impossible objective. This objective is instantly completed when the stage starts.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|

```yaml
instant:
```

```yaml
instant:
```

## job_exp
The player has to gain a certain amount of job experience.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `amount` | The amount of experience that needs to be gained. | 1 | false |
| `reason` | The reason for the experience gain. One of `command`, `crafting`, `dungeon`, `item`, `mob`, `quest`, `unknown` | `crafting` | false |

```yaml
job_exp: amount=100
```

```yaml
job_exp:
  amount: 100
  reason: mob
```

## jump
This objective is completed when the player jumps.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|

```yaml
jump:
```

```yaml
jump:
```

## kill_mob
This objective is completed when a player kills a specific mob, optionally within a certain radius around an event.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `mob` | The ID of the mob |  | true |
| `radius` | The radius around the event in which the mob has to be killed. Useful for events | -1 | false |

```yaml
kill_mob: mob=evil_mob
```

```yaml
kill_mob:
  mob: 'evil_mob'
  radius: 10
```

## leave_region
This objective is completed when a player leaves a specific region. QuestsXL regions are used. `/q region`

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `region` | The ID of the region that the player must leave. |  | true |

```yaml
leave_region: region=region_id
```

```yaml
leave_region:
  region: 'region_id'
```

## location
This objective is completed when a player reaches a specific location. Can be cancelled, preventing the player from moving closer

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `location` | The location that the player must reach. QLocation |  | true |
| `range` | How close the player has to get to the location | 1 | false |

```yaml
location: x=64; y=64; z=64; range=5
```

```yaml
location: # This is completed when the player moves five blocks up from their current location.
  x: ~0
  y: ~5
  z: ~0
```

## login
This objective is completed when the player logs in.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|

```yaml
login:
```

```yaml
login:
```

## pickup_item
This objective is completed when the player picks up a specific item. Can be cancelled, preventing the item from being picked up.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `item` | The key of the item that needs to be picked up. Same as in /give |  | true |

```yaml
pickup_item: item=erethon:fancy_sword
```

```yaml
pickup_item:
  item: 'erethon:fancy_sword' # Needs to be quoted due to the colon.
  cancel: true
```

## server_command
This objective is completed when a command is executed on the server, for example by a command block

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `identifier` | The command that has to be executed |  | true |

```yaml
server_command: identifier=q trigger 33333
```

```yaml
server_command:
  identifier: q trigger 3333
```

## sign_edit
This objective is completed when the player start editing a sign. Can be cancelled.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|

```yaml
sign_edit:
```

```yaml
sign_edit:
```

## sleep
This objective is completed when the player sleeps. Can be cancelled, preventing the player from entering the bed.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|

```yaml
sleep:
```

```yaml
sleep:
```

## sneak
This objective is completed when the player starts sneaking.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|

```yaml
sneak:
```

```yaml
sneak:
```

## wait
This objective is completed after a certain amount of time has passed, or after a player has been in a certain location for a certain amount of time.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `duration` | The time in seconds that the objective holder has to wait. |  | true |
| `location` | The location that the objective holder must be in to complete the objective. |  | false |
| `range` | How close the objective holder has to get to the location | 1 | false |

```yaml
wait: duration=100
```

```yaml
wait:
  duration: 10
  x: 100
  y: 100
  z: 100
  range: 5
```

