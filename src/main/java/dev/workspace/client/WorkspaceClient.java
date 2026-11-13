package dev.workspace.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WorkspaceClient implements ClientModInitializer {
	public static final String MOD_ID = "workspace-client";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		LOGGER.info("Workspace Client initialized");
	}
}
