package dev.workspace.client.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import dev.workspace.client.adapter.ClientAdapters;
import dev.workspace.client.module.Category;
import dev.workspace.client.module.Module;
import dev.workspace.client.setting.EnumSetting;

/**
 * Vanilla-like auto sprint: only while moving forward, and only when hunger
 * would allow a normal sprint. No bypass of vanilla restrictions.
 */
public final class SprintModule extends Module {
	public enum Mode {
		HOLD,
		TOGGLE
	}

	private final EnumSetting<Mode> mode = addSetting(new EnumSetting<>("mode", "Mode", Mode.TOGGLE));

	public SprintModule() {
		super("Sprint", Category.PLAYER, "Keeps vanilla sprint held while you move forward.");
	}

	@Override
	public void onTick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null) {
			return;
		}
		if (this.mode.get() == Mode.HOLD && !isHoldKeyDown()) {
			return;
		}
		if (ClientAdapters.canVanillaSprint(player, client)) {
			player.setSprinting(true);
		}
	}

	private boolean isHoldKeyDown() {
		int key = keyCode();
		if (key <= 0) {
			return true;
		}
		return ClientAdapters.isKeyDown(key);
	}

	@Override
	public boolean toggleOnKeyPress() {
		return this.mode.get() != Mode.HOLD;
	}
}
