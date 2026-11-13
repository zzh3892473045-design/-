package dev.workspace.client.module.impl;

import dev.workspace.client.event.HudRenderEvent;
import dev.workspace.client.event.ModuleToggleEvent;
import dev.workspace.client.hud.NotificationRenderer;
import dev.workspace.client.module.Category;
import dev.workspace.client.module.Module;
import dev.workspace.client.setting.EnumSetting;
import dev.workspace.client.setting.NumberSetting;

/**
 * Local on-screen chips for persisted module toggles. Off by default; when
 * disabled, no chips are queued or drawn. No sound, history, or network.
 */
public final class NotificationsModule extends Module {
	public enum Position {
		TOP_CENTER,
		TOP_RIGHT,
		BOTTOM_RIGHT
	}

	private final NumberSetting duration = addSetting(new NumberSetting("duration", "Duration", 2.5, 1.0, 5.0, 0.5));
	private final EnumSetting<Position> position = addSetting(new EnumSetting<>("position", "Position", Position.TOP_CENTER));
	private final NotificationRenderer renderer = new NotificationRenderer();

	public NotificationsModule() {
		super("Notifications", Category.CLIENT, "Shows a brief on-screen chip when a module is toggled.");
		listen(ModuleToggleEvent.class, this::onModuleToggle);
		listen(HudRenderEvent.class, this::onHudRender);
	}

	@Override
	protected void onDisable() {
		this.renderer.clear();
	}

	private void onModuleToggle(ModuleToggleEvent event) {
		if (!event.persist()) {
			return;
		}
		Module module = event.module();
		if (module == this) {
			return;
		}
		if (module instanceof ClickGuiModule || "ClickGUI".equals(module.name())) {
			return;
		}
		this.renderer.enqueue(module.name(), event.enabled(), this.duration.asDouble());
	}

	private void onHudRender(HudRenderEvent event) {
		this.renderer.render(event, this.position.get());
	}
}
