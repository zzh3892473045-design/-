package dev.workspace.client.event;

import java.util.function.Consumer;

/**
 * A handle returned by {@link EventBus#subscribe(Class, Consumer)} so a
 * listener can be removed without the subscriber keeping the raw consumer.
 */
public final class EventSubscription {
	private final EventBus bus;
	private final Class<?> eventType;
	private final Consumer<?> handler;
	private boolean active = true;

	EventSubscription(EventBus bus, Class<?> eventType, Consumer<?> handler) {
		this.bus = bus;
		this.eventType = eventType;
		this.handler = handler;
	}

	public void unsubscribe() {
		if (!this.active) {
			return;
		}
		this.active = false;
		this.bus.unsubscribe(this.eventType, this.handler);
	}

	public boolean isActive() {
		return this.active;
	}
}
