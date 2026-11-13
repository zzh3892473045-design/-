package dev.workspace.client.hud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.joml.Matrix3x2fStack;

import com.mojang.blaze3d.pipeline.RenderPipeline;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import dev.workspace.client.WorkspaceClient;
import dev.workspace.client.event.HudRenderEvent;
import dev.workspace.client.gui.ClickGuiTheme;
import dev.workspace.client.module.Module;
import dev.workspace.client.module.impl.HudModule;

/**
 * In-world overlay: watermark, FPS, coordinates, facing, armor, effects, and the
 * enabled-module list. Colors come from {@link ClickGuiTheme}; the user Color
 * setting only overrides main text. Scaling uses {@link GuiGraphics#pose()}, a
 * {@link Matrix3x2fStack} on 1.21.6+ (official mappings).
 */
public final class HudRenderer {
	private static final int SAFE_INSET = 6;
	private static final int CHIP_PAD_H = 3;
	private static final int CHIP_PAD_V = 2;
	private static final int ROW_GAP = 3;
	private static final int ACCENT_BAR = 2;
	private static final int CROSSHAIR_RESERVE = 24;
	private static final int COLUMN_GAP = 6;
	private static final String ELLIPSIS = "...";

	/** {@link ClickGuiTheme#PANEL} RGB with the suggested 0x99 (~60%) alpha. */
	private static final int CHIP_BACKGROUND = 0x99000000 | (ClickGuiTheme.PANEL & 0x00FFFFFF);

	/**
	 * Vanilla yaw 0 looks toward +Z (South) and increases clockwise from above.
	 * Index 0 is S; each step is 45°. This is the conventional official mapping
	 * and must be confirmed in-game against +Z / -Z / +X / -X. Do not apply an
	 * extra 180° offset unless those axis checks contradict the table.
	 */
	private static final String[] COMPASS_8 = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

	private static final EquipmentSlot[] ARMOR_SLOTS = {
		EquipmentSlot.HEAD,
		EquipmentSlot.CHEST,
		EquipmentSlot.LEGS,
		EquipmentSlot.FEET
	};
	private static final int ARMOR_ICON = 16;
	private static final int ARMOR_BAR = 2;
	private static final int ARMOR_ITEM_GAP = 3;
	private static final int ARMOR_PERCENT_GAP = 3;
	/** Visual-spec HIT / low colors; not added to {@link ClickGuiTheme}. */
	private static final int ARMOR_MID = 0xFFE8C56B;
	private static final int ARMOR_LOW = 0xFFCC5555;

	private static final int EFFECT_ICON = 18;
	private static final int EFFECT_GAP = 3;
	private static final int EFFECTS_PER_ROW = 5;
	private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

	private HudRenderer() {
	}

