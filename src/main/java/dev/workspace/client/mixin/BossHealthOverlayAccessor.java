package dev.workspace.client.mixin;

import java.util.Map;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;

/**
 * Read-only view of active boss-bar events so the HUD can dodge that band.
 * The official field name is {@code events} (1.21.11). Loom remaps this
 * accessor; raw reflection on the official name would not match at runtime.
 */
@Mixin(BossHealthOverlay.class)
public interface BossHealthOverlayAccessor {
	@Accessor("events")
	Map<UUID, LerpingBossEvent> workspaceClient$getEvents();
}
