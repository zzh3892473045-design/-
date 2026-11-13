package dev.workspace.client.adapter;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.food.FoodData;

import dev.workspace.client.WorkspaceClient;
import dev.workspace.client.gui.ClickGuiScreen;
import dev.workspace.client.module.impl.ToggleSneakModule;

/**
 * Isolates Minecraft / Fabric calls that sit next to official 1.21.11 names.
 * Names used here were checked against mappings.dev and Fabric API 0.141.6
 * documentation. If a call later fails to compile, this is the file to adjust.
 *
 * Uncertainties (see docs/handoff.md):
 * <ul>
 *   <li>{@link Screen#getFocused()} is the official 1.21.11 name.</li>
 *   <li>{@link AbstractSignEditScreen} and {@link BookEditScreen} packages are
 *       the long-standing official names; they were not re-opened as source.</li>
 *   <li>{@link LocalPlayer} food / crouch / abilities methods below are the
 *       official names that have been stable across recent 1.21 releases.</li>
 *   <li>Keyboard state uses {@link Minecraft#getWindow()} plus
 *       {@link InputConstants#isKeyDown(com.mojang.blaze3d.platform.Window, int)}
 *       (official 1.21.11; the old {@code isKeyDown(long, int)} overload is gone).</li>
 * </ul>
 */
public final class ClientAdapters {
	private ClientAdapters() {
	}

	public static boolean isTextInputFocused(Minecraft client) {
		if (client == null) {
			return false;
		}
		Screen screen = client.screen;
		if (screen == null || screen instanceof ClickGuiScreen) {
			return false;
		}
		if (screen instanceof ChatScreen
			|| screen instanceof AbstractSignEditScreen
			|| screen instanceof BookEditScreen) {
			return true;
		}
		GuiEventListener focused = screen.getFocused();
		return focused instanceof EditBox;
	}

	public static boolean isKeyDown(int glfwKey) {
		if (glfwKey <= 0) {
			return false;
		}
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return false;
		}
		var window = client.getWindow();
		if (window == null) {
			return false;
		}
		return InputConstants.isKeyDown(window, glfwKey);
	}

	public static boolean canVanillaSprint(LocalPlayer player, Minecraft client) {
		if (player == null || client == null) {
			return false;
		}
		if (player.isCrouching()) {
			return false;
		}
		if (!client.options.keyUp.isDown()) {
			return false;
		}
		Abilities abilities = player.getAbilities();
		if (abilities != null && abilities.instabuild) {
			return true;
		}
		FoodData food = player.getFoodData();
		return food == null || food.getFoodLevel() > 6;
	}

	public static String keyDisplayName(int glfwKey) {
		if (glfwKey <= 0) {
			return "None";
		}
		return switch (glfwKey) {
			case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RShift";
			case GLFW.GLFW_KEY_LEFT_SHIFT -> "LShift";
			case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCtrl";
			case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCtrl";
			case GLFW.GLFW_KEY_RIGHT_ALT -> "RAlt";
			case GLFW.GLFW_KEY_LEFT_ALT -> "LAlt";
			case GLFW.GLFW_KEY_ESCAPE -> "Esc";
			case GLFW.GLFW_KEY_TAB -> "Tab";
			case GLFW.GLFW_KEY_CAPS_LOCK -> "Caps";
			case GLFW.GLFW_KEY_ENTER -> "Enter";
			case GLFW.GLFW_KEY_BACKSPACE -> "Back";
			case GLFW.GLFW_KEY_SPACE -> "Space";
			case GLFW.GLFW_KEY_UP -> "Up";
			case GLFW.GLFW_KEY_DOWN -> "Down";
			case GLFW.GLFW_KEY_LEFT -> "Left";
			case GLFW.GLFW_KEY_RIGHT -> "Right";
			case GLFW.GLFW_KEY_DELETE -> "Delete";
			case GLFW.GLFW_KEY_HOME -> "Home";
			case GLFW.GLFW_KEY_END -> "End";
			case GLFW.GLFW_KEY_PAGE_UP -> "PgUp";
			case GLFW.GLFW_KEY_PAGE_DOWN -> "PgDn";
			case GLFW.GLFW_KEY_INSERT -> "Insert";
			default -> {
				String name = GLFW.glfwGetKeyName(glfwKey, 0);
				if (name != null && !name.isBlank()) {
					yield name.toUpperCase();
				}
				if (glfwKey >= GLFW.GLFW_KEY_F1 && glfwKey <= GLFW.GLFW_KEY_F25) {
					yield "F" + (glfwKey - GLFW.GLFW_KEY_F1 + 1);
				}
				yield "Key" + glfwKey;
			}
		};
	}

	/**
	 * Whether ToggleSneak should rewrite {@code KeyboardInput} so vanilla
	 * treats sneak as held this tick. False when the module is off, when
	 * HOLD mode has a bound key that is not down, or when the player is
	 * riding ({@code getVehicle() != null}) so we never force a dismount.
	 * An unbound HOLD key ({@code keyCode() <= 0}) is treated as allowed,
	 * matching {@code SprintModule}.
	 */
	public static boolean shouldForceVanillaSneak(LocalPlayer player) {
		if (player == null || player.getVehicle() != null) {
			return false;
		}
		WorkspaceClient workspace = WorkspaceClient.getInstance();
		if (workspace == null) {
			return false;
		}
		ToggleSneakModule module = workspace.moduleManager().of(ToggleSneakModule.class);
		if (module == null || !module.isEnabled()) {
			return false;
		}
		if (module.mode() == ToggleSneakModule.Mode.HOLD) {
			int key = module.keyCode();
			if (key > 0 && !isKeyDown(key)) {
				return false;
			}
		}
		return true;
	}
}
