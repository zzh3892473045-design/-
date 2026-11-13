package dev.workspace.client.gui;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import dev.workspace.client.WorkspaceClient;
import dev.workspace.client.adapter.ClientAdapters;
import dev.workspace.client.module.Category;
import dev.workspace.client.module.Module;
import dev.workspace.client.setting.BooleanSetting;
import dev.workspace.client.setting.ColorSetting;
import dev.workspace.client.setting.EnumSetting;
import dev.workspace.client.setting.NumberSetting;
import dev.workspace.client.setting.Setting;

/**
 * One draggable column of modules and their expanded settings.
 */
public final class CategoryPanel {
	private final Category category;
	private final List<Module> modules;
	private final Set<Module> expanded = new HashSet<>();
	private int x;
	private int y;
	private int height;
	private boolean dragging;
	private double dragOffsetX;
	private double dragOffsetY;
	private NumberSetting slidingNumber;
	private int slidingColorChannel = -1;
	private ColorSetting slidingColor;

	public CategoryPanel(Category category, List<Module> modules, int x, int y) {
		this.category = category;
		this.modules = modules;
		this.x = x;
		this.y = y;
	}

	public Category category() {
		return this.category;
	}

	public int x() {
		return this.x;
	}

	public int y() {
		return this.y;
	}

	public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
		recomputeHeight();
		int width = ClickGuiTheme.PANEL_WIDTH;
		graphics.fill(this.x, this.y, this.x + width, this.y + this.height, ClickGuiTheme.PANEL);
		graphics.fill(this.x, this.y, this.x + width, this.y + ClickGuiTheme.HEADER_HEIGHT, ClickGuiTheme.HEADER);
		graphics.renderOutline(this.x, this.y, width, this.height, ClickGuiTheme.BORDER);
		graphics.drawString(font, this.category.displayName(), this.x + 6, this.y + 4, ClickGuiTheme.HEADER_TEXT, false);

