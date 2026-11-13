package dev.workspace.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class EnumSetting<E extends Enum<E>> extends Setting<E> {
	private final Class<E> type;
	private E value;

	public EnumSetting(String id, String displayName, E defaultValue) {
		super(id, displayName);
		if (defaultValue == null) {
			throw new IllegalArgumentException("defaultValue is required");
		}
		this.type = defaultValue.getDeclaringClass();
		this.value = defaultValue;
	}

	public Class<E> enumType() {
		return this.type;
	}

	public E[] constants() {
		return this.type.getEnumConstants();
	}

	@Override
	public E get() {
		return this.value;
	}

	@Override
	public void set(E value) {
		if (value == null || this.value == value) {
			return;
		}
		this.value = value;
		notifyChanged();
	}

	public void cycle() {
		E[] all = constants();
		set(all[(this.value.ordinal() + 1) % all.length]);
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(this.value.name());
	}

	@Override
	public void fromJson(JsonElement element) {
		if (element == null || !element.isJsonPrimitive()) {
			return;
		}
		try {
			set(Enum.valueOf(this.type, element.getAsString()));
		} catch (IllegalArgumentException ignored) {
			// Keep the default when the stored name is unknown.
		}
	}
}
