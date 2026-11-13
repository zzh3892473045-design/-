package dev.workspace.client.hud;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;

import dev.workspace.client.WorkspaceClient;
import dev.workspace.client.event.HudRenderEvent;
import dev.workspace.client.gui.ClickGuiScreen;
import dev.workspace.client.gui.ClickGuiTheme;
import dev.workspace.client.mixin.BossHealthOverlayAccessor;
import dev.workspace.client.module.Module;
import dev.workspace.client.module.impl.HudModule;
import dev.workspace.client.module.impl.NotificationsModule;

/**
 * Draws the Notifications chip queue. Lives in this package so it can sit
 * beside {@link HudRenderer} without editing that class, {@link HudModule},
 * or {@link HudSafeInsets}. Boss-bar height is measured locally through
 * {@link BossHealthOverlayAccessor}; the module-list reserve is estimated
 * from public {@code ModuleManager} / {@code HudModule} APIs.
 */
public final class NotificationRenderer {
	private static final int SAFE_INSET = 6;
	private static final int CHIP_PAD_H = 3;
	private static final int CHIP_PAD_V = 2;
	private static final int ROW_GAP = 3;
	private static final int CLUSTER_GAP = 6;
	private static final int ACCENT_BAR = 2;
	private static final int MAX_VISIBLE = 3;
	private static final float FADE_SECONDS = 0.4f;
	private static final int HOTBAR_HEIGHT = 22;
	private static final int HOTBAR_DECORATION = 28;
	private static final int BOSS_BAR_WIDTH = 182;
	private static final int BOSS_START_Y = 12;
	private static final int BOSS_STEP_EXTRA = 10;
	private static final int CLICK_GUI_TITLE_X = 20;
	private static final int CLICK_GUI_TITLE_Y = 10;
	private static final String ELLIPSIS = "...";
	private static final int CHIP_BACKGROUND = 0x99000000 | (ClickGuiTheme.PANEL & 0x00FFFFFF);
	private static final String LANG_ON = "workspace-client.notifications.on";
	private static final String LANG_OFF = "workspace-client.notifications.off";

	private final ArrayDeque<Entry> entries = new ArrayDeque<>();

	public void enqueue(String moduleName, boolean enabled, double durationSeconds) {
		if (moduleName == null || moduleName.isEmpty()) {
			return;
		}
		double seconds = Math.max(0.1, durationSeconds);
		long now = System.nanoTime();
		this.entries.addFirst(new Entry(moduleName, enabled, now, (long) (seconds * 1_000_000_000.0)));
		while (this.entries.size() > MAX_VISIBLE) {
			this.entries.removeLast();
		}
	}

	public void clear() {
		this.entries.clear();
	}

	public void render(HudRenderEvent event, NotificationsModule.Position position) {
		long now = System.nanoTime();
		expire(now);
		if (this.entries.isEmpty()) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.options == null) {
			return;
		}
		if (client.options.hideGui) {
			return;
		}
		if (client.getDebugOverlay() != null && client.getDebugOverlay().showDebugScreen()) {
			return;
		}
		GuiGraphics graphics = event.graphics();
		Font font = client.font;
		int guiWidth = graphics.guiWidth();
		int guiHeight = graphics.guiHeight();
		int chipH = CHIP_PAD_V * 2 + font.lineHeight;
		NotificationsModule.Position anchor = position == null
			? NotificationsModule.Position.TOP_CENTER
			: position;

