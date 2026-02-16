package de.erethon.questsxl.instancing;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.time.Instant;
import java.util.UUID;

/**
 * Data Access Object for instance persistence.
 */
public interface InstanceDao {

    // ==================== Template Operations ====================

    @SqlUpdate("""
        INSERT INTO q_instance_templates (template_id, world_name, min_x, min_y, min_z, max_x, max_y, max_z, block_data, block_entities)
        VALUES (:templateId, :worldName, :minX, :minY, :minZ, :maxX, :maxY, :maxZ, :blockData, :blockEntities)
        ON CONFLICT (template_id) DO UPDATE SET
            world_name = :worldName,
            min_x = :minX, min_y = :minY, min_z = :minZ,
            max_x = :maxX, max_y = :maxY, max_z = :maxZ,
            block_data = :blockData,
            block_entities = :blockEntities
    """)
    void upsertTemplate(
            @Bind("templateId") String templateId,
            @Bind("worldName") String worldName,
            @Bind("minX") int minX,
            @Bind("minY") int minY,
            @Bind("minZ") int minZ,
            @Bind("maxX") int maxX,
            @Bind("maxY") int maxY,
            @Bind("maxZ") int maxZ,
            @Bind("blockData") byte[] blockData,
            @Bind("blockEntities") byte[] blockEntities
    );

    @SqlQuery("""
        SELECT world_name, min_x, min_y, min_z, max_x, max_y, max_z, block_data, block_entities
        FROM q_instance_templates
        WHERE template_id = :templateId
    """)
    TemplateData getTemplateData(@Bind("templateId") String templateId);

    @SqlUpdate("DELETE FROM q_instance_templates WHERE template_id = :templateId")
    void deleteTemplate(@Bind("templateId") String templateId);

    @SqlQuery("SELECT template_id FROM q_instance_templates")
    java.util.List<String> getAllTemplateIds();

    // ==================== Instance State Operations ====================

    @SqlUpdate("""
        INSERT INTO q_character_instances (character_id, template_id, modified_blocks, block_entities)
        VALUES (:characterId, :templateId, :modifiedBlocks, :blockEntities)
        ON CONFLICT (character_id, template_id) DO UPDATE SET
            modified_blocks = :modifiedBlocks,
            block_entities = :blockEntities,
            updated_at = CURRENT_TIMESTAMP
    """)
    void upsertInstanceState(
            @Bind("characterId") UUID characterId,
            @Bind("templateId") String templateId,
            @Bind("modifiedBlocks") byte[] modifiedBlocks,
            @Bind("blockEntities") byte[] blockEntities
    );

    @SqlQuery("""
        SELECT modified_blocks, block_entities
        FROM q_character_instances
        WHERE character_id = :characterId AND template_id = :templateId
    """)
    InstanceStateData getInstanceState(
            @Bind("characterId") UUID characterId,
            @Bind("templateId") String templateId
    );

    @SqlUpdate("""
        DELETE FROM q_character_instances
        WHERE character_id = :characterId AND template_id = :templateId
    """)
    void deleteInstanceState(
            @Bind("characterId") UUID characterId,
            @Bind("templateId") String templateId
    );

    // ==================== Template Chunk Operations ====================

    @SqlUpdate("DELETE FROM q_apartment_template_chunks WHERE template_id = :templateId")
    void deleteTemplateChunks(@Bind("templateId") String templateId);

    @SqlUpdate("""
        INSERT INTO q_apartment_template_chunks (template_id, world_name, chunk_x, chunk_z)
        VALUES (:templateId, :worldName, :chunkX, :chunkZ)
        ON CONFLICT (template_id, world_name, chunk_x, chunk_z) DO NOTHING
    """)
    void insertTemplateChunk(
            @Bind("templateId") String templateId,
            @Bind("worldName") String worldName,
            @Bind("chunkX") int chunkX,
            @Bind("chunkZ") int chunkZ
    );

    @SqlQuery("""
        SELECT world_name, chunk_x, chunk_z
        FROM q_apartment_template_chunks
        WHERE template_id = :templateId
    """)
    java.util.List<TemplateChunk> getTemplateChunks(@Bind("templateId") String templateId);

    @SqlQuery("""
        SELECT template_id, world_name, chunk_x, chunk_z
        FROM q_apartment_template_chunks
    """)
    java.util.List<TemplateChunk> getAllTemplateChunks();

    // ==================== Apartment Rentals ====================

    @SqlUpdate("""
        INSERT INTO q_apartment_rentals (character_id, template_id, expires_at)
        VALUES (:characterId, :templateId, :expiresAt)
        ON CONFLICT (character_id, template_id) DO UPDATE SET expires_at = :expiresAt
    """)
    void upsertRental(
            @Bind("characterId") UUID characterId,
            @Bind("templateId") String templateId,
            @Bind("expiresAt") Instant expiresAt
    );

    @SqlQuery("""
        SELECT expires_at
        FROM q_apartment_rentals
        WHERE character_id = :characterId AND template_id = :templateId
    """)
    Instant getRentalExpiry(
            @Bind("characterId") UUID characterId,
            @Bind("templateId") String templateId
    );

    @SqlQuery("""
        SELECT template_id
        FROM q_apartment_rentals
        WHERE character_id = :characterId AND expires_at > :now
    """)
    java.util.List<RentalRow> getActiveRentals(
            @Bind("characterId") UUID characterId,
            @Bind("now") Instant now
    );

    // ==================== Data Classes ====================

    class TemplateData {
        public String worldName;
        public int minX, minY, minZ;
        public int maxX, maxY, maxZ;
        public byte[] blockData;
        public byte[] blockEntities;
    }

    class InstanceStateData {
        public byte[] modifiedBlocks;
        public byte[] blockEntities;
    }

    class TemplateChunk {
        public String templateId;
        public String worldName;
        public int chunkX, chunkZ;
    }

    class RentalRow {
        public String templateId;
    }
}
