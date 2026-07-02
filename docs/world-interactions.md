---
title: World  Interactions
sidebar_position: 7
---


# World Interactions

World Interactions are location-based objective holders that activate when players are nearby.

## Configuration

World interactions are configured in YAML files in the `interactions/` directory.

### Basic Structure

```yaml
my_interaction_id:
  world: Erethon           # World name
  x: 100.0                 # X coordinate
  y: 64.0                  # Y coordinate
  z: 200.0                 # Z coordinate
  radius: 32.0             # Activation radius (default: 32.0)
  repeatable: true         # Whether the interaction can be completed multiple times (default: true)
  objectives:
    # Objectives configuration
```

Interactions need a location and radius. This is used to dynamically add/remove the objectives belonging to an interaction to nearby players. 
Try to configure the radius as low as reasonably possible for performance reasons. 

### Repeatable Property

The `repeatable` property controls whether an interaction can be completed multiple times by the same character:

- **`repeatable: true`** (default): The interaction can be completed repeatedly. Objectives will reset and can be triggered again.
- **`repeatable: false`**: Once all objectives are completed, the interaction cannot be activated again for that character. This is useful for one-time events or discoveries.

### Examples

#### Repeatable Interaction (Lever that opens a door)

```yaml
crane_lever:
  world: Erethon
  x: 150.5
  y: 70.0
  z: -320.8
  radius: 10.0
  repeatable: true         # Can be used multiple times
  objectives:
    interact:
      location:
	    world: Erethon
		x: 100
		y: 64
		z: 100
      onComplete:
        - set_block: location: x=0; y=64; z=0; material=air
```

#### Non-Repeatable Interaction (One-time discovery)

```yaml
ancient_shrine:
  world: Erethon
  x: 500.0
  y: 80.0
  z: -1000.0
  radius: 20.0
  repeatable: false        # Can only be completed once per character
  objectives:
    location:
      location:
	    world: Erethon
		x: 100
		y: 64
		z: 100
      radius: 5.0
      onComplete:
        - "message: message=<green>You discovered an ancient shrine!"
```