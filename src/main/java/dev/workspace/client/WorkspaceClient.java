package dev.workspace.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

import dev.workspace.client.config.ConfigManager;
import dev.workspace.client.event.ClientTickEvent;
import dev.workspace.client.event.EventBus;
import dev.workspace.client.event.HudRenderEvent;
import dev.workspace.client.input.KeybindManager;
import dev.workspace.client.module.ModuleManager;
import dev.workspace.client.module.impl.CrosshairModule;

/**
 * Client entrypoint. Wires the event bus, modules, keybinds, config, and HUD
 * layer. Product name is the placeholder "Workspace Client".
 */
public final class WorkspaceClient implements ClientModInitializer {
	public static final String MOD_ID = "workspace-client";
	public static final String NAME = "Workspace Client";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static WorkspaceClient instance;

	private final EventBus eventBus = new EventBus();
	private final ModuleManager moduleManager = new ModuleManager();
	private final ConfigManager configManager = new ConfigManager();
	private final KeybindManager keybindManager = new KeybindManager();

	public static WorkspaceClient getInstance() {
		return instance;
	}

	public EventBus eventBus() {
		return this.eventBus;
	}

	public ModuleManager moduleManager() {
		return this.moduleManager;
	}

	public ConfigManager configManager() {
		return this.configManager;
	}

	public KeybindManager keybindManager() {
		return this.keybindManager;
	}

	@Override
	public void onInitializeClient() {
		instance = this;
		this.moduleManager.registerBuiltins();
		this.keybindManager.register();
		registerClientHooks();
		CrosshairModule.registerLayer();
		this.configManager.load();
		LOGGER.info("{} 0.2.0 initialized", NAME);
	}

	private void registerClientHooks() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			this.eventBus.post(new ClientTickEvent(client));
			this.moduleManager.tickEnabled(client);
			this.keybindManager.tick(client);
			this.configManager.tickDebounce();
		});
		ClientLifecycleEvents.CLIENT_STOPPING.register(this::onClientStopping);
		// Attach before chat so the layer inherits vanilla hideGui.
		// HudRenderCallback is deprecated since Fabric API 0.116 and is not used.
		HudElementRegistry.attachElementBefore(
			VanillaHudElements.CHAT,
			Identifier.fromNamespaceAndPath(MOD_ID, "overlay"),
			(graphics, deltaTracker) -> this.eventBus.post(new HudRenderEvent(graphics, deltaTracker))
		);
	}

	private void onClientStopping(Minecraft client) {
		try {
			this.configManager.saveNow();
		} catch (RuntimeException exception) {
			LOGGER.warn("Failed to save config on shutdown", exception);
		}
	}
}
