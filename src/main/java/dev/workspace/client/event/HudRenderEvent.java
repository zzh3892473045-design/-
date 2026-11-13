package dev.workspace.client.event;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Fired from the Fabric {@code HudElementRegistry} overlay layer each frame
 * the vanilla HUD is drawn.
 */
public final class HudRenderEvent {
	private final GuiGraphics graphics;
	private final DeltaTracker deltaTracker;

	public HudRenderEvent(GuiGraphics graphics, DeltaTracker deltaTracker) {
		this.graphics = graphics;
		this.deltaTracker = deltaTracker;
	}

	public GuiGraphics graphics() {
		return this.graphics;
	}

	public DeltaTracker deltaTracker() {
		return this.deltaTracker;
	}
}
