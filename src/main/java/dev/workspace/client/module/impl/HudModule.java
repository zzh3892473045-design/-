package dev.workspace.client.module.impl;

import dev.workspace.client.event.HudRenderEvent;
import dev.workspace.client.hud.HudRenderer;
import dev.workspace.client.module.Category;
import dev.workspace.client.module.Module;
import dev.workspace.client.setting.BooleanSetting;
import dev.workspace.client.setting.ColorSetting;
import dev.workspace.client.setting.NumberSetting;

public final class HudModule extends Module {
	private final BooleanSetting watermark = addSetting(new BooleanSetting("watermark", "Watermark", true));
	private final BooleanSetting fps = addSetting(new BooleanSetting("fps", "FPS", true));
	private final BooleanSetting coordinates = addSetting(new BooleanSetting("coordinates", "Coordinates", true));
	private final BooleanSetting moduleList = addSetting(new BooleanSetting("module_list", "Module List", true));
	private final NumberSetting scale = addSetting(new NumberSetting("scale", "Scale", 1.0, 0.5, 2.0, 0.1));
	private final ColorSetting color = addSetting(new ColorSetting("color", "Color", 0xFFE8EDF2));

	public HudModule() {
		super("HUD", Category.RENDER, "In-game watermark, FPS, coordinates, and enabled modules.");
		listen(HudRenderEvent.class, event -> HudRenderer.render(event, this));
		setEnabled(true, false);
	}

	public boolean showWatermark() {
		return this.watermark.asBoolean();
	}

	public boolean showFps() {
		return this.fps.asBoolean();
	}

	public boolean showCoordinates() {
		return this.coordinates.asBoolean();
	}

	public boolean showModuleList() {
		return this.moduleList.asBoolean();
	}

	public float scale() {
		return this.scale.asFloat();
	}

	public int color() {
		return this.color.argb();
	}
}
