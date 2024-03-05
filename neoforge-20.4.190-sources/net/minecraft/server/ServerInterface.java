package net.minecraft.server;

import net.minecraft.server.dedicated.DedicatedServerProperties;

public interface ServerInterface extends ServerInfo {
    DedicatedServerProperties getProperties();

    /**
     * Returns the server's hostname.
     */
    String getServerIp();

    /**
     * Never used, but "getServerPort" is already taken.
     */
    int getServerPort();

    /**
     * Returns the server message of the day
     */
    String getServerName();

    /**
     * Returns an array of the usernames of all the connected players.
     */
    String[] getPlayerNames();

    String getLevelIdName();

    /**
     * Used by RCon's Query in the form of "MajorServerMod 1.2.3: MyPlugin 1.3" AnotherPlugin 2.1" AndSoForth 1.0".
     */
    String getPluginNames();

    /**
     * Handle a command received by an RCon instance
     */
    String runCommand(String pCommand);
}
