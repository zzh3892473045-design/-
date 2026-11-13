package dev.workspace.client.input;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

import dev.workspace.client.WorkspaceClient;
import dev.workspace.client.adapter.ClientAdapters;
import dev.workspace.client.gui.ClickGuiScreen;
import dev.workspace.client.module.Module;
import dev.workspace.client.module.impl.ClickGuiModule;

/**
 * Fabric {@link KeyMapping} for opening the ClickGUI (default Right Shift)
 * plus raw GLFW edge detection for per-module binds. Keys are ignored while
 * chat or another text field is focused.
 */
public final class KeybindManager {
	public static final int DEFAULT_CLICK_GUI_KEY = GLFW.GLFW_KEY_RIGHT_SHIFT;

	private KeyMapping clickGuiMapping;
	private final boolean[] wasDown = new boolean[GLFW.GLFW_KEY_LAST + 1];
	private boolean appliedMappingAfterOptions;

	public void register() {
		KeyMapping.Category category = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath(WorkspaceClient.MOD_ID, "main")
		);
		this.clickGuiMapping = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.workspace-client.clickgui",
			InputConstants.Type.KEYSYM,
			DEFAULT_CLICK_GUI_KEY,
			category
		));
		WorkspaceClient workspace = WorkspaceClient.getInstance();
		if (workspace != null) {
			ClickGuiModule module = workspace.moduleManager().of(ClickGuiModule.class);
			if (module != null) {
				syncClickGuiMapping(module.keyCode());
			}
		}
	}

	public KeyMapping clickGuiMapping() {
		return this.clickGuiMapping;
	}

	public void tick(Minecraft client) {
		if (client == null || ClientAdapters.isTextInputFocused(client)) {
			clearWasDown();
			return;
		}
		// Options may overwrite the Fabric mapping after config load; push once,
		// then follow later changes made in the vanilla controls menu.
		if (!this.appliedMappingAfterOptions) {
			WorkspaceClient workspace = WorkspaceClient.getInstance();
			if (workspace != null) {
				ClickGuiModule module = workspace.moduleManager().of(ClickGuiModule.class);
				if (module != null) {
					syncClickGuiMapping(module.keyCode());
				}
			}
			this.appliedMappingAfterOptions = true;
		} else {
			pullClickGuiMappingIntoModule();
		}
		boolean screenIsClickGui = client.screen instanceof ClickGuiScreen;
		boolean handledClickGui = false;
		if (this.clickGuiMapping != null) {
			while (this.clickGuiMapping.consumeClick()) {
				handledClickGui = true;
				if (screenIsClickGui) {
					if (client.screen != null) {
						client.screen.onClose();
					}
				} else {
					openClickGui(client);
				}
			}
		}
		WorkspaceClient workspace = WorkspaceClient.getInstance();
		if (workspace == null) {
			return;
		}
		// Always record current key state so closing the GUI on a held bind
		// cannot immediately re-open it on the next tick.
		for (Module module : workspace.moduleManager().modules()) {
			int key = module.keyCode();
			if (key <= 0 || key >= this.wasDown.length) {
				continue;
			}
			boolean down = ClientAdapters.isKeyDown(key);
			boolean pressed = down && !this.wasDown[key];
			this.wasDown[key] = down;
			if (!pressed || screenIsClickGui) {
				continue;
			}
			if (module instanceof ClickGuiModule) {
				if (!handledClickGui) {
					openClickGui(client);
				}
				continue;
			}
			if (module.toggleOnKeyPress()) {
				module.toggle();
			}
		}
	}

	public static void openClickGui(Minecraft client) {
		if (client == null || client.screen instanceof ClickGuiScreen) {
			return;
		}
		ClickGuiModule module = WorkspaceClient.getInstance().moduleManager().of(ClickGuiModule.class);
		if (module != null && !module.isEnabled()) {
			module.setEnabled(true);
			return;
		}
		Screen parent = client.screen;
		client.setScreen(new ClickGuiScreen(parent));
	}

	/**
	 * Keeps the Fabric {@link KeyMapping} in the vanilla controls menu in
	 * lockstep with {@link ClickGuiModule#keyCode()}.
	 */
	public void onModuleKeyChanged(Module module, int keyCode) {
		if (module instanceof ClickGuiModule) {
			syncClickGuiMapping(keyCode);
		}
	}

	public void syncClickGuiMapping(int glfwKey) {
		if (this.clickGuiMapping == null) {
			return;
		}
		InputConstants.Key next = glfwKey <= 0
			? InputConstants.UNKNOWN
			: InputConstants.Type.KEYSYM.getOrCreate(glfwKey);
		if (next.equals(boundKey(this.clickGuiMapping))) {
			return;
		}
		this.clickGuiMapping.setKey(next);
		KeyMapping.resetMapping();
		Minecraft client = Minecraft.getInstance();
		if (client != null && client.options != null) {
			client.options.save();
		}
	}

	private void pullClickGuiMappingIntoModule() {
		if (this.clickGuiMapping == null) {
			return;
		}
		WorkspaceClient workspace = WorkspaceClient.getInstance();
		if (workspace == null) {
			return;
		}
		ClickGuiModule module = workspace.moduleManager().of(ClickGuiModule.class);
		if (module == null) {
			return;
		}
		InputConstants.Key bound = boundKey(this.clickGuiMapping);
		if (bound.getType() == InputConstants.Type.MOUSE) {
			return;
		}
		int mapped = glfwKeyOf(bound);
		if (mapped != module.keyCode()) {
			module.setKeyCode(mapped);
		}
	}

	private static int glfwKeyOf(InputConstants.Key key) {
		if (key == null || key.equals(InputConstants.UNKNOWN) || key.getType() != InputConstants.Type.KEYSYM) {
			return 0;
		}
		return key.getValue();
	}

	private static InputConstants.Key boundKey(KeyMapping mapping) {
		if (mapping == null || mapping.isUnbound()) {
			return InputConstants.UNKNOWN;
		}
		try {
			return InputConstants.getKey(mapping.saveString());
		} catch (RuntimeException ignored) {
			return InputConstants.UNKNOWN;
		}
	}

	private void clearWasDown() {
		for (int i = 0; i < this.wasDown.length; i++) {
			this.wasDown[i] = false;
		}
	}
}
