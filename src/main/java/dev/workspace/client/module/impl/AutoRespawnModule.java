package dev.workspace.client.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;

import dev.workspace.client.module.Category;
import dev.workspace.client.module.Module;
import dev.workspace.client.setting.NumberSetting;

/**
 * Presses the vanilla death-screen respawn (or hardcore spectate) action
 * after a delay, by calling {@link net.minecraft.client.player.LocalPlayer#respawn()}.
 * Never skips the vanilla 20-tick button unlock, and does not click "Title Screen".
 */
public final class AutoRespawnModule extends Module {
	private final NumberSetting delay = addSetting(new NumberSetting("delay", "Delay", 0.5, 0.0, 10.0, 0.5));

	private int ticksOnDeathScreen;
	private boolean respawnRequested;

	public AutoRespawnModule() {
		super("AutoRespawn", Category.MISC, "Calls the vanilla respawn action after a delay on the death screen.");
	}

	@Override
	protected void onDisable() {
		resetTimer();
	}

	@Override
	public void onTick(Minecraft client) {
		if (!(client.screen instanceof DeathScreen) || client.player == null) {
			resetTimer();
			return;
		}
		if (this.respawnRequested) {
			return;
		}
		this.ticksOnDeathScreen++;
		if (this.ticksOnDeathScreen >= requiredTicks()) {
			client.player.respawn();
			this.respawnRequested = true;
		}
	}

	/**
	 * Converts the delay setting (seconds) to ticks with {@link Math#ceil},
	 * then enforces at least the vanilla 20-tick unlock:
	 * {@code max(20, ceil(delay * 20))}. With the 0.5-second step, {@code delay * 20}
	 * is an integer, so ceil and rounding agree (0.0 and 0.5 both wait 20 ticks).
	 */
	private int requiredTicks() {
		int fromDelay = (int) Math.ceil(this.delay.asDouble() * 20.0);
		return Math.max(20, fromDelay);
	}

	private void resetTimer() {
		this.ticksOnDeathScreen = 0;
		this.respawnRequested = false;
	}
}
