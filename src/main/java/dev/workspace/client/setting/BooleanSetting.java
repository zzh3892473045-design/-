package dev.workspace.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class BooleanSetting extends Setting<Boolean> {
	private boolean value;

	public BooleanSetting(String id, String displayName, boolean defaultValue) {
		super(id, displayName);
		this.value = defaultValue;
	}

	@Override
	public Boolean get() {
		return this.value;
	}

	public boolean asBoolean() {
		return this.value;
	}

	@Override
	public void set(Boolean value) {
		boolean next = Boolean.TRUE.equals(value);
		if (this.value == next) {
			return;
		}
		this.value = next;
		notifyChanged();
	}

	public void toggle() {
		set(!this.value);
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(this.value);
	}

	@Override
	public void fromJson(JsonElement element) {
		if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()) {
			set(element.getAsBoolean());
		}
	}
}
