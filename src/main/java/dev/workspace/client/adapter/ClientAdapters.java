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

import dev.workspace.client.gui.ClickGuiScreen;

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
 *   <li>Keyboard state uses {@link GLFW#glfwGetCurrentContext()} plus
 *       {@link InputConstants#isKeyDown(long, int)} so we do not depend on
 *       {@code Window.handle()} vs {@code getHandle()}.</li>
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
		long window = GLFW.glfwGetCurrentContext();
		if (window == 0L) {
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
}