		int rowY = this.y + ClickGuiTheme.HEADER_HEIGHT;
		for (Module module : this.modules) {
			boolean hover = contains(mouseX, mouseY, this.x, rowY, width, ClickGuiTheme.ROW_HEIGHT);
			graphics.fill(this.x + 1, rowY, this.x + width - 1, rowY + ClickGuiTheme.ROW_HEIGHT, hover ? ClickGuiTheme.ROW_HOVER : ClickGuiTheme.ROW);
			int nameColor = module.isEnabled() ? ClickGuiTheme.ENABLED : ClickGuiTheme.DISABLED;
			graphics.drawString(font, module.name(), this.x + 6, rowY + 3, nameColor, false);
			String mark = this.expanded.contains(module) ? "-" : "+";
			int markX = this.x + width - 10 - font.width(mark);
			graphics.drawString(font, mark, markX, rowY + 3, ClickGuiTheme.TEXT_MUTED, false);
			rowY += ClickGuiTheme.ROW_HEIGHT;
			if (this.expanded.contains(module)) {
				rowY = renderSettings(graphics, font, module, rowY, mouseX, mouseY);
			}
		}
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int width = ClickGuiTheme.PANEL_WIDTH;
		if (contains(mouseX, mouseY, this.x, this.y, width, ClickGuiTheme.HEADER_HEIGHT)) {
			if (button == 0) {
				this.dragging = true;
				this.dragOffsetX = mouseX - this.x;
				this.dragOffsetY = mouseY - this.y;
				return true;
			}
			return false;
		}
		int rowY = this.y + ClickGuiTheme.HEADER_HEIGHT;
		for (Module module : this.modules) {
			if (contains(mouseX, mouseY, this.x, rowY, width, ClickGuiTheme.ROW_HEIGHT)) {
				if (button == 0) {
					module.toggle();
				} else if (button == 1) {
					if (!this.expanded.add(module)) {
						this.expanded.remove(module);
					}
				}
				return true;
			}
			rowY += ClickGuiTheme.ROW_HEIGHT;
			if (this.expanded.contains(module)) {
				int consumed = clickSettings(module, rowY, mouseX, mouseY, button);
				if (consumed >= 0) {
					return consumed > 0;
				}
				rowY += settingsHeight(module);
			}
		}
		return contains(mouseX, mouseY, this.x, this.y, width, this.height) && button == 0;
	}

	public void mouseDragged(double mouseX, double mouseY) {
		if (this.dragging) {
			this.x = (int) Math.round(mouseX - this.dragOffsetX);
			this.y = (int) Math.round(mouseY - this.dragOffsetY);
			this.x = Math.max(0, this.x);
			this.y = Math.max(0, this.y);
			WorkspaceClient workspace = WorkspaceClient.getInstance();
			if (workspace != null) {
				workspace.configManager().setPanelPosition(this.category, this.x, this.y);
			}
			return;
		}
		if (this.slidingNumber != null) {
			applySlider(this.slidingNumber, mouseX);
		}
		if (this.slidingColor != null && this.slidingColorChannel >= 0) {
			applyColorSlider(this.slidingColor, this.slidingColorChannel, mouseX);
		}
	}

	public void mouseReleased() {
		this.dragging = false;
		this.slidingNumber = null;
		this.slidingColor = null;
		this.slidingColorChannel = -1;
	}

	int height() {
		recomputeHeight();
		return this.height;
	}

	private int renderSettings(GuiGraphics graphics, Font font, Module module, int startY, int mouseX, int mouseY) {
		int width = ClickGuiTheme.PANEL_WIDTH;
		int rowY = startY;
		for (Setting<?> setting : module.settings()) {
			int h = settingHeight(setting);
			graphics.fill(this.x + 1, rowY, this.x + width - 1, rowY + h, ClickGuiTheme.SETTING_ROW);
			if (setting instanceof BooleanSetting booleanSetting) {
				String label = setting.displayName() + ": " + (booleanSetting.asBoolean() ? "On" : "Off");
				int color = booleanSetting.asBoolean() ? ClickGuiTheme.ENABLED : ClickGuiTheme.TEXT_MUTED;
				graphics.drawString(font, label, this.x + 8, rowY + 3, color, false);
			} else if (setting instanceof NumberSetting numberSetting) {
				graphics.drawString(font, setting.displayName() + ": " + formatNumber(numberSetting), this.x + 8, rowY + 2, ClickGuiTheme.TEXT, false);
				drawSlider(graphics, rowY + 10, numberSetting.ratio(), mouseX, mouseY);
			} else if (setting instanceof EnumSetting<?> enumSetting) {
				graphics.drawString(font, setting.displayName() + ": " + prettyEnum(enumSetting.get().name()), this.x + 8, rowY + 3, ClickGuiTheme.TEXT, false);
			} else if (setting instanceof ColorSetting colorSetting) {
				graphics.drawString(font, setting.displayName(), this.x + 8, rowY + 2, ClickGuiTheme.TEXT, false);
				drawColorSwatch(graphics, rowY + 2, colorSetting);
				drawColorChannel(graphics, rowY + 11, 0, colorSetting.red() / 255.0);
				drawColorChannel(graphics, rowY + 21, 1, colorSetting.green() / 255.0);
				drawColorChannel(graphics, rowY + 31, 2, colorSetting.blue() / 255.0);
				drawColorChannel(graphics, rowY + 41, 3, colorSetting.alpha() / 255.0);
			} else {
				graphics.drawString(font, setting.displayName(), this.x + 8, rowY + 3, ClickGuiTheme.TEXT, false);
			}
			rowY += h;
		}
		rowY = renderKeybindRow(graphics, font, module, rowY, mouseX, mouseY);
		return rowY;
	}

	private int renderKeybindRow(GuiGraphics graphics, Font font, Module module, int rowY, int mouseX, int mouseY) {
		int width = ClickGuiTheme.PANEL_WIDTH;
		graphics.fill(this.x + 1, rowY, this.x + width - 1, rowY + ClickGuiTheme.SETTING_HEIGHT, ClickGuiTheme.SETTING_ROW);
		boolean capturing = ClickGuiScreen.isCapturing(module);
		String value = capturing ? "..." : ClientAdapters.keyDisplayName(module.keyCode());
		int color = capturing ? ClickGuiTheme.ACCENT : ClickGuiTheme.TEXT;
		graphics.drawString(font, "Bind: " + value, this.x + 8, rowY + 3, color, false);
		return rowY + ClickGuiTheme.SETTING_HEIGHT;
	}

	private void drawSlider(GuiGraphics graphics, int y, double ratio, int mouseX, int mouseY) {
		int left = this.x + 8;
		int right = this.x + ClickGuiTheme.PANEL_WIDTH - 8;
		int fill = left + (int) Math.round((right - left) * Math.max(0.0, Math.min(1.0, ratio)));
		graphics.fill(left, y, right, y + 3, ClickGuiTheme.SLIDER_TRACK);
		graphics.fill(left, y, Math.max(left, fill), y + 3, ClickGuiTheme.ACCENT);
	}

	private void drawColorSwatch(GuiGraphics graphics, int y, ColorSetting colorSetting) {
		int size = 8;
		int left = this.x + ClickGuiTheme.PANEL_WIDTH - 8 - size;
		int right = left + size;
		int bottom = y + size;
		graphics.fill(left - 1, y - 1, right + 1, bottom + 1, ClickGuiTheme.BORDER);
		int opaque = 0xFF000000 | (colorSetting.argb() & 0x00FFFFFF);
		int mid = left + size / 2;
		graphics.fill(left, y, mid, bottom, opaque);
		graphics.fill(mid, y, right, bottom, colorSetting.argb());
	}

	private void drawColorChannel(GuiGraphics graphics, int y, int channel, double ratio) {
		int left = this.x + 8;
		int right = this.x + ClickGuiTheme.PANEL_WIDTH - 8;
		int fill = left + (int) Math.round((right - left) * Math.max(0.0, Math.min(1.0, ratio)));
		int color = channel == 0 ? 0xFFCC5555 : channel == 1 ? 0xFF55AA70 : channel == 2 ? 0xFF5577CC : ClickGuiTheme.TEXT_MUTED;
		graphics.fill(left, y, right, y + 3, ClickGuiTheme.SLIDER_TRACK);
		graphics.fill(left, y, Math.max(left, fill), y + 3, color);
	}

	/**
	 * @return -1 if the point is not inside this settings block, 0 if it is
	 *         inside but unused, 1 if a control handled the click
	 */
	private int clickSettings(Module module, int startY, double mouseX, double mouseY, int button) {
		int width = ClickGuiTheme.PANEL_WIDTH;
		int total = settingsHeight(module);
		if (!contains(mouseX, mouseY, this.x, startY, width, total)) {
			return -1;
		}
		if (button != 0) {
			return 0;
		}
		int rowY = startY;
		for (Setting<?> setting : module.settings()) {
			int h = settingHeight(setting);
			if (contains(mouseX, mouseY, this.x, rowY, width, h)) {
				if (setting instanceof BooleanSetting booleanSetting) {
					booleanSetting.toggle();
					return 1;
				}
				if (setting instanceof NumberSetting numberSetting) {
					this.slidingNumber = numberSetting;
					applySlider(numberSetting, mouseX);
					return 1;
				}
				if (setting instanceof EnumSetting<?> enumSetting) {
					enumSetting.cycle();
					return 1;
				}
				if (setting instanceof ColorSetting colorSetting) {
					int channel = colorChannelAt(rowY, mouseY);
					if (channel >= 0) {
						this.slidingColor = colorSetting;
						this.slidingColorChannel = channel;
						applyColorSlider(colorSetting, channel, mouseX);
					}
					return 1;
				}
				return 0;
			}
			rowY += h;
		}
		if (contains(mouseX, mouseY, this.x, rowY, width, ClickGuiTheme.SETTING_HEIGHT)) {
			ClickGuiScreen.beginCapture(module);
			return 1;
		}
		return 0;
	}

	private int colorChannelAt(int rowY, double mouseY) {
		if (mouseY >= rowY + 10 && mouseY < rowY + 19) {
			return 0;
		}
		if (mouseY >= rowY + 20 && mouseY < rowY + 29) {
			return 1;
		}
		if (mouseY >= rowY + 30 && mouseY < rowY + 39) {
			return 2;
		}
		if (mouseY >= rowY + 40 && mouseY < rowY + 49) {
			return 3;
		}
		return -1;
	}

	private void applySlider(NumberSetting setting, double mouseX) {
		int left = this.x + 8;
		int right = this.x + ClickGuiTheme.PANEL_WIDTH - 8;
		if (right <= left) {
			return;
		}
		setting.setFromRatio((mouseX - left) / (double) (right - left));
	}

	private void applyColorSlider(ColorSetting setting, int channel, double mouseX) {
		int left = this.x + 8;
		int right = this.x + ClickGuiTheme.PANEL_WIDTH - 8;
		if (right <= left) {
			return;
		}
		double ratio = Math.max(0.0, Math.min(1.0, (mouseX - left) / (double) (right - left)));
		setting.setChannel(channel, (int) Math.round(ratio * 255.0));
	}

	private void recomputeHeight() {
		int total = ClickGuiTheme.HEADER_HEIGHT;
		for (Module module : this.modules) {
			total += ClickGuiTheme.ROW_HEIGHT;
			if (this.expanded.contains(module)) {
				total += settingsHeight(module);
			}
		}
		this.height = Math.max(total, ClickGuiTheme.HEADER_HEIGHT);
	}

	private static int settingsHeight(Module module) {
		int total = ClickGuiTheme.SETTING_HEIGHT; // bind row
		for (Setting<?> setting : module.settings()) {
			total += settingHeight(setting);
		}
		return total;
	}

	private static int settingHeight(Setting<?> setting) {
		if (setting instanceof ColorSetting) {
			return ClickGuiTheme.COLOR_HEIGHT;
		}
		return ClickGuiTheme.SETTING_HEIGHT;
	}

	private static String formatNumber(NumberSetting setting) {
		double value = setting.asDouble();
		if (setting.step() >= 1.0 && value == Math.rint(value)) {
			return Integer.toString(setting.asInt());
		}
		return String.format(java.util.Locale.ROOT, "%.2f", value);
	}

	private static String prettyEnum(String name) {
		String lower = name.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
		if (lower.isEmpty()) {
			return name;
		}
		return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
	}

	private static boolean contains(double mx, double my, int x, int y, int width, int height) {
		return mx >= x && mx < x + width && my >= y && my < y + height;
	}

}
