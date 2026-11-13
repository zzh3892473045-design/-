package dev.workspace.client.module.impl;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

import dev.workspace.client.WorkspaceClient;
import dev.workspace.client.gui.ClickGuiTheme;
import dev.workspace.client.module.Category;
import dev.workspace.client.module.Module;
import dev.workspace.client.setting.BooleanSetting;
import dev.workspace.client.setting.ColorSetting;
import dev.workspace.client.setting.NumberSetting;

/**
 * Client-only replacement crosshair and an optional hit flash. No aim assist.
 * This layer does not redraw the vanilla attack indicator; when Custom is on,
 * that indicator is not shown. That limitation is intentional for this pass.
 */
public final class CrosshairModule extends Module {
	/**
	 * 1px outline color taken from {@link ClickGuiTheme#OVERLAY}'s slate RGB
	 * ({@code #10141C}). User Color does not override this token.
	 */
	private static final int OUTLINE_COLOR = 0xFF000000 | (ClickGuiTheme.OVERLAY & 0x00FFFFFF);

	private final BooleanSetting custom = addSetting(new BooleanSetting("custom", "Custom Crosshair", true));
	private final BooleanSetting hitMarker = addSetting(new BooleanSetting("hit_marker", "Hit Marker", true));
	private final BooleanSetting outline = addSetting(new BooleanSetting("outline", "Outline", true));
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
			boolean customVisible = module != null
				&& module.isEnabled()
				&& module.custom.asBoolean()
				&& shouldVanillaShowCrosshair(Minecraft.getInstance());
			if (customVisible) {
				module.renderCrosshair(graphics);
			} else if (module == null || !module.isEnabled() || !module.custom.asBoolean()) {
				original.render(graphics, delta);
			}
			if (module != null
				&& module.isEnabled()
				&& module.hitMarker.asBoolean()
				&& shouldVanillaShowCrosshair(Minecraft.getInstance())) {
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
		int xStart = spanStart(cx, t);
		int yStart = spanStart(cy, t);
		int xEnd = xStart + t;
		int yEnd = yStart + t;
		boolean drawOutline = this.outline.asBoolean();
		// Horizontal arms.
		fillArm(graphics, cx - g - arm, yStart, cx - g, yEnd, c, drawOutline);
		fillArm(graphics, cx + g, yStart, cx + g + arm, yEnd, c, drawOutline);
		// Vertical arms.
		fillArm(graphics, xStart, cy - g - arm, xEnd, cy - g, c, drawOutline);
		fillArm(graphics, xStart, cy + g, xEnd, cy + g + arm, c, drawOutline);
	}

	private static void fillArm(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color, boolean drawOutline) {
		if (x1 <= x0 || y1 <= y0) {
			return;
		}
		if (drawOutline) {
			graphics.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, OUTLINE_COLOR);
		}
		graphics.fill(x0, y0, x1, y1, color);
	}

	/**
	 * Inclusive-start / exclusive-end origin for a stroke of {@code thickness}
	 * centered on {@code center}.
	 * Odd thickness is anchored on the center pixel; even thickness straddles
	 * the grid line at {@code center} equally on both sides.
	 */
	static int spanStart(int center, int thickness) {
		if ((thickness & 1) == 1) {
			return center - (thickness - 1) / 2;
		}
		return center - thickness / 2;
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

	/**
	 * Mirrors the vanilla {@code Gui.renderCrosshair} early-outs plus the
	 * {@code hideGui} gate in {@code Gui.render}, and also hides while sleeping.
	 * The vanilla attack indicator is not reproduced here.
	 */
	static boolean shouldVanillaShowCrosshair(Minecraft client) {
		if (client == null || client.player == null || client.level == null || client.options == null) {
			return false;
		}
		if (client.options.hideGui) {
			return false;
		}
		LocalPlayer player = client.player;
		if (player.isSleeping() || player.getSleepTimer() > 0) {
			return false;
		}
		CameraType cameraType = client.options.getCameraType();
		if (cameraType == null || !cameraType.isFirstPerson()) {
			return false;
		}
		MultiPlayerGameMode gameMode = client.gameMode;
		if (gameMode != null && gameMode.getPlayerMode() == GameType.SPECTATOR) {
			if (!canRenderCrosshairForSpectator(client, client.hitResult)) {
				return false;
			}
		}
		if (client.debugEntries != null
			&& client.debugEntries.isCurrentlyEnabled(DebugScreenEntries.THREE_DIMENSIONAL_CROSSHAIR)) {
			return false;
		}
		return true;
	}

	/**
	 * Same conditions as vanilla {@code Gui.canRenderCrosshairForSpectator}.
	 */
	private static boolean canRenderCrosshairForSpectator(Minecraft client, HitResult hit) {
		if (hit == null) {
			return false;
		}
		if (hit.getType() == HitResult.Type.ENTITY && hit instanceof EntityHitResult entityHit) {
			return entityHit.getEntity() instanceof MenuProvider;
		}
		if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult blockHit) {
			Level level = client.level;
			if (level == null) {
				return false;
			}
			BlockPos pos = blockHit.getBlockPos();
			BlockState state = level.getBlockState(pos);
			return state.getMenuProvider(level, pos) != null;
		}
		return false;
	}

	private static CrosshairModule current() {
		WorkspaceClient workspace = WorkspaceClient.getInstance();
		return workspace == null ? null : workspace.moduleManager().of(CrosshairModule.class);
	}
}