	public static void render(HudRenderEvent event, HudModule module) {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null || client.level == null || client.options == null) {
			return;
		}
		if (client.options.hideGui) {
			return;
		}
		if (module.hideOnDebug() && client.getDebugOverlay() != null && client.getDebugOverlay().showDebugScreen()) {
			return;
		}
		GuiGraphics graphics = event.graphics();
		Font font = client.font;
		float scale = Math.max(0.01f, module.scale());
		int textColor = module.color();
		int guiWidth = graphics.guiWidth();
		int guiHeight = graphics.guiHeight();
		int spaceW = Math.max(1, Math.round(guiWidth / scale));
		int spaceH = Math.max(1, Math.round(guiHeight / scale));
		HudSafeInsets insets = HudSafeInsets.measure(client, font, guiWidth, guiHeight);

		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.scale(scale, scale);
		try {
			LayoutBounds bounds = LayoutBounds.from(spaceW, spaceH, scale, insets);
			int y = bounds.top;
			int chipH = CHIP_PAD_V * 2 + font.lineHeight;
			if (module.showWatermark() && y + chipH <= bounds.leftBottom) {
				y = drawWatermark(graphics, font, bounds, y, textColor);
			}
			if (module.showFps() && y + chipH <= bounds.leftBottom) {
				y = drawLabeledValue(graphics, font, bounds, y, "FPS", String.valueOf(client.getFps()), textColor);
			}
			if (module.showCoordinates() && y + chipH <= bounds.leftBottom) {
				LocalPlayer player = client.player;
				String value = String.format(
					Locale.ROOT,
					"%.1f %.1f %.1f",
					player.getX(),
					player.getY(),
					player.getZ()
				);
				y = drawLabeledValue(graphics, font, bounds, y, "XYZ", value, textColor);
			}
			if (module.showFacing() && y + chipH <= bounds.leftBottom) {
				float partialTick = event.deltaTracker().getGameTimeDeltaPartialTick(false);
				y = drawFacing(graphics, font, bounds, y, client.player, partialTick, module.showFacingPitch(), textColor);
			}
			int leftCursor = y;
			int moduleListBottom = bounds.top;
			if (module.showModuleList()) {
				moduleListBottom = drawModuleList(graphics, font, bounds);
			}
			if (module.showEffects()) {
				leftCursor = drawEffects(graphics, font, bounds, client.player, moduleListBottom, leftCursor, textColor);
			}
			if (module.showArmor()) {
				drawArmor(graphics, font, bounds, insets, client.player, leftCursor, module.showArmorPercent());
			}
		} finally {
			pose.popMatrix();
		}
	}

	private static int drawWatermark(GuiGraphics graphics, Font font, LayoutBounds bounds, int y, int textColor) {
		int x = bounds.left;
		int maxWidth = bounds.leftMaxWidth;
		if (maxWidth <= 0) {
			return y;
		}
		String name = ellipsize(font, WorkspaceClient.NAME, Math.max(0, maxWidth - CHIP_PAD_H * 2 - ACCENT_BAR));
		int textW = font.width(name);
		int width = ACCENT_BAR + CHIP_PAD_H * 2 + textW;
		int height = CHIP_PAD_V * 2 + font.lineHeight;
		width = Math.min(width, maxWidth);
		if (intersectsHotZone(x, y, x + width, y + height, graphics, bounds.scale)) {
			width = shrinkWidthToClearHotZone(x, y, height, width, graphics, bounds.scale);
			if (width <= ACCENT_BAR + CHIP_PAD_H * 2) {
				return y;
			}
			name = ellipsize(font, WorkspaceClient.NAME, Math.max(0, width - CHIP_PAD_H * 2 - ACCENT_BAR));
			width = Math.min(width, ACCENT_BAR + CHIP_PAD_H * 2 + font.width(name));
		}
		fillChip(graphics, x, y, width, height);
		graphics.fill(x, y, x + ACCENT_BAR, y + height, ClickGuiTheme.ACCENT);
		graphics.drawString(font, name, x + ACCENT_BAR + CHIP_PAD_H, y + CHIP_PAD_V, textColor, false);
		return y + height + ROW_GAP;
	}

	private static int drawLabeledValue(
		GuiGraphics graphics,
		Font font,
		LayoutBounds bounds,
		int y,
		String label,
		String value,
		int textColor
	) {
		int x = bounds.left;
		int maxWidth = bounds.leftMaxWidth;
		if (maxWidth <= 0) {
			return y;
		}
		int height = CHIP_PAD_V * 2 + font.lineHeight;
		int width = maxWidth;
		if (intersectsHotZone(x, y, x + width, y + height, graphics, bounds.scale)) {
			width = shrinkWidthToClearHotZone(x, y, height, width, graphics, bounds.scale);
		}
		int inner = Math.max(0, width - CHIP_PAD_H * 2);
		String labelText = label;
		String gap = " ";
		int labelWidth = font.width(labelText);
		int gapWidth = font.width(gap);
		int remaining = inner - labelWidth - gapWidth;
		if (remaining < 0) {
			labelText = ellipsize(font, label, inner);
			gap = "";
			value = "";
			labelWidth = font.width(labelText);
			gapWidth = 0;
		} else {
			value = ellipsize(font, value, remaining);
		}
		int content = labelWidth + gapWidth + font.width(value);
		width = Math.min(width, CHIP_PAD_H * 2 + content);
		if (width <= 0) {
			return y;
		}
		fillChip(graphics, x, y, width, height);
		int textX = x + CHIP_PAD_H;
		int textY = y + CHIP_PAD_V;
		graphics.drawString(font, labelText, textX, textY, ClickGuiTheme.TEXT_MUTED, false);
		if (!value.isEmpty()) {
			graphics.drawString(font, gap + value, textX + labelWidth, textY, textColor, false);
		}
		return y + height + ROW_GAP;
	}

	private static int drawFacing(
		GuiGraphics graphics,
		Font font,
		LayoutBounds bounds,
		int y,
		LocalPlayer player,
		float partialTick,
		boolean showPitch,
		int textColor
	) {
		int maxWidth = bounds.leftMaxWidth;
		if (maxWidth <= 0) {
			return y;
		}
		int height = CHIP_PAD_V * 2 + font.lineHeight;
		int width = maxWidth;
		int x = bounds.left;
		if (intersectsHotZone(x, y, x + width, y + height, graphics, bounds.scale)) {
			width = shrinkWidthToClearHotZone(x, y, height, width, graphics, bounds.scale);
		}
		int inner = Math.max(0, width - CHIP_PAD_H * 2);
		String label = "DIR";
		String gap = " ";
		int labelWidth = font.width(label);
		int gapWidth = font.width(gap);
		int remaining = inner - labelWidth - gapWidth;
		FacingText value;
		if (remaining < 0) {
			label = ellipsize(font, "DIR", inner);
			gap = "";
			value = FacingText.EMPTY;
			labelWidth = font.width(label);
			gapWidth = 0;
		} else {
			value = formatFacing(player, partialTick, font, remaining, showPitch);
		}
		int content = labelWidth + gapWidth + value.width(font);
		width = Math.min(width, CHIP_PAD_H * 2 + content);
		if (width <= 0) {
			return y;
		}
		fillChip(graphics, x, y, width, height);
		int textX = x + CHIP_PAD_H;
		int textY = y + CHIP_PAD_V;
		graphics.drawString(font, label, textX, textY, ClickGuiTheme.TEXT_MUTED, false);
		if (!value.dir.isEmpty()) {
			int cx = textX + labelWidth;
			graphics.drawString(font, gap, cx, textY, textColor, false);
			cx += gapWidth;
			graphics.drawString(font, value.dir, cx, textY, textColor, false);
			cx += font.width(value.dir);
			if (!value.yaw.isEmpty()) {
				String yawPart = "  " + value.yaw;
				graphics.drawString(font, yawPart, cx, textY, ClickGuiTheme.TEXT_MUTED, false);
				cx += font.width(yawPart);
			}
			if (!value.pitch.isEmpty()) {
				String pitchPart = "  " + value.pitch;
				graphics.drawString(font, pitchPart, cx, textY, ClickGuiTheme.TEXT_MUTED, false);
			}
		}
		return y + height + ROW_GAP;
	}

	/**
	 * Drop pitch first, then the yaw number, always keeping the compass name when
	 * any of the value still fits. Compass uses the user Color; yaw and pitch use
	 * {@link ClickGuiTheme#TEXT_MUTED}.
	 */
	private static FacingText formatFacing(LocalPlayer player, float partialTick, Font font, int maxWidth, boolean showPitch) {
		float yaw = player.getYRot(partialTick);
		String dir = compass8(yaw);
		String yawText = String.format(Locale.ROOT, "%.1f", Mth.wrapDegrees(yaw));
		String pitchText = String.format(Locale.ROOT, "%.1f", player.getXRot(partialTick));
		if (showPitch) {
			FacingText withPitch = new FacingText(dir, yawText, pitchText);
			if (withPitch.width(font) <= maxWidth) {
				return withPitch;
			}
		}
		FacingText withYaw = new FacingText(dir, yawText, "");
		if (withYaw.width(font) <= maxWidth) {
			return withYaw;
		}
		FacingText dirOnly = new FacingText(dir, "", "");
		if (dirOnly.width(font) <= maxWidth) {
			return dirOnly;
		}
		return new FacingText(ellipsize(font, dir, maxWidth), "", "");
	}

	private static String compass8(float yaw) {
		int index = Math.floorMod(Math.round(yaw / 45.0f), 8);
		return COMPASS_8[index];
	}

	private static int drawModuleList(GuiGraphics graphics, Font font, LayoutBounds bounds) {
		WorkspaceClient workspace = WorkspaceClient.getInstance();
		if (workspace == null) {
			return bounds.top;
		}
		int y = bounds.top;
		int lastBottom = bounds.top;
		int height = CHIP_PAD_V * 2 + font.lineHeight;
		for (Module module : workspace.moduleManager().enabledVisibleInHud()) {
			if (y + height > bounds.bottom) {
				break;
			}
			int maxWidth = bounds.rightMaxWidth;
			if (maxWidth <= ACCENT_BAR + CHIP_PAD_H * 2) {
				break;
			}
			if (intersectsHotZone(bounds.right - maxWidth, y, bounds.right, y + height, graphics, bounds.scale)) {
				int allowed = shrinkWidthFromRight(bounds.right, y, height, maxWidth, graphics, bounds.scale);
				if (allowed <= ACCENT_BAR + CHIP_PAD_H * 2) {
					break;
				}
				maxWidth = allowed;
			}
			String name = ellipsize(font, module.name(), Math.max(0, maxWidth - CHIP_PAD_H * 2 - ACCENT_BAR));
			int textW = font.width(name);
			int width = ACCENT_BAR + CHIP_PAD_H * 2 + textW;
			width = Math.min(width, maxWidth);
			int x = bounds.right - width;
			fillChip(graphics, x, y, width - ACCENT_BAR, height);
			graphics.fill(x + width - ACCENT_BAR, y, x + width, y + height, ClickGuiTheme.ACCENT);
			graphics.drawString(font, name, x + CHIP_PAD_H, y + CHIP_PAD_V, ClickGuiTheme.ENABLED, false);
			lastBottom = y + height;
			y += height + ROW_GAP;
		}
		return lastBottom;
	}

	private static int drawEffects(
		GuiGraphics graphics,
		Font font,
		LayoutBounds bounds,
		LocalPlayer player,
		int moduleListBottom,
		int leftCursor,
		int textColor
	) {
		List<EffectChip> chips = collectEffectChips(player, font);
		if (chips.isEmpty()) {
			return leftCursor;
		}
		int rightTop = moduleListBottom == bounds.top ? bounds.top : moduleListBottom + COLUMN_GAP;
		Rect packed = packEffects(chips, font, bounds.rightMaxWidth, EFFECTS_PER_ROW);
		int x = bounds.right - packed.width;
		int y = rightTop;
		boolean useLeft = packed.height <= 0
			|| y + packed.height > bounds.bottom
			|| x < bounds.left + bounds.leftMaxWidth + COLUMN_GAP && y < leftCursor
			|| intersectsHotZone(x, y, x + packed.width, y + packed.height, graphics, bounds.scale);
		if (!useLeft) {
			drawEffectRows(graphics, font, packed, x, y, true, textColor);
			return leftCursor;
		}
		Rect leftPacked = packEffects(chips, font, bounds.leftMaxWidth, EFFECTS_PER_ROW);
		int leftX = bounds.left;
		int leftY = leftCursor;
		if (leftPacked.height <= 0 || leftY + leftPacked.height > bounds.leftBottom) {
			return leftCursor;
		}
		if (intersectsHotZone(leftX, leftY, leftX + leftPacked.width, leftY + leftPacked.height, graphics, bounds.scale)) {
			int allowed = shrinkWidthToClearHotZone(leftX, leftY, leftPacked.height, leftPacked.width, graphics, bounds.scale);
			if (allowed < leftPacked.minChipWidth) {
				return leftCursor;
			}
			leftPacked = packEffects(chips, font, allowed, EFFECTS_PER_ROW);
			if (leftPacked.height <= 0 || leftY + leftPacked.height > bounds.leftBottom) {
				return leftCursor;
			}
		}
		drawEffectRows(graphics, font, leftPacked, leftX, leftY, false, textColor);
		return leftY + leftPacked.height + ROW_GAP;
	}

	private static List<EffectChip> collectEffectChips(LocalPlayer player, Font font) {
		List<MobEffectInstance> effects = new ArrayList<>();
		for (MobEffectInstance instance : player.getActiveEffects()) {
			if (instance != null && instance.showIcon()) {
				effects.add(instance);
			}
		}
		effects.sort(
			Comparator
				.<MobEffectInstance>comparingInt(HudRenderer::effectCategoryRank)
				.thenComparingInt(instance -> instance.isInfiniteDuration() ? Integer.MAX_VALUE : instance.getDuration())
		);
		List<EffectChip> chips = new ArrayList<>(effects.size());
		for (MobEffectInstance instance : effects) {
			Holder<MobEffect> holder = instance.getEffect();
			String time = formatEffectTime(instance);
			int level = instance.getAmplifier() + 1;
			String roman = level > 1 ? romanNumeral(level) : "";
			int inner = Math.max(EFFECT_ICON, font.width(time));
			if (!roman.isEmpty()) {
				inner = Math.max(inner, EFFECT_ICON);
			}
			int width = CHIP_PAD_H * 2 + inner;
			int height = CHIP_PAD_V + EFFECT_ICON + 1 + font.lineHeight + CHIP_PAD_V;
			chips.add(new EffectChip(holder, time, roman, width, height));
		}
		return chips;
	}

	private static int effectCategoryRank(MobEffectInstance instance) {
		MobEffect effect = instance.getEffect().value();
		if (effect.isBeneficial()) {
			return 0;
		}
		if (effect.getCategory() == MobEffectCategory.HARMFUL) {
			return 2;
		}
		return 1;
	}

	private static String formatEffectTime(MobEffectInstance instance) {
		if (instance.isInfiniteDuration()) {
			return "∞";
		}
		int seconds = Math.max(0, instance.getDuration() / 20);
		int minutes = seconds / 60;
		int rem = seconds % 60;
		return minutes + ":" + (rem < 10 ? "0" : "") + rem;
	}

	private static String romanNumeral(int level) {
		if (level >= 1 && level < ROMAN.length) {
			return ROMAN[level];
		}
		return String.valueOf(level);
	}

	private static Rect packEffects(List<EffectChip> chips, Font font, int maxWidth, int preferredPerRow) {
		if (chips.isEmpty() || maxWidth <= 0) {
			return Rect.empty();
		}
		int minChip = Integer.MAX_VALUE;
		for (EffectChip chip : chips) {
			minChip = Math.min(minChip, chip.width);
		}
		if (minChip > maxWidth) {
			return Rect.empty();
		}
		List<Integer> rowCounts = new ArrayList<>();
		List<Integer> rowWidths = new ArrayList<>();
		List<Integer> rowHeights = new ArrayList<>();
		List<EffectChip> placed = new ArrayList<>();
		int width = 0;
		int height = 0;
		int i = 0;
		while (i < chips.size()) {
			if (chips.get(i).width > maxWidth) {
				// A single chip that cannot fit is skipped; remaining chips still pack.
				i++;
				continue;
			}
			int count = 1;
			int rw = chips.get(i).width;
			int rh = chips.get(i).height;
			while (count < preferredPerRow && i + count < chips.size()) {
				EffectChip next = chips.get(i + count);
				int candidate = rw + EFFECT_GAP + next.width;
				if (candidate > maxWidth) {
					break;
				}
				rw = candidate;
				rh = Math.max(rh, next.height);
				count++;
			}
			for (int j = 0; j < count; j++) {
				placed.add(chips.get(i + j));
			}
			rowCounts.add(count);
			rowWidths.add(rw);
			rowHeights.add(rh);
			width = Math.max(width, rw);
			if (height > 0) {
				height += EFFECT_GAP;
			}
			height += rh;
			i += count;
		}
		if (rowCounts.isEmpty()) {
			return Rect.empty();
		}
		return new Rect(width, height, minChip, rowCounts, rowWidths, rowHeights, placed);
	}

	private static void drawEffectRows(
		GuiGraphics graphics,
		Font font,
		Rect packed,
		int anchorX,
		int top,
		boolean rightAlign,
		int textColor
	) {
		int index = 0;
		int y = top;
		RenderPipeline pipeline = RenderPipelines.GUI_TEXTURED;
		List<EffectChip> chips = packed.chips;
		for (int row = 0; row < packed.rowHeights.size(); row++) {
			int rowWidth = packed.rowWidths.get(row);
			int rowHeight = packed.rowHeights.get(row);
			int x = rightAlign ? anchorX + packed.width - rowWidth : anchorX;
			int drawn = 0;
			int perRow = packed.rowCounts.get(row);
			while (drawn < perRow && index < chips.size()) {
				EffectChip chip = chips.get(index);
				fillChip(graphics, x, y, chip.width, chip.height);
				int iconX = x + (chip.width - EFFECT_ICON) / 2;
				int iconY = y + CHIP_PAD_V;
				Identifier sprite = Gui.getMobEffectSprite(chip.holder);
				graphics.blitSprite(pipeline, sprite, iconX, iconY, EFFECT_ICON, EFFECT_ICON);
				if (!chip.roman.isEmpty()) {
					int badgeX = x + chip.width - CHIP_PAD_H - font.width(chip.roman);
					graphics.drawString(font, chip.roman, badgeX, iconY - 1, textColor, false);
				}
				int timeW = font.width(chip.time);
				int timeX = x + (chip.width - timeW) / 2;
				int timeY = iconY + EFFECT_ICON + 1;
				graphics.drawString(font, chip.time, timeX, timeY, ClickGuiTheme.TEXT_MUTED, false);
				x += chip.width + EFFECT_GAP;
				index++;
				drawn++;
			}
			y += rowHeight + EFFECT_GAP;
		}
	}

	private static void drawArmor(
		GuiGraphics graphics,
		Font font,
		LayoutBounds bounds,
		HudSafeInsets insets,
		LocalPlayer player,
		int leftCursor,
		boolean showPercent
	) {
		List<ArmorPiece> pieces = new ArrayList<>(4);
		for (EquipmentSlot slot : ARMOR_SLOTS) {
			ItemStack stack = player.getItemBySlot(slot);
			if (stack != null && !stack.isEmpty()) {
				pieces.add(new ArmorPiece(stack));
			}
		}
		if (pieces.isEmpty()) {
			return;
		}
		int colW = 0;
		int colH = 0;
		int percentCol = showPercent ? font.width("100%") : 0;
		for (int i = 0; i < pieces.size(); i++) {
			ArmorPiece piece = pieces.get(i);
			int rowH = ARMOR_ICON + (piece.showBar ? ARMOR_BAR : 0);
			int rowW = ARMOR_ICON;
			if (showPercent && piece.damageable) {
				rowW += ARMOR_PERCENT_GAP + Math.max(percentCol, font.width(piece.percentText()));
			}
			colW = Math.max(colW, rowW);
			if (i > 0) {
				colH += ARMOR_ITEM_GAP;
			}
			colH += rowH;
		}
		if (colW <= 0 || colH <= 0) {
			return;
		}
		int vanillaGap = LayoutBounds.scaledCeil(HudSafeInsets.STATUS_CLUSTER_GAP, bounds.scale);
		int statusTop = LayoutBounds.scaledFloor(insets.statusTop, bounds.scale);
		int statusLeft = LayoutBounds.scaledFloor(insets.statusLeft, bounds.scale);
		int maxBottom = Math.min(bounds.leftBottom, statusTop - vanillaGap);
		int x = bounds.left;
		if (x + colW + vanillaGap > statusLeft) {
			maxBottom = Math.min(maxBottom, statusTop - vanillaGap);
		}
		int y = maxBottom - colH;
		if (y < leftCursor) {
			return;
		}
		if (intersectsHotZone(x, y, x + colW, y + colH, graphics, bounds.scale)) {
			int lo = leftCursor;
			int hi = y;
			int best = -1;
			while (lo <= hi) {
				int mid = (lo + hi) >>> 1;
				if (!intersectsHotZone(x, mid, x + colW, mid + colH, graphics, bounds.scale) && mid + colH <= maxBottom) {
					best = mid;
					lo = mid + 1;
				} else {
					hi = mid - 1;
				}
			}
			if (best < 0) {
				return;
			}
			y = best;
		}
		int cursor = y;
		for (ArmorPiece piece : pieces) {
			int iconX = x;
			int iconY = cursor;
			graphics.renderItem(piece.stack, iconX, iconY);
			if (piece.showBar) {
				int barY = iconY + ARMOR_ICON;
				graphics.fill(iconX, barY, iconX + ARMOR_ICON, barY + ARMOR_BAR, ClickGuiTheme.SLIDER_TRACK);
				int fill = Math.max(0, Math.min(ARMOR_ICON, Math.round(piece.remaining() * ARMOR_ICON)));
				if (fill > 0) {
					graphics.fill(iconX, barY, iconX + fill, barY + ARMOR_BAR, piece.barColor());
				}
			}
			if (showPercent) {
				String text = piece.percentText();
				int textX = iconX + ARMOR_ICON + ARMOR_PERCENT_GAP;
				int textY = iconY + (ARMOR_ICON - font.lineHeight) / 2;
				graphics.drawString(font, text, textX, textY, ClickGuiTheme.TEXT_MUTED, false);
			}
			cursor += ARMOR_ICON + (piece.showBar ? ARMOR_BAR : 0) + ARMOR_ITEM_GAP;
		}
	}

	private static void fillChip(GuiGraphics graphics, int x, int y, int width, int height) {
		if (width <= 0 || height <= 0) {
			return;
		}
		int x1 = x + width;
		int y1 = y + height;
		graphics.fill(x, y, x1, y1, CHIP_BACKGROUND);
		graphics.fill(x, y, x1, y + 1, ClickGuiTheme.BORDER);
		graphics.fill(x, y1 - 1, x1, y1, ClickGuiTheme.BORDER);
		graphics.fill(x, y, x + 1, y1, ClickGuiTheme.BORDER);
		graphics.fill(x1 - 1, y, x1, y1, ClickGuiTheme.BORDER);
	}

	private static String ellipsize(Font font, String text, int maxWidth) {
		if (text == null || maxWidth <= 0) {
			return "";
		}
		if (font.width(text) <= maxWidth) {
			return text;
		}
		int ellipsisWidth = font.width(ELLIPSIS);
		if (ellipsisWidth > maxWidth) {
			return font.plainSubstrByWidth(text, maxWidth);
		}
		return font.plainSubstrByWidth(text, maxWidth - ellipsisWidth) + ELLIPSIS;
	}

	private static boolean intersectsHotZone(int x0, int y0, int x1, int y1, GuiGraphics graphics, float scale) {
		return HotZone.of(graphics, scale).intersects(x0, y0, x1, y1);
	}

	private static int shrinkWidthToClearHotZone(int x, int y, int height, int width, GuiGraphics graphics, float scale) {
		HotZone zone = HotZone.of(graphics, scale);
		int lo = 0;
		int hi = width;
		int best = 0;
		while (lo <= hi) {
			int mid = (lo + hi) >>> 1;
			if (!zone.intersects(x, y, x + mid, y + height)) {
				best = mid;
				lo = mid + 1;
			} else {
				hi = mid - 1;
			}
		}
		return best;
	}

	private static int shrinkWidthFromRight(int right, int y, int height, int width, GuiGraphics graphics, float scale) {
		HotZone zone = HotZone.of(graphics, scale);
		int lo = 0;
		int hi = width;
		int best = 0;
		while (lo <= hi) {
			int mid = (lo + hi) >>> 1;
			if (!zone.intersects(right - mid, y, right, y + height)) {
				best = mid;
				lo = mid + 1;
			} else {
				hi = mid - 1;
			}
		}
		return best;
	}

	private record HotZone(double cx, double cy, double radius) {
		static HotZone of(GuiGraphics graphics, float scale) {
			double safeScale = Math.max(0.01f, scale);
			return new HotZone(
				graphics.guiWidth() * 0.5 / safeScale,
				graphics.guiHeight() * 0.5 / safeScale,
				CROSSHAIR_RESERVE / safeScale
			);
		}

		boolean intersects(int x0, int y0, int x1, int y1) {
			double closestX = Math.min(Math.max(this.cx, x0), x1);
			double closestY = Math.min(Math.max(this.cy, y0), y1);
			double dx = this.cx - closestX;
			double dy = this.cy - closestY;
			return dx * dx + dy * dy < this.radius * this.radius;
		}
	}

	private record EffectChip(Holder<MobEffect> holder, String time, String roman, int width, int height) {
	}

	private static final class ArmorPiece {
		final ItemStack stack;
		final boolean damageable;
		/** True only when the item can take damage and already has some. Full-durability pieces show no bar. */
		final boolean showBar;
		final int remaining;
		final int max;

		ArmorPiece(ItemStack stack) {
			this.stack = stack;
			this.max = stack.getMaxDamage();
			this.damageable = stack.isDamageableItem() && this.max > 0;
			int damage = stack.getDamageValue();
			this.remaining = this.damageable ? Math.max(0, this.max - damage) : 0;
			this.showBar = this.damageable && damage > 0;
		}

		float remaining() {
			if (!this.damageable || this.max <= 0) {
				return 1.0f;
			}
			return this.remaining / (float) this.max;
		}

		String percentText() {
			if (!this.damageable) {
				return "";
			}
			return Math.round(remaining() * 100.0f) + "%";
		}

		int barColor() {
			float remain = remaining();
			if (remain > 0.5f) {
				return ClickGuiTheme.ACCENT;
			}
			if (remain >= 0.25f) {
				return ARMOR_MID;
			}
			return ARMOR_LOW;
		}
	}

	private record FacingText(String dir, String yaw, String pitch) {
		static final FacingText EMPTY = new FacingText("", "", "");

		int width(Font font) {
			if (this.dir.isEmpty()) {
				return 0;
			}
			int width = font.width(this.dir);
			if (!this.yaw.isEmpty()) {
				width += font.width("  " + this.yaw);
			}
			if (!this.pitch.isEmpty()) {
				width += font.width("  " + this.pitch);
			}
			return width;
		}
	}

	private record Rect(
		int width,
		int height,
		int minChipWidth,
		List<Integer> rowCounts,
		List<Integer> rowWidths,
		List<Integer> rowHeights,
		List<EffectChip> chips
	) {
		static Rect empty() {
			return new Rect(0, 0, 0, List.of(), List.of(), List.of(), List.of());
		}
	}

	private static final class LayoutBounds {
		final int left;
		final int right;
		final int top;
		final int bottom;
		final int leftBottom;
		final int leftMaxWidth;
		final int rightMaxWidth;
		final float scale;

		private LayoutBounds(
			int left,
			int right,
			int top,
			int bottom,
			int leftBottom,
			int leftMaxWidth,
			int rightMaxWidth,
			float scale
		) {
			this.left = left;
			this.right = right;
			this.top = top;
			this.bottom = bottom;
			this.leftBottom = leftBottom;
			this.leftMaxWidth = leftMaxWidth;
			this.rightMaxWidth = rightMaxWidth;
			this.scale = scale;
		}

		static LayoutBounds from(int spaceW, int spaceH, float scale, HudSafeInsets insets) {
			int left = SAFE_INSET;
			int right = spaceW - SAFE_INSET;
			int top = SAFE_INSET;
			int bottom = spaceH - SAFE_INSET;

			int sidebarLeft = scaledFloor(insets.sidebarLeft, scale);
			if (sidebarLeft < spaceW) {
				right = Math.min(right, sidebarLeft - 2);
			}

			int hotbarTop = scaledFloor(insets.hotbarTop, scale);
			bottom = Math.min(bottom, hotbarTop);

			int chatTop = scaledFloor(insets.chatTop, scale);
			int chatRight = Math.min(spaceW, scaledCeil(insets.chatWidth, scale));
			int leftBottom = bottom;
			if (chatRight > left) {
				leftBottom = Math.min(leftBottom, chatTop);
			}

			if (insets.hasBossEvents()) {
				int bossLeft = scaledFloor(insets.bossLeft, scale);
				int bossRight = scaledCeil(insets.bossRight, scale);
				if (left < bossRight && right > bossLeft) {
					top = Math.max(top, scaledCeil(insets.bossBottom, scale) + ROW_GAP);
				}
			}

			if (right <= left) {
				int mid = Math.max(left, spaceW / 2);
				left = Math.min(left, mid);
				right = Math.max(left + 1, mid);
			}

			int available = Math.max(0, right - left);
			int leftMax = Math.max(0, (available - COLUMN_GAP) / 2);
			int rightMax = Math.max(0, available - COLUMN_GAP - leftMax);
			bottom = Math.max(top, bottom);
			leftBottom = Math.max(top, leftBottom);
			return new LayoutBounds(left, right, top, bottom, leftBottom, leftMax, rightMax, scale);
		}

		private static int scaledFloor(int unscaled, float scale) {
			return (int) Math.floor(unscaled / Math.max(0.01f, scale));
		}

		private static int scaledCeil(int unscaled, float scale) {
			return (int) Math.ceil(unscaled / Math.max(0.01f, scale));
		}
	}
}
