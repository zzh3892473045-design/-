package dev.workspace.client.module.impl;

import net.minecraft.client.Minecraft;

import dev.workspace.client.gui.ClickGuiScreen;
import dev.workspace.client.input.KeybindManager;
import dev.workspace.client.module.Category;
import dev.workspace.client.module.Module;

public final class ClickGuiModule extends Module {
	public ClickGuiModule() {
		super("ClickGUI", Category.CLIENT, "Opens the on-screen module settings.");
		setKeyCode(KeybindManager.DEFAULT_CLICK_GUI_KEY);
	}

	@Override
	protected void onEnable() {
		Minecraft client = Minecraft.getInstance();
		if (client != null && !(client.screen instanceof ClickGuiScreen)) {
			client.setScreen(new ClickGuiScreen(client.screen));
		}
	}

	@Override
	protected void onDisable() {
		Minecraft client = Minecraft.getInstance();
		if (client != null && client.screen instanceof ClickGuiScreen screen) {
			screen.dismiss();
		}
	}
}
