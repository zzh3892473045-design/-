package dev.workspace.client.hud;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.Scoreboard;

import dev.workspace.client.mixin.BossHealthOverlayAccessor;

/**
 * Vanilla HUD reservations in unscaled GUI pixels. Used only to dodge boss bars,
 * the sidebar, the hotbar, and chat — not to hide the custom HUD layer.
 */
final class HudSafeInsets {
	private static final int BOSS_BAR_WIDTH = 182;
	private static final int BOSS_START_Y = 12;
	private static final int BOSS_STEP_EXTRA = 10;
	private static final int SIDEBAR_EDGE = 3;
	private static final int SIDEBAR_MAX_ENTRIES = 15;
	private static final int HOTBAR_HEIGHT = 22;
	private static final int HOTBAR_DECORATION = 28;
	private static final int HOTBAR_HALF = 91;
	private static final int OFFHAND_WIDTH = 29;
	private static final int OFFHAND_TOP_OFFSET = 23;
	private static final int HEALTH_BASE_OFFSET = 39;
	private static final int STATUS_ROW = 10;
	/** Unscaled gap kept between the armor cluster and vanilla status widgets. */
	static final int STATUS_CLUSTER_GAP = 4;

	final int bossLeft;
	final int bossRight;
	final int bossBottom;
	final int sidebarLeft;
	final int chatWidth;
	final int chatTop;
	final int hotbarTop;
	/** Left edge of the vanilla hotbar / offhand / health cluster (unscaled). Conservative; both offhand sides reserved. */
	final int statusLeft;
	/** Top edge of the vanilla hotbar / XP / health / air / vehicle cluster (unscaled). Conservative, unverified in-game. */
	final int statusTop;

	private HudSafeInsets(
		int bossLeft,
		int bossRight,
		int bossBottom,
		int sidebarLeft,
		int chatWidth,
		int chatTop,
		int hotbarTop,
		int statusLeft,
		int statusTop
	) {
		this.bossLeft = bossLeft;
		this.bossRight = bossRight;
		this.bossBottom = bossBottom;
		this.sidebarLeft = sidebarLeft;
		this.chatWidth = chatWidth;
		this.chatTop = chatTop;
		this.hotbarTop = hotbarTop;
		this.statusLeft = statusLeft;
		this.statusTop = statusTop;
	}

	static HudSafeInsets measure(Minecraft client, Font font, int guiWidth, int guiHeight) {
		int bossCount = bossEventCount(client);
		int bossLeft = guiWidth;
		int bossRight = 0;
		int bossBottom = 0;
		if (bossCount > 0) {
			int maxVisible = 0;
			int y = BOSS_START_Y;
			int limit = guiHeight / 3;
			while (maxVisible < bossCount && y < limit) {
				maxVisible++;
				y += BOSS_STEP_EXTRA + font.lineHeight;
			}
			if (maxVisible > 0) {
				// Name is drawn 9px above the bar start (vanilla uses 9, which is the default line height).
				int nameTop = BOSS_START_Y - font.lineHeight;
				bossBottom = Math.max(0, nameTop) + maxVisible * (BOSS_STEP_EXTRA + font.lineHeight);
				bossLeft = guiWidth / 2 - BOSS_BAR_WIDTH / 2;
				bossRight = bossLeft + BOSS_BAR_WIDTH;
			}
		}

		int sidebarLeft = guiWidth;
		if (client.level != null) {
			Scoreboard scoreboard = client.level.getScoreboard();
			Objective sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
			if (sidebar != null) {
				int width = font.width(sidebar.getDisplayName());
				int colonWidth = font.width(":");
				int shown = 0;
				Collection<PlayerScoreEntry> entries = scoreboard.listPlayerScores(sidebar);
				for (PlayerScoreEntry entry : entries) {
					if (entry.isHidden()) {
						continue;
					}
					if (shown >= SIDEBAR_MAX_ENTRIES) {
						break;
					}
					shown++;
					Component name = entry.ownerName();
					int scoreWidth = font.width(String.valueOf(entry.value()));
					int rowWidth = font.width(name) + (scoreWidth > 0 ? colonWidth + scoreWidth : 0);
					width = Math.max(width, rowWidth);
				}
				sidebarLeft = guiWidth - width - SIDEBAR_EDGE;
			}
		}

		boolean chatFocused = client.gui != null && client.gui.getChat() != null && client.gui.getChat().isChatFocused();
		double heightOpt = chatFocused
			? client.options.chatHeightFocused().get()
			: client.options.chatHeightUnfocused().get();
		int chatHeight = ChatComponent.getHeight(heightOpt);
		int chatWidth = ChatComponent.getWidth(client.options.chatWidth().get());
		int chatTop = guiHeight - chatHeight;

		int hotbarTop = guiHeight - HOTBAR_HEIGHT - HOTBAR_DECORATION;
		int statusLeft = guiWidth / 2 - HOTBAR_HALF - OFFHAND_WIDTH - STATUS_CLUSTER_GAP;
		int statusTop = measureStatusTop(client, guiHeight);

		return new HudSafeInsets(bossLeft, bossRight, bossBottom, sidebarLeft, chatWidth, chatTop, hotbarTop, statusLeft, statusTop);
	}

	/**
	 * Conservative unscaled top of the vanilla bottom-center status cluster.
	 * Heart / armor / air / vehicle row counts follow 1.21.11 {@code Gui} offsets
	 * ({@code guiHeight - 39}, 10px rows, offhand at {@code guiHeight - 23}) and
	 * still need in-game confirmation.
	 */
	private static int measureStatusTop(Minecraft client, int guiHeight) {
		int top = guiHeight - OFFHAND_TOP_OFFSET;
		LocalPlayer player = client.player;
		if (player == null || player.isCreative() || player.isSpectator()) {
			return top;
		}
		int rows = heartRows(player.getMaxHealth() + player.getAbsorptionAmount());
		if (player.getArmorValue() > 0) {
			rows++;
		}
		if (player.getAirSupply() < player.getMaxAirSupply()) {
			rows++;
		}
		Entity vehicle = player.getVehicle();
		if (vehicle instanceof LivingEntity living && living.getMaxHealth() > 0.0f) {
			rows += heartRows(living.getMaxHealth());
		}
		int healthTop = guiHeight - HEALTH_BASE_OFFSET - STATUS_ROW * Math.max(0, rows - 1);
		return Math.min(top, healthTop);
	}

	private static int heartRows(float hearts) {
		if (hearts <= 0.0f) {
			return 1;
		}
		return Math.max(1, (int) Math.ceil(hearts / 20.0f));
	}


	boolean hasBossEvents() {
		return this.bossBottom > 0;
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

}
