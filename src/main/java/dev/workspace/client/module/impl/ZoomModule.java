package dev.workspace.client.module.impl;

import org.lwjgl.glfw.GLFW;

import dev.workspace.client.WorkspaceClient;
import dev.workspace.client.adapter.ClientAdapters;
import dev.workspace.client.module.Category;
import dev.workspace.client.module.Module;
import dev.workspace.client.setting.EnumSetting;
import dev.workspace.client.setting.NumberSetting;

/**
 * Replaces the world FOV while active, without writing the vanilla FOV option.
 * Applied from {@code GameRendererMixin} on {@code getFov}.
 */
public final class ZoomModule extends Module {
	public enum Mode {
		TOGGLE,
		HOLD
	}

	private final EnumSetting<Mode> mode = addSetting(new EnumSetting<>("mode", "Mode", Mode.TOGGLE));
	private final NumberSetting fov = addSetting(new NumberSetting("fov", "FOV", 30.0, 15.0, 70.0, 1.0));

	public ZoomModule() {
		super("Zoom", Category.RENDER, "Narrows the field of view while held or toggled. Restores vanilla FOV when off.");
		setKeyCode(GLFW.GLFW_KEY_C);
	}

	@Override
	public boolean toggleOnKeyPress() {
		return this.mode.get() != Mode.HOLD;
	}

	public static boolean isEffectivelyZooming() {
		WorkspaceClient workspace = WorkspaceClient.getInstance();
		if (workspace == null) {
			return false;
		}
		ZoomModule module = workspace.moduleManager().of(ZoomModule.class);
		if (module == null || !module.isEnabled()) {
			return false;
		}
		if (module.mode.get() == Mode.HOLD) {
			int key = module.keyCode();
			if (key > 0) {
				return ClientAdapters.isKeyDown(key);
			}
		}
		return true;
	}

	public static Float currentFovOrNull() {
		if (!isEffectivelyZooming()) {
			return null;
		}
		WorkspaceClient workspace = WorkspaceClient.getInstance();
		ZoomModule module = workspace.moduleManager().of(ZoomModule.class);
		return module == null ? null : module.fov.asFloat();
	}
}
