package dev.workspace.client.event;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Small synchronous event bus used only on the Minecraft client thread.
 * Listeners are stored per exact event class (no superclass dispatch).
 */
public final class EventBus {
	private final Map<Class<?>, List<Consumer<?>>> listeners = new IdentityHashMap<>();

	public synchronized <T> EventSubscription subscribe(Class<T> eventType, Consumer<T> handler) {
		if (eventType == null || handler == null) {
			throw new IllegalArgumentException("eventType and handler are required");
		}
		this.listeners.computeIfAbsent(eventType, type -> new ArrayList<>()).add(handler);
		return new EventSubscription(this, eventType, handler);
	}

	synchronized void unsubscribe(Class<?> eventType, Consumer<?> handler) {
		List<Consumer<?>> list = this.listeners.get(eventType);
		if (list == null) {
			return;
		}
		list.remove(handler);
		if (list.isEmpty()) {
			this.listeners.remove(eventType);
		}
	}

	/**
	 * Delivers {@code event} to every current subscriber. The listener list is
	 * copied first so a handler may subscribe or unsubscribe safely.
	 */
	@SuppressWarnings("unchecked")
	public <T> T post(T event) {
		if (event == null) {
			return null;
		}
		List<Consumer<?>> snapshot;
		synchronized (this) {
			List<Consumer<?>> list = this.listeners.get(event.getClass());
			if (list == null || list.isEmpty()) {
				return event;
			}
			snapshot = List.copyOf(list);
		}
		for (Consumer<?> handler : snapshot) {
			((Consumer<T>) handler).accept(event);
		}
		return event;
	}
}
