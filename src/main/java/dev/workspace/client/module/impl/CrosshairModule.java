package dev.workspace.client.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

import dev.workspace.client.WorkspaceClient;
import dev.workspace.client.module.Category;
import dev.workspace.client.module.Module;
import dev.workspace.client.setting.BooleanSetting;
import dev.workspace.client.setting.ColorSetting;
import dev.workspace.client.setting.NumberSetting;

/**
 * Client-only replacement crosshair and an optional hit flash. No aim assist.
 */
public final class CrosshairModule extends Module {
	private final BooleanSetting custom = addSetting(new BooleanSetting("custom", "Custom Crosshair", true));
	private final BooleanSetting hitMarker = addSetting(new BooleanSetting("hit_marker", "Hit Marker", true));
	private final NumberSetting size = addSetting(new NumberSetting("size", "Size", 4.0, 2.0, 12.0, 1.0));
	private final NumberSetting gap = addSetting(new NumberSetting("gap", "Gap", 2.0, 0.0, 8.0, 1.0));
	private final NumberSetting thickness = addSetting(new NumberSetting("thickness", "Thickness", 1.0, 1.0, 3.0, 1.0));
	private final ColorSetting color = addSetting(new ColorSetting("color", "Color", 0xFFE8EDF2));
	private final ColorSetting markerColor = addSetting(new ColorSetting("marker_color", "Marker Color", 0xFFFFE08A));

	private int hitTicks;
	private int lastHurtTime;
	private Entity lastTarget;

	public CrosshairModule() {
		super("Crosshair", Category.RENDER, "Draws a simple custom crosshair and a brief visual hit marker.");
	}

	public static void registerLayer() {
		HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> (graphics, delta) -> {
			CrosshairModule module = current();
			if (module != null && module.isEnabled() && module.custom.asBoolean()) {
				module.renderCrosshair(graphics);
			} else {
				original.render(graphics, delta);
			}
			if (module != null && module.isEnabled() && module.hitMarker.asBoolean()) {
				module.renderHitMarker(graphics);
			}
		});
	}

	@Override
	public void onTick(Minecraft client) {
		if (this.hitTicks > 0) {
			this.hitTicks--;
		}
		if (!this.hitMarker.asBoolean() || client.player == null) {
			return;
		}
		Entity target = client.crosshairPickEntity;
		if (target instanceof LivingEntity living) {
			int hurtTime = living.hurtTime;
			if (living == this.lastTarget && hurtTime > this.lastHurtTime) {
				this.hitTicks = 8;
			}
			this.lastHurtTime = hurtTime;
			this.lastTarget = living;
		} else {
			this.lastHurtTime = 0;
			this.lastTarget = null;
		}
	}

	private void renderCrosshair(GuiGraphics graphics) {
		int cx = graphics.guiWidth() / 2;
		int cy = graphics.guiHeight() / 2;
		int arm = this.size.asInt();
		int g = this.gap.asInt();
		int t = Math.max(1, this.thickness.asInt());
		int c = this.color.argb();
		int halfT = t / 2;
		// Horizontal arms.
		graphics.fill(cx - g - arm, cy - halfT, cx - g, cy - halfT + t, c);
		graphics.fill(cx + g, cy - halfT, cx + g + arm, cy - halfT + t, c);
		// Vertical arms.
		graphics.fill(cx - halfT, cy - g - arm, cx - halfT + t, cy - g, c);
		graphics.fill(cx - halfT, cy + g, cx - halfT + t, cy + g + arm, c);
	}

	private void renderHitMarker(GuiGraphics graphics) {
		if (this.hitTicks <= 0) {
			return;
		}
		int cx = graphics.guiWidth() / 2;
		int cy = graphics.guiHeight() / 2;
		int reach = 3 + (8 - this.hitTicks);
		int c = this.markerColor.argb();
		graphics.fill(cx - reach, cy - reach, cx - reach + 2, cy - reach + 2, c);
		graphics.fill(cx + reach - 2, cy - reach, cx + reach, cy - reach + 2, c);
		graphics.fill(cx - reach, cy + reach - 2, cx - reach + 2, cy + reach, c);
		graphics.fill(cx + reach - 2, cy + reach - 2, cx + reach, cy + reach, c);
	}

	private static CrosshairModule current() {
		WorkspaceClient workspace = WorkspaceClient.getInstance();
		return workspace == null ? null : workspace.moduleManager().of(CrosshairModule.class);
	}
}
