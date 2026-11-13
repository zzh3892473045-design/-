package dev.workspace.client.gui;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import dev.workspace.client.WorkspaceClient;
import dev.workspace.client.module.Category;
import dev.workspace.client.module.Module;
import dev.workspace.client.module.impl.ClickGuiModule;

/**
 * Custom settings screen. Category columns are drawn by {@link CategoryPanel}.
 * Escape or the ClickGUI bind (default Right Shift) closes the screen.
 */
public final class ClickGuiScreen extends Screen {
	private static Module capturingModule;

	private final Screen parent;
	private final List<CategoryPanel> panels = new ArrayList<>();

	public ClickGuiScreen(Screen parent) {
		super(Component.literal("Workspace Client"));
		this.parent = parent;
	}

	public static void beginCapture(Module module) {
		capturingModule = module;
	}

	public static boolean isCapturing(Module module) {
		return capturingModule == module;
	}

	@Override
	protected void init() {
		this.panels.clear();
		WorkspaceClient workspace = WorkspaceClient.getInstance();
		if (workspace == null) {
			return;
		}
		int index = 0;
		int fallbackX = 20;
		int fallbackY = 32;
		for (Category category : Category.values()) {
			List<Module> modules = workspace.moduleManager().modules(category);
			if (modules.isEmpty()) {
				continue;
			}
			int[] position = workspace.configManager().panelPosition(category, fallbackX, fallbackY);
			this.panels.add(new CategoryPanel(category, modules, position[0], position[1]));
			fallbackX += ClickGuiTheme.PANEL_WIDTH + ClickGuiTheme.PANEL_GAP;
			index++;
			if (index == 3) {
				fallbackX = 20;
				fallbackY += 160;
			}
		}
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(0, 0, this.width, this.height, ClickGuiTheme.OVERLAY);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawString(this.font, "Workspace Client", 20, 10, ClickGuiTheme.HEADER_TEXT, false);
		for (CategoryPanel panel : this.panels) {
			panel.render(graphics, this.font, mouseX, mouseY);
		}
		String hint = capturingModule != null
			? "Press a key to bind, or Esc to clear"
			: "Esc or Right Shift to close  ·  left click toggles  ·  right click opens settings";
		int hintX = Math.max(20, (this.width - this.font.width(hint)) / 2);
		graphics.drawString(this.font, hint, hintX, this.height - 14, ClickGuiTheme.HINT, false);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
		for (int i = this.panels.size() - 1; i >= 0; i--) {
			if (this.panels.get(i).mouseClicked(event.x(), event.y(), event.button())) {
				return true;
			}
		}
		return super.mouseClicked(event, doubled);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
		for (CategoryPanel panel : this.panels) {
			panel.mouseDragged(event.x(), event.y());
		}
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		for (CategoryPanel panel : this.panels) {
			panel.mouseReleased();
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (capturingModule != null) {
			int key = event.key();
			if (key == GLFW.GLFW_KEY_ESCAPE) {
				capturingModule.setKeyCode(0);
			} else if (key != GLFW.GLFW_KEY_UNKNOWN) {
				capturingModule.setKeyCode(key);
			}
			capturingModule = null;
			return true;
		}
		if (isCloseKey(event.key())) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return capturingModule == null;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	/**
	 * Leaves this screen without touching the ClickGUI module enabled flag.
	 * Used by {@link ClickGuiModule#onDisable()} to avoid a setEnabled loop.
	 */
	public void dismiss() {
		capturingModule = null;
		if (this.minecraft != null) {
			this.minecraft.setScreen(this.parent);
		}
	}

	@Override
	public void onClose() {
		dismiss();
		WorkspaceClient workspace = WorkspaceClient.getInstance();
		if (workspace != null) {
			ClickGuiModule module = workspace.moduleManager().of(ClickGuiModule.class);
			if (module != null && module.isEnabled()) {
				module.setEnabled(false);
			}
		}
	}

	@Override
	public void removed() {
		capturingModule = null;
		super.removed();
	}

	private static boolean isCloseKey(int key) {
		if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
			return true;
		}
		WorkspaceClient workspace = WorkspaceClient.getInstance();
		if (workspace == null) {
			return false;
		}
		ClickGuiModule module = workspace.moduleManager().of(ClickGuiModule.class);
		return module != null && module.keyCode() > 0 && module.keyCode() == key;
	}
}
