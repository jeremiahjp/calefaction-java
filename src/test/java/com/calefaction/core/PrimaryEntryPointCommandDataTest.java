package com.calefaction.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.junit.jupiter.api.Test;

class PrimaryEntryPointCommandDataTest {

    @Test
    void testSerializationToData() {
        PrimaryEntryPointCommandData command = new PrimaryEntryPointCommandData();
        assertEquals("launch", command.getName());

        DataObject data = command.toData();
        assertEquals("launch", data.getString("name"));
        assertEquals("Launch the Contexto Activity", data.getString("description"));
        assertEquals(4, data.getInt("type")); // PRIMARY_ENTRY_POINT
        assertEquals(2, data.getInt("handler")); // DISCORD_LAUNCH_ACTIVITY

        DataArray integrationTypes = data.getArray("integration_types");
        assertNotNull(integrationTypes);
        assertEquals(2, integrationTypes.length());

        DataArray contexts = data.getArray("contexts");
        assertNotNull(contexts);
        assertEquals(3, contexts.length());
    }

    @Test
    void testContextsAndIntegrations() {
        PrimaryEntryPointCommandData command = new PrimaryEntryPointCommandData("custom_launch", "Custom Description", 2);
        assertEquals("custom_launch", command.getName());
        assertFalse(command.isNSFW());
        assertTrue(command.getContexts().contains(InteractionContextType.GUILD));
        assertTrue(command.getContexts().contains(InteractionContextType.BOT_DM));
        assertTrue(command.getContexts().contains(InteractionContextType.PRIVATE_CHANNEL));
        assertTrue(command.getIntegrationTypes().contains(IntegrationType.GUILD_INSTALL));
        assertTrue(command.getIntegrationTypes().contains(IntegrationType.USER_INSTALL));
    }
}