		List<Chip> chips = layoutChips(font, guiWidth, this.entries, now);
		if (chips.isEmpty()) {
			return;
		}
		int stackTop = stackTop(client, font, guiWidth, guiHeight, anchor, chips, chipH);
		int y = stackTop;
		if (anchor == NotificationsModule.Position.BOTTOM_RIGHT) {
			int total = chips.size() * chipH + (chips.size() - 1) * ROW_GAP;
			y = stackTop - total;
		}
		for (Chip chip : chips) {
			drawChip(graphics, font, chip, chipX(anchor, guiWidth, chip.width), y);
			y += chipH + ROW_GAP;
		}
	}

	private void expire(long now) {
		this.entries.removeIf(entry -> now - entry.startNanos >= entry.durationNanos);
	}

	private static List<Chip> layoutChips(Font font, int guiWidth, ArrayDeque<Entry> entries, long now) {
		int maxWidth = Math.max(0, guiWidth - SAFE_INSET * 2);
		List<Chip> chips = new ArrayList<>();
		for (Entry entry : entries) {
			float alpha = fadeAlpha(entry, now);
			if (Math.round(alpha * 255.0f) < 4) {
				continue;
			}
			Component status = Component.translatable(entry.enabled ? LANG_ON : LANG_OFF);
			int statusW = font.width(status);
			int gapW = font.width(" ");
			int innerMax = maxWidth - ACCENT_BAR - CHIP_PAD_H * 2 - gapW - statusW;
			String name = ellipsize(font, entry.moduleName, Math.max(0, innerMax));
			int width = ACCENT_BAR + CHIP_PAD_H * 2 + font.width(name) + gapW + statusW;
			width = Math.min(width, maxWidth);
			if (width <= 0) {
				continue;
			}
			chips.add(new Chip(name, status, entry.enabled, width, alpha));
		}
		return chips;
	}

	private static int stackTop(
		Minecraft client,
		Font font,
		int guiWidth,
		int guiHeight,
		NotificationsModule.Position anchor,
		List<Chip> chips,
		int chipH
	) {
		int top = SAFE_INSET;
		int bottomLimit = guiHeight - SAFE_INSET;
		int hotbarTop = guiHeight - HOTBAR_HEIGHT - HOTBAR_DECORATION;
		bottomLimit = Math.min(bottomLimit, hotbarTop);

		if (anchor == NotificationsModule.Position.TOP_RIGHT) {
			int listBottom = estimateModuleListBottom(font);
			if (listBottom > 0) {
				top = Math.max(top, listBottom + CLUSTER_GAP);
			}
		}

		int widest = 0;
		for (Chip chip : chips) {
			widest = Math.max(widest, chip.width);
		}
		int probeX = chipX(anchor, guiWidth, widest);
		int probeW = widest;

		int bossCount = bossEventCount(client);
		if (bossCount > 0 && (anchor == NotificationsModule.Position.TOP_CENTER || anchor == NotificationsModule.Position.TOP_RIGHT)) {
			int bossLeft = guiWidth / 2 - BOSS_BAR_WIDTH / 2;
			int bossRight = bossLeft + BOSS_BAR_WIDTH;
			int bossBottom = measureBossBottom(client, font, guiHeight, bossCount);
			if (bossBottom > 0 && overlapsHorizontally(probeX, probeX + probeW, bossLeft, bossRight)) {
				top = Math.max(top, bossBottom + ROW_GAP);
			}
		}

		if (client.screen instanceof ClickGuiScreen) {
			int titleW = font.width(WorkspaceClient.NAME);
			int titleRight = CLICK_GUI_TITLE_X + titleW;
			int titleBottom = CLICK_GUI_TITLE_Y + font.lineHeight;
			int firstBottom = (anchor == NotificationsModule.Position.BOTTOM_RIGHT)
				? bottomLimit
				: top + chipH;
			int firstTop = (anchor == NotificationsModule.Position.BOTTOM_RIGHT)
				? firstBottom - (chips.size() * chipH + (chips.size() - 1) * ROW_GAP)
				: top;
			if (overlaps(probeX, firstTop, probeX + probeW, firstBottom, CLICK_GUI_TITLE_X, CLICK_GUI_TITLE_Y, titleRight, titleBottom)) {
				top = Math.max(top, titleBottom + ROW_GAP);
			}
		}

		if (anchor == NotificationsModule.Position.BOTTOM_RIGHT) {
			return bottomLimit;
		}
		return top;
	}

	private static int chipX(NotificationsModule.Position anchor, int guiWidth, int width) {
		int x;
		if (anchor == NotificationsModule.Position.TOP_RIGHT || anchor == NotificationsModule.Position.BOTTOM_RIGHT) {
			x = guiWidth - SAFE_INSET - width;
		} else {
			x = (guiWidth - width) / 2;
		}
		int min = SAFE_INSET;
		int max = guiWidth - SAFE_INSET - width;
		if (max < min) {
			return Math.max(0, min);
		}
		return Math.max(min, Math.min(x, max));
	}

	private static void drawChip(GuiGraphics graphics, Font font, Chip chip, int x, int y) {
		int height = CHIP_PAD_V * 2 + font.lineHeight;
		int width = chip.width;
		fillChip(graphics, x, y, width, height, chip.alpha);
		int bar = applyAlpha(chip.enabled ? ClickGuiTheme.ACCENT : ClickGuiTheme.BORDER, chip.alpha);
		graphics.fill(x, y, x + ACCENT_BAR, y + height, bar);
		int textX = x + ACCENT_BAR + CHIP_PAD_H;
		int textY = y + CHIP_PAD_V;
		int nameColor = applyAlpha(chip.enabled ? ClickGuiTheme.ENABLED : ClickGuiTheme.TEXT, chip.alpha);
		int statusColor = applyAlpha(chip.enabled ? ClickGuiTheme.ENABLED : ClickGuiTheme.DISABLED, chip.alpha);
		graphics.drawString(font, chip.name, textX, textY, nameColor, false);
		int statusX = textX + font.width(chip.name) + font.width(" ");
		graphics.drawString(font, chip.status, statusX, textY, statusColor, false);
	}

	private static void fillChip(GuiGraphics graphics, int x, int y, int width, int height, float alpha) {
		if (width <= 0 || height <= 0) {
			return;
		}
		int bg = applyAlpha(CHIP_BACKGROUND, alpha);
		int border = applyAlpha(ClickGuiTheme.BORDER, alpha);
		int x1 = x + width;
		int y1 = y + height;
		graphics.fill(x, y, x1, y1, bg);
		graphics.fill(x, y, x1, y + 1, border);
		graphics.fill(x, y1 - 1, x1, y1, border);
		graphics.fill(x, y, x + 1, y1, border);
		graphics.fill(x1 - 1, y, x1, y1, border);
	}

	private static int estimateModuleListBottom(Font font) {
		WorkspaceClient workspace = WorkspaceClient.getInstance();
		if (workspace == null) {
			return 0;
		}
		HudModule hud = workspace.moduleManager().of(HudModule.class);
		if (hud == null || !hud.isEnabled() || !hud.showModuleList()) {
			return 0;
		}
		List<Module> modules = workspace.moduleManager().enabledVisibleInHud();
		if (modules.isEmpty()) {
			return 0;
		}
		float scale = Math.max(0.01f, hud.scale());
		int chipH = CHIP_PAD_V * 2 + font.lineHeight;
		int listH = modules.size() * chipH + (modules.size() - 1) * ROW_GAP;
		return (int) Math.ceil((SAFE_INSET + listH) * scale);
	}

	private static int measureBossBottom(Minecraft client, Font font, int guiHeight, int bossCount) {
		int maxVisible = 0;
		int y = BOSS_START_Y;
		int limit = guiHeight / 3;
		while (maxVisible < bossCount && y < limit) {
			maxVisible++;
			y += BOSS_STEP_EXTRA + font.lineHeight;
		}
		if (maxVisible <= 0) {
			return 0;
		}
		int nameTop = BOSS_START_Y - font.lineHeight;
		return Math.max(0, nameTop) + maxVisible * (BOSS_STEP_EXTRA + font.lineHeight);
	}

	private static int bossEventCount(Minecraft client) {
		if (client.gui == null) {
			return 0;
		}
		BossHealthOverlay overlay = client.gui.getBossOverlay();
		if (overlay == null) {
			return 0;
		}
		Map<UUID, LerpingBossEvent> events = ((BossHealthOverlayAccessor) (Object) overlay).workspaceClient$getEvents();
		return events == null ? 0 : events.size();
	}

	private static float fadeAlpha(Entry entry, long now) {
		long remaining = entry.durationNanos - (now - entry.startNanos);
		long fadeNanos = (long) (FADE_SECONDS * 1_000_000_000.0f);
		if (remaining >= fadeNanos) {
			return 1.0f;
		}
		if (remaining <= 0L) {
			return 0.0f;
		}
		return remaining / (float) fadeNanos;
	}

	private static int applyAlpha(int argb, float alpha) {
		if (alpha >= 1.0f) {
			return argb;
		}
		if (alpha <= 0.0f) {
			return argb & 0x00FFFFFF;
		}
		int a = Math.round(((argb >>> 24) & 0xFF) * alpha);
		return (a << 24) | (argb & 0x00FFFFFF);
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

	private static boolean overlapsHorizontally(int a0, int a1, int b0, int b1) {
		return a0 < b1 && a1 > b0;
	}

	private static boolean overlaps(int a0, int ay0, int a1, int ay1, int b0, int by0, int b1, int by1) {
		return a0 < b1 && a1 > b0 && ay0 < by1 && ay1 > by0;
	}

	private static final class Entry {
		final String moduleName;
		final boolean enabled;
		final long startNanos;
		final long durationNanos;

		Entry(String moduleName, boolean enabled, long startNanos, long durationNanos) {
			this.moduleName = moduleName;
			this.enabled = enabled;
			this.startNanos = startNanos;
			this.durationNanos = durationNanos;
		}
	}

	private static final class Chip {
		final String name;
		final Component status;
		final boolean enabled;
		final int width;
		final float alpha;

		Chip(String name, Component status, boolean enabled, int width, float alpha) {
			this.name = name;
			this.status = status;
			this.enabled = enabled;
			this.width = width;
			this.alpha = alpha;
		}
	}
}
