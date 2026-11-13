package dev.workspace.client.setting;

import com.google.gson.JsonElement;

/**
 * A named, serializable module setting.
 */
public abstract class Setting<T> {
	private final String id;
	private final String displayName;
	private Runnable changeCallback;

	protected Setting(String id, String displayName) {
		this.id = id;
		this.displayName = displayName;
	}

	public final String id() {
		return this.id;
	}

	public final String displayName() {
		return this.displayName;
	}

	public abstract T get();

	public abstract void set(T value);

	public abstract JsonElement toJson();

	/**
	 * Applies a persisted value. Invalid input is ignored and the current
	 * (default) value is kept.
	 */
	public abstract void fromJson(JsonElement element);

	public final void setChangeCallback(Runnable changeCallback) {
		this.changeCallback = changeCallback;
	}

	protected final void notifyChanged() {
		if (this.changeCallback != null) {
			this.changeCallback.run();
		}
	}
}
