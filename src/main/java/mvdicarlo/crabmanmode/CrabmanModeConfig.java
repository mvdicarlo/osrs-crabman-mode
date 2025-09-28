package mvdicarlo.crabmanmode;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(CrabmanModePlugin.CONFIG_GROUP)
public interface CrabmanModeConfig extends Config {
    @ConfigItem(keyName = "namesBronzeman", name = "Bronzeman Names", position = 1, description = "Configures names of bronzemen to highlight in chat. Format: (name), (name)")
    default String namesBronzeman() {
        return "";
    }

    @ConfigItem(keyName = "enableCrabman", name = "Enable group bronzeman for character name", position = 2, description = "Enables group bronzeman mode for provided character name.")
    default String enableCrabman() {
        return "";
    }

    @ConfigItem(keyName = "storageType", name = "Storage", position = 3, description = "Specifies which storage option to use.")
    default StorageType storageType() {
        return StorageType.AZURE;
    }

    @ConfigItem(secret = true, keyName = "azureSasUrl", name = "Azure Storage Account SAS URL", position = 4, description = "The SAS Url string for your group storage account.")
    default String azureSasUrl() {
        return "";
    }
    @ConfigItem(secret = true, keyName = "firebaseRealtimeDatabaseUrl", name = "Firebase Realtime Database URL", position = 5, description = "The url to your firebase database.")
    default String firebaseRealtimeDatabaseUrl() {
        return "";
    }
}
