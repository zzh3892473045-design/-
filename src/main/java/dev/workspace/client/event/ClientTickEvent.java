package dev.workspace.client.event;

import net.minecraft.client.Minecraft;

/**
 * Fired at the end of every client tick.
 */
public final class ClientTickEvent {
	private final Minecraft client;

	public ClientTickEvent(Minecraft client) {
		this.client = client;
	}

	public Minecraft client() {
		return this.client;
	}
}
