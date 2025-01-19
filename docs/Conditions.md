# Conditions

## active_quest
Checks if the player has the specified quest active.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `quest` | The ID of the quest. |  | true |

```yaml
active_quest: quest=example_quest
```

```yaml
active_quest:
  quest: example_quest
```

## completed_quest
Checks if the player has the specified quest completed.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `quest` | The name of the quest that the player must have completed. |  | true |

```yaml
completed_quest: quest=example_quest
```

```yaml
completed_quest:
  quest: example_quest
```

## event_state
Checks if the specified event is in the specified state.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `event` | The ID of the event. |  | false |
| `state` | The state the event should be in. One of `active`, `inactive`, `completed` or `disabled` | active | false |

```yaml
event_state: event=example_event; state=active
```

```yaml
event_state:
  event: example_event
  state: active
```

## global_score
Checks if a certain global score is larger or equal to a value. Global scores are server-wide.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `score` | The name of the global score. |  | true |
| `value` | The value the score should be larger or equal to. | 1 | false |

```yaml
global_score: score=example_score; value=5
```

```yaml
global_score:
  score: example_score
  value: 5
```

## group_size
Checks if the player's current group is between min and max group size. Requires Aergia to be installed.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `max` | The maximum group size. | 5 | false |
| `min` | The minimum group size. | 1 | false |

```yaml
group_size: min=2; max=5
```

```yaml
group_size:
  min: 2
  max: 5
```

## inventory_contains
Checks if the player has the specified item in their inventory. Supports both Materials and Hephaestus items.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `amount` | The amount of the item. |  | false |
| `item` | The Hephaestus item to check for. |  | false |
| `material` | The material of the item. |  | false |

```yaml
inventory_contains: material=diamond_ore; amount=5
```

```yaml
inventory_contains:
  item: 'erethon:hoe' # Needs to be quoted due to the colon
  amount: 1
```

## inverted
This condition is successful if all of its conditions are not successful.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `conditions` | A list of conditions that must not be successful for this condition to be successful. |  | true |

```yaml
<no short syntax>
```

```yaml
inverted:
  conditions:
  - event_state: id=example; state=disabled
```

## job_level
Checks if a player has a certain level in a job. Requires JobsXL to be installed.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `job` | The name of the job that the player must have a certain level in. |  | true |
| `level` | The level that the player must have in the specified job. |  | true |

```yaml
job_level: job=Miner; level=5
```

```yaml
job_level:
  job: Miner
  level: 5
```

## location
Checks if the player is within a certain range of a location.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `location` | The location the player has to be in range of. QLocation |  | true |
| `range` | The radius the player has to be in. | 1 | false |

```yaml
location: x=192; y=64; y=20; radius=10
```

```yaml
location:
  x: 192
  y: 64
  z: 20
  radius: 10
```

## looking_at
Checks if the player is looking at a certain location or block.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `accuracy` | The accuracy the player has to be looking at the location. | 1 | false |
| `block` | The block the player has to be looking at. Can be used together with location |  | false |
| `location` | The location the player has to be looking at. QLocation |  | false |

```yaml
looking_at: x=192; y=64; y=20; accuracy=1
```

```yaml
looking_at:
  block: DIAMOND_BLOCK
```

## permission
Checks if the player has a certain permission.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `permission` | The permission the player has to have. |  | true |

```yaml
permission: permission=questsxl.test
```

```yaml
permission:
  permission: questsxl.test
```

## player_score
Checks if a player has at least a certain score. Per-player

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `score` | The name of the score. |  | true |
| `value` | The value the score should be larger or equal to. | 1 | false |

```yaml
player_score: score=score_name; value=5
```

```yaml
player_score:
  score: score_name
  value: 5
```

## players_in_range
Checks if the specified event or player has a certain amount of players in range.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `event` | The ID of the event. |  | false |
| `max_players` | The maximum amount of players required. | 0 | true |
| `min_players` | The minimum amount of players required. | 0 | true |
| `mode` | The mode to check for. Can be `in_range_even`, `in_range_player` or `participating` | `in_range_event` | false |
| `range` | The range to check for players when mode `in_range_player` is used. | 32 | false |

```yaml
players_in_range: event=example_event; min_players=1; max_players=5; mode=in_range_event
```

```yaml
players_in_range:
  min_players: 1
  max_players: 5
  mode: in_range_player
  range: 16
```

## region
Checks if the player is in a certain region. QXL-Region, not Faction-Region!

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `region` | The ID of the region. |  | true |

```yaml
region: region=region_id
```

```yaml
region:
  region: region_id
```

## time
Checks if the current time is between min and max time.

#### Parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `maxHour` | The maximum hour. |  | false |
| `maxMinute` | The maximum minute. |  | false |
| `minHour` | The minimum hour. |  | false |
| `minMinute` | The minimum minute. |  | false |

```yaml
time: minHour=18; minMinute=0; maxHour=20; maxMinute=0 #18:00 - 20:00
```

```yaml
time:
  minHour: 18
  minMinute: 0
  maxHour: 20
  maxMinute: 0
  timeZone: ECT
```

