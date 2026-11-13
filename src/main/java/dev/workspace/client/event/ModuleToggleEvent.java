package dev.workspace.client.event;

import dev.workspace.client.module.Module;

/**
 * Fired after a module's enabled flag actually flips, once
 * {@code onEnable}/{@code onDisable} have run. The bus dispatches by exact
 * runtime type; this is a standalone final class.
 *
 * {@code persist} is {@code true} for a user or keybind toggle, and
 * {@code false} when the change comes from config load.
 */
public final class ModuleToggleEvent {
	private final Module module;
	private final boolean enabled;
	private final boolean persist;

	public ModuleToggleEvent(Module module, boolean enabled, boolean persist) {
		this.module = module;
		this.enabled = enabled;
		this.persist = persist;
	}

	public Module module() {
		return this.module;
	}

	public boolean enabled() {
		return this.enabled;
	}

	public boolean persist() {
		return this.persist;
	}
}
