package dev.workspace.client.module.impl;

import dev.workspace.client.module.Category;
import dev.workspace.client.module.Module;
import dev.workspace.client.setting.EnumSetting;

/**
 * Vanilla-like sneak hold: equivalent to keeping the sneak key pressed.
 * Applied at the end of {@code KeyboardInput#tick()} so collision and
 * the input packet see the same shift state. Does not force sneak while
 * riding, and does not change pose, collision shape, or sneak speed.
 */
public final class ToggleSneakModule extends Module {
	public enum Mode {
		HOLD,
		TOGGLE
	}

	private final EnumSetting<Mode> mode = addSetting(new EnumSetting<>("mode", "Mode", Mode.TOGGLE));

	public ToggleSneakModule() {
		super("ToggleSneak", Category.PLAYER, "Holds vanilla sneak as if the sneak key is held. Does not force sneak while riding.");
	}

	public Mode mode() {
		return this.mode.get();
	}

	@Override
	public boolean toggleOnKeyPress() {
		return this.mode.get() != Mode.HOLD;
	}
}
