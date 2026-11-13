package dev.workspace.client.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import net.fabricmc.loader.api.FabricLoader;

import dev.workspace.client.WorkspaceClient;
import dev.workspace.client.module.Category;
import dev.workspace.client.module.Module;
import dev.workspace.client.module.impl.ClickGuiModule;
import dev.workspace.client.setting.Setting;

/**
 * Local JSON persistence. The file lives under the game config directory and
 * is never sent over the network. A corrupt file is logged and discarded.
 */
public final class ConfigManager {
	public static final String RELATIVE_PATH = "workspace-client/config.json";
	private static final int SAVE_DELAY_TICKS = 20;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Map<Category, int[]> panelPositions = new EnumMap<>(Category.class);
	private Path file;
	private int dirtyTicks = -1;

	public Path file() {
		if (this.file == null) {
			this.file = FabricLoader.getInstance().getConfigDir().resolve(RELATIVE_PATH);
		}
		return this.file;
	}

	public void load() {
		Path path = file();
		if (!Files.isRegularFile(path)) {
			WorkspaceClient.LOGGER.info("No config at {}, using defaults", path);
			return;
		}
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			JsonElement rootElement = JsonParser.parseReader(reader);
			if (rootElement == null || !rootElement.isJsonObject()) {
				WorkspaceClient.LOGGER.warn("Config {} is empty or not an object; using defaults", path);
				return;
			}
			apply(rootElement.getAsJsonObject());
			WorkspaceClient.LOGGER.info("Loaded config from {}", path);
		} catch (IOException | RuntimeException exception) {
			WorkspaceClient.LOGGER.warn("Config {} is corrupt or unreadable; using defaults", path, exception);
		}
		this.dirtyTicks = -1;
	}

	public void markDirty() {
		this.dirtyTicks = SAVE_DELAY_TICKS;
	}

	public void tickDebounce() {
		if (this.dirtyTicks < 0) {
			return;
		}
		this.dirtyTicks--;
		if (this.dirtyTicks == 0) {
			saveNow();
		}
	}

	public void saveNow() {
		this.dirtyTicks = -1;
		Path path = file();
		try {
			Path parent = path.getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}
			try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				GSON.toJson(toJson(), writer);
			}
		} catch (IOException exception) {
			WorkspaceClient.LOGGER.warn("Failed to save config to {}", path, exception);
		}
	}

	public int[] panelPosition(Category category, int fallbackX, int fallbackY) {
		int[] stored = this.panelPositions.get(category);
		if (stored == null) {
			return new int[] {fallbackX, fallbackY};
		}
		return new int[] {stored[0], stored[1]};
	}

	public void setPanelPosition(Category category, int x, int y) {
		int[] stored = this.panelPositions.get(category);
		if (stored != null && stored[0] == x && stored[1] == y) {
			return;
		}
		this.panelPositions.put(category, new int[] {x, y});
		markDirty();
	}

	private void apply(JsonObject root) {
		WorkspaceClient workspace = WorkspaceClient.getInstance();
		if (workspace == null) {
			return;
		}
		JsonObject modules = object(root, "modules");
		if (modules != null) {
			for (Module module : workspace.moduleManager().modules()) {
				JsonObject entry = object(modules, module.name());
				if (entry == null) {
					continue;
				}
				if (entry.has("bind") && entry.get("bind").isJsonPrimitive()) {
					try {
						module.setKeyCode(entry.get("bind").getAsInt());
					} catch (RuntimeException ignored) {
						// Keep the default bind.
					}
				}
				JsonObject settings = object(entry, "settings");
				if (settings != null) {
					for (Setting<?> setting : module.settings()) {
						if (settings.has(setting.id())) {
							try {
								setting.fromJson(settings.get(setting.id()));
							} catch (RuntimeException exception) {
								WorkspaceClient.LOGGER.warn("Ignoring bad value for setting {} in {}", setting.id(), module.name(), exception);
							}
						}
					}
				}
				if (entry.has("enabled") && entry.get("enabled").isJsonPrimitive() && !(module instanceof ClickGuiModule)) {
					try {
						module.setEnabled(entry.get("enabled").getAsBoolean(), false);
					} catch (RuntimeException exception) {
						WorkspaceClient.LOGGER.warn("Ignoring bad enabled flag for {}", module.name(), exception);
					}
				}
			}
		}
		JsonObject panels = object(root, "panels");
		if (panels != null) {
			for (Category category : Category.values()) {
				JsonObject entry = object(panels, category.name());
				if (entry == null) {
					continue;
				}
				try {
					int x = entry.has("x") ? entry.get("x").getAsInt() : 20;
					int y = entry.has("y") ? entry.get("y").getAsInt() : 40;
					this.panelPositions.put(category, new int[] {x, y});
				} catch (RuntimeException ignored) {
					// Keep the default position.
				}
			}
		}
	}

	private JsonObject toJson() {
		JsonObject root = new JsonObject();
		root.addProperty("version", 1);
		JsonObject modules = new JsonObject();
		WorkspaceClient workspace = WorkspaceClient.getInstance();
		if (workspace != null) {
			for (Module module : workspace.moduleManager().modules()) {
				JsonObject entry = new JsonObject();
				boolean enabled = module instanceof ClickGuiModule ? false : module.isEnabled();
				entry.addProperty("enabled", enabled);
				entry.addProperty("bind", module.keyCode());
				JsonObject settings = new JsonObject();
				for (Setting<?> setting : module.settings()) {
					settings.add(setting.id(), setting.toJson());
				}
				entry.add("settings", settings);
				modules.add(module.name(), entry);
			}
		}
		root.add("modules", modules);
		JsonObject panels = new JsonObject();
		for (Map.Entry<Category, int[]> entry : this.panelPositions.entrySet()) {
			JsonObject position = new JsonObject();
			position.addProperty("x", entry.getValue()[0]);
			position.addProperty("y", entry.getValue()[1]);
			panels.add(entry.getKey().name(), position);
		}
		root.add("panels", panels);
		return root;
	}

	private static JsonObject object(JsonObject parent, String key) {
		if (parent == null || !parent.has(key)) {
			return null;
		}
		JsonElement element = parent.get(key);
		return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
	}
}
