package dev.workspace.client.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;

import dev.workspace.client.WorkspaceClient;
import dev.workspace.client.event.EventSubscription;
import dev.workspace.client.event.ModuleToggleEvent;
import dev.workspace.client.setting.Setting;

/**
 * A toggleable client feature. Event listeners registered with
 * {@link #listen(Class, Consumer)} are subscribed only while the module is on.
 */
public abstract class Module {
	private final String name;
	private final Category category;
	private final String description;
	private final List<Setting<?>> settings = new ArrayList<>();
	private final List<ListenerBinding<?>> listenerBindings = new ArrayList<>();
	private final List<EventSubscription> activeSubscriptions = new ArrayList<>();
	private boolean enabled;
	private int keyCode;

	protected Module(String name, Category category, String description) {
		this.name = name;
		this.category = category;
		this.description = description;
	}

	public final String name() {
		return this.name;
	}

	public final Category category() {
		return this.category;
	}

	public final String description() {
		return this.description;
	}

	public final boolean isEnabled() {
		return this.enabled;
	}

	public final int keyCode() {
		return this.keyCode;
	}

	public final List<Setting<?>> settings() {
		return Collections.unmodifiableList(this.settings);
	}

	public final Setting<?> setting(String id) {
		for (Setting<?> setting : this.settings) {
			if (setting.id().equals(id)) {
				return setting;
			}
		}
		return null;
	}

	public final void setKeyCode(int keyCode) {
		int next = Math.max(0, keyCode);
		if (this.keyCode == next) {
			return;
		}
		this.keyCode = next;
		WorkspaceClient client = WorkspaceClient.getInstance();
		if (client != null) {
			client.configManager().markDirty();
			client.keybindManager().onModuleKeyChanged(this, next);
		}
	}

	public final void toggle() {
		setEnabled(!this.enabled);
	}

	public final void setEnabled(boolean enabled) {
		setEnabled(enabled, true);
	}

	public final void setEnabled(boolean enabled, boolean persist) {
		if (this.enabled == enabled) {
			return;
		}
		this.enabled = enabled;
		if (enabled) {
			attachListeners();
			onEnable();
		} else {
			try {
				onDisable();
			} finally {
				detachListeners();
			}
		}
		WorkspaceClient client = WorkspaceClient.getInstance();
		if (persist) {
			if (client != null) {
				client.configManager().markDirty();
			}
		}
		if (client != null) {
			client.eventBus().post(new ModuleToggleEvent(this, enabled, persist));
		}
	}

	/**
	 * When {@code false}, the module's key is used as a hold input and does not
	 * toggle the enabled flag. {@link dev.workspace.client.module.impl.ZoomModule}
	 * uses this for its hold mode.
	 */
	public boolean toggleOnKeyPress() {
		return true;
	}

	protected void onEnable() {
	}

	protected void onDisable() {
	}

	public void onTick(Minecraft client) {
	}

	protected final <T extends Setting<?>> T addSetting(T setting) {
		this.settings.add(setting);
		setting.setChangeCallback(() -> {
			WorkspaceClient client = WorkspaceClient.getInstance();
			if (client != null) {
				client.configManager().markDirty();
			}
		});
		return setting;
	}

	protected final <T> void listen(Class<T> eventType, Consumer<T> handler) {
		this.listenerBindings.add(new ListenerBinding<>(eventType, handler));
		if (this.enabled) {
			this.activeSubscriptions.add(WorkspaceClient.getInstance().eventBus().subscribe(eventType, handler));
		}
	}

	private void attachListeners() {
		WorkspaceClient client = WorkspaceClient.getInstance();
		if (client == null) {
			return;
		}
		for (ListenerBinding<?> binding : this.listenerBindings) {
			this.activeSubscriptions.add(binding.subscribe(client));
		}
	}

	private void detachListeners() {
		for (EventSubscription subscription : this.activeSubscriptions) {
			subscription.unsubscribe();
		}
		this.activeSubscriptions.clear();
	}

	private record ListenerBinding<T>(Class<T> eventType, Consumer<T> handler) {
		EventSubscription subscribe(WorkspaceClient client) {
			return client.eventBus().subscribe(this.eventType, this.handler);
		}
	}
}
