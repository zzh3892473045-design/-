package dev.workspace.client.hud;

import org.joml.Matrix3x2fStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;

import dev.workspace.client.WorkspaceClient;
import dev.workspace.client.event.HudRenderEvent;
import dev.workspace.client.module.Module;
import dev.workspace.client.module.impl.HudModule;

/**
 * In-world overlay: watermark, FPS, coordinates, and the enabled-module list.
 * Scaling uses {@link GuiGraphics#pose()}, which is a {@link Matrix3x2fStack}
 * on 1.21.6+ (official mappings, confirmed via NeoForge / mappings javadoc).
 */
public final class HudRenderer {
	private HudRenderer() {
	}

	public static void render(HudRenderEvent event, HudModule module) {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null || client.level == null) {
			return;
		}
		GuiGraphics graphics = event.graphics();
		Font font = client.font;
		float scale = module.scale();
		int color = module.color();
		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.scale(scale, scale);
		try {
			int y = 4;
			int x = 4;
			if (module.showWatermark()) {
				y = drawLine(graphics, font, x, y, WorkspaceClient.NAME, color);
			}
			if (module.showFps()) {
				y = drawLine(graphics, font, x, y, "FPS " + client.getFps(), color);
			}
			if (module.showCoordinates()) {
				LocalPlayer player = client.player;
				String coords = String.format(
					java.util.Locale.ROOT,
					"XYZ %.1f %.1f %.1f",
					player.getX(),
					player.getY(),
					player.getZ()
				);
				y = drawLine(graphics, font, x, y, coords, color);
			}
			if (module.showModuleList()) {
				drawModuleList(graphics, font, color, scale);
			}
		} finally {
			pose.popMatrix();
		}
	}

	private static int drawLine(GuiGraphics graphics, Font font, int x, int y, String text, int color) {
		int width = font.width(text);
		int height = font.lineHeight;
		graphics.fill(x - 1, y - 1, x + width + 1, y + height, 0x66000000);
		graphics.drawString(font, text, x, y, color, false);
		return y + height + 2;
	}

	private static void drawModuleList(GuiGraphics graphics, Font font, int color, float scale) {
		WorkspaceClient workspace = WorkspaceClient.getInstance();
		if (workspace == null) {
			return;
		}
		int screenWidth = Math.round(graphics.guiWidth() / Math.max(0.01f, scale));
		int y = 4;
		for (Module module : workspace.moduleManager().enabledVisibleInHud()) {
			String name = module.name();
			int width = font.width(name);
			int x = screenWidth - width - 4;
			int height = font.lineHeight;
			graphics.fill(x - 1, y - 1, x + width + 1, y + height, 0x66000000);
			graphics.drawString(font, name, x, y, color, false);
			y += height + 2;
		}
	}
}
