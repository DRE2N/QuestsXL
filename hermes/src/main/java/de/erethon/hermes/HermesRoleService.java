package de.erethon.hermes;

import de.erethon.hecate.Hecate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class HermesRoleService {

    private final Hermes plugin;

    public HermesRoleService(Hermes plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<Void> ensureSchema() {
        CompletableFuture<?> schemaFuture = Hecate.getInstance().getDatabaseManager().queryAsync(handle -> {
            handle.execute("ALTER TABLE Players ADD COLUMN IF NOT EXISTS web_role VARCHAR(32)");
            return null;
        });
        return schemaFuture.handle((ignored, throwable) -> {
            if (throwable == null) {
                return null;
            }
            plugin.getLogger().warning("Failed to ensure Hermes role schema: " + throwable.getMessage());
            throw new IllegalStateException(throwable);
        });
    }

    public CompletableFuture<HermesWebUser> getWebUser(UUID playerId) {
        return Hecate.getInstance().getDatabaseManager().queryAsync(handle ->
                handle.createQuery("""
                                SELECT player_id, COALESCE(last_known_name, '') AS last_known_name, COALESCE(web_role, '') AS web_role
                                FROM Players
                                WHERE player_id = :playerId
                                """)
                        .bind("playerId", playerId)
                        .map((rs, ctx) -> new HermesWebUser(
                                rs.getObject("player_id", UUID.class),
                                rs.getString("last_known_name"),
                                HermesWebRole.fromDatabase(rs.getString("web_role"))
                        ))
                        .findOne()
                        .orElse(new HermesWebUser(playerId, "", HermesWebRole.NONE))
        );
    }

    public CompletableFuture<List<HermesWebUser>> listWebUsers() {
        return Hecate.getInstance().getDatabaseManager().queryAsync(handle ->
                handle.createQuery("""
                                SELECT player_id, COALESCE(last_known_name, '') AS last_known_name, COALESCE(web_role, '') AS web_role
                                FROM Players
                                WHERE web_enabled_at IS NOT NULL OR web_role IS NOT NULL
                                ORDER BY last_known_name ASC
                                """)
                        .map((rs, ctx) -> new HermesWebUser(
                                rs.getObject("player_id", UUID.class),
                                rs.getString("last_known_name"),
                                HermesWebRole.fromDatabase(rs.getString("web_role"))
                        ))
                        .list()
        );
    }

    public CompletableFuture<HermesWebUser> setWebRole(UUID playerId, HermesWebRole role) {
        return Hecate.getInstance().getDatabaseManager().queryAsync(handle -> {
            handle.createUpdate("UPDATE Players SET web_role = :role WHERE player_id = :playerId")
                    .bind("role", role.name())
                    .bind("playerId", playerId)
                    .execute();
            return handle.createQuery("""
                            SELECT player_id, COALESCE(last_known_name, '') AS last_known_name, COALESCE(web_role, '') AS web_role
                            FROM Players
                            WHERE player_id = :playerId
                            """)
                    .bind("playerId", playerId)
                    .map((rs, ctx) -> new HermesWebUser(
                            rs.getObject("player_id", UUID.class),
                            rs.getString("last_known_name"),
                            HermesWebRole.fromDatabase(rs.getString("web_role"))
                    ))
                    .findOne()
                    .orElse(new HermesWebUser(playerId, "", HermesWebRole.NONE));
        });
    }
}
