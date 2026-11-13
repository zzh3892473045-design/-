package dev.workspace.client.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;

import dev.workspace.client.module.impl.ClickGuiModule;
import dev.workspace.client.module.impl.CrosshairModule;
import dev.workspace.client.module.impl.FullbrightModule;
import dev.workspace.client.module.impl.HudModule;
import dev.workspace.client.module.impl.SprintModule;
import dev.workspace.client.module.impl.ZoomModule;

public final class ModuleManager {
	private final List<Module> modules = new ArrayList<>();
	private final Map<Category, List<Module>> byCategory = new EnumMap<>(Category.class);

	public void registerBuiltins() {
		register(new ClickGuiModule());
		register(new HudModule());
		register(new FullbrightModule());
		register(new SprintModule());
		register(new ZoomModule());
		register(new CrosshairModule());
	}

	public void register(Module module) {
		this.modules.add(module);
		this.byCategory.computeIfAbsent(module.category(), category -> new ArrayList<>()).add(module);
	}

	public List<Module> modules() {
		return Collections.unmodifiableList(this.modules);
	}

	public List<Module> modules(Category category) {
		List<Module> list = this.byCategory.get(category);
		if (list == null) {
			return List.of();
		}
		return Collections.unmodifiableList(list);
	}

	public Module byName(String name) {
		if (name == null) {
			return null;
		}
		for (Module module : this.modules) {
			if (module.name().equalsIgnoreCase(name)) {
				return module;
			}
		}
		return null;
	}

	public <T extends Module> T of(Class<T> type) {
		for (Module module : this.modules) {
			if (type.isInstance(module)) {
				return type.cast(module);
			}
		}
		return null;
	}

	public void tickEnabled(Minecraft client) {
		for (Module module : this.modules) {
			if (module.isEnabled()) {
				module.onTick(client);
			}
		}
	}

	public List<Module> enabledVisibleInHud() {
		List<Module> enabled = new ArrayList<>();
		for (Module module : this.modules) {
			if (module.isEnabled() && !(module instanceof ClickGuiModule)) {
				enabled.add(module);
			}
		}
		enabled.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
		return enabled;
	}
}
