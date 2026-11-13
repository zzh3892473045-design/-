package dev.workspace.client.event;

/**
 * Base type for events that a listener may cancel. The bus still delivers the
 * event to every subscriber; later listeners should check {@link #isCancelled()}.
 */
public abstract class CancellableEvent {
	private boolean cancelled;

	public final void cancel() {
		this.cancelled = true;
	}

	public final boolean isCancelled() {
		return this.cancelled;
	}
}
