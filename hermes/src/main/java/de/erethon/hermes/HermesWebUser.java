package de.erethon.hermes;

import java.util.UUID;

public record HermesWebUser(UUID playerId, String playerName, HermesWebRole role) {
}
