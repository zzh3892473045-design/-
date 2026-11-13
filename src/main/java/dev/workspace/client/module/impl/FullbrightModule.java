package dev.workspace.client.module.impl;

import dev.workspace.client.WorkspaceClient;
import dev.workspace.client.module.Category;
import dev.workspace.client.module.Module;
import dev.workspace.client.setting.NumberSetting;

/**
 * Raises the gamma value that {@code LightTexture#updateLightTexture} reads,
 * then restores the player's real option so {@code options.txt} is not
 * overwritten. See {@code LightTextureMixin}.
 */
public final class FullbrightModule extends Module {
	private final NumberSetting gamma = addSetting(new NumberSetting("gamma", "Gamma", 12.0, 1.0, 16.0, 0.5));

	public FullbrightModule() {
		super("Fullbright", Category.RENDER, "Brightens the world locally by boosting gamma during light-map updates.");
	}

	public static boolean isEffectivelyActive() {
		WorkspaceClient workspace = WorkspaceClient.getInstance();
		if (workspace == null) {
			return false;
		}
		FullbrightModule module = workspace.moduleManager().of(FullbrightModule.class);
		return module != null && module.isEnabled();
	}

	public static double currentGamma() {
		WorkspaceClient workspace = WorkspaceClient.getInstance();
		if (workspace == null) {
			return 12.0;
		}
		FullbrightModule module = workspace.moduleManager().of(FullbrightModule.class);
		return module == null ? 12.0 : module.gamma.asDouble();
	}
}
