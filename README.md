# Piston Tweaks
Piston Tweaks is a Minecraft Paper 1.26.2 plugin that allows you to tweak the default behaviour of pistons and sticky pistons.

## Config Options
| Options | Description | Accepted Values |
| ------- | ----------- | --------------- |
| sticky-piston-push-strength | Number of attached blocks sticky pistons can pull. | int |
| sticky-piston-pull-strength | Number of attached blocks sticky pistons can push. | int |
| piston-push-strength | Number of attached blocks normal pistons can push. | int |
| piston-pull-strength | Number of attached blocks normal pistons can pull (requires all-pistons-are-sticky: true). | int |
| all-pistons-are-sticky | Whether normal pistons should be able to pull blocks like sticky pistons. | boolean |
| log-debug | Whether debug logs should be sent to console (disabled by default for spam saving). | boolean |
| sticky-blocks | List of blocks that should be treated as sticky (like slime blocks). | list<string> |
| unmoveable-blocks | List of blocks that cannot be moved by pistons (if you remove pistons from this list movement will break). | list<string> |

## Permissions
| Permission | Description |
| ---------- | ----------- |
| pistontweaks.reload | Grant permission to the /piston-tweaks:reload command. |

## Commands
| Command | Description |
| ------- | ----------- |
| /piston-tweaks:reload | Reloads configuration from config file. |
