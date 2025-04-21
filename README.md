# BossCore

BossCore is a Minecraft Spigot plugin that creates an interactive boss event where players compete to strike the boss the most times. The player with the most hits wins prizes!

## Features

- Iron Golem boss that players can attack
- Configurable countdown before event starts
- Real-time scoreboard tracking hits and rankings
- Automated reward distribution to top 3 players
- Fully customizable messages and event parameters
- Permission-based admin controls

## Commands

- `/bosscore start` - Start the boss event countdown
- `/bosscore stop` - Stop the event or cancel the countdown
- `/bosscore reload` - Reload the plugin configuration
- `/bosscore setlocation` - Set the boss spawn location to your current position
- `/bosscore credits` - Show plugin credits

## Permissions

- `bosscore.admin` - Allows access to all BossCore admin commands

## Configuration

The plugin is highly configurable through the `config.yml` file:

- Event parameters (countdown timer, boss health, etc.)
- Scoreboard appearance and content
- Reward commands for winners
- Custom messages for all plugin actions

## Installation

1. Place the `BossCore.jar` file in your server's `plugins` folder
2. Restart your server
3. Configure the plugin by editing `plugins/BossCore/config.yml`
4. Set the boss spawn location using `/bosscore setlocation`

## How It Works

1. Admins start the event using `/bosscore start`
2. A countdown begins with a visual scoreboard for all players
3. The Iron Golem boss spawns at the configured location
4. Players compete to hit the boss the most times
5. The event ends when the boss's health reaches zero
6. Top 3 players receive configured rewards automatically
7. Results are displayed in chat for all players

## Technical Requirements

- Spigot/Bukkit server 1.8.8 or later
- Java 8 or later

## License

This plugin is licensed under the [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.html).

## Author

Created by Fl1uxxNoob
