package dev.workspace.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * Packed ARGB colour. The high byte is alpha; callers should pass opaque
 * values ({@code 0xFFrrggbb}) for text that must remain visible in 1.21.6+.
 */
public final class ColorSetting extends Setting<Integer> {
	private int value;

	public ColorSetting(String id, String displayName, int defaultArgb) {
		super(id, displayName);
		this.value = defaultArgb;
	}

	@Override
	public Integer get() {
		return this.value;
	}

	public int argb() {
		return this.value;
	}

	public int red() {
		return (this.value >> 16) & 0xFF;
	}

	public int green() {
		return (this.value >> 8) & 0xFF;
	}

	public int blue() {
		return this.value & 0xFF;
	}

	public int alpha() {
		return (this.value >>> 24) & 0xFF;
	}

	@Override
	public void set(Integer value) {
		if (value == null) {
			return;
		}
		int next = value;
		if (this.value == next) {
			return;
		}
		this.value = next;
		notifyChanged();
	}

	public void setRgb(int red, int green, int blue) {
		int a = alpha();
		if (a == 0) {
			a = 0xFF;
		}
		set((a << 24) | (clamp8(red) << 16) | (clamp8(green) << 8) | clamp8(blue));
	}

	public void setChannel(int channel, int amount) {
		int r = red();
		int g = green();
		int b = blue();
		int v = clamp8(amount);
		if (channel == 0) {
			r = v;
		} else if (channel == 1) {
			g = v;
		} else {
			b = v;
		}
		setRgb(r, g, b);
	}

	private static int clamp8(int value) {
		return Math.max(0, Math.min(255, value));
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(this.value);
	}

	@Override
	public void fromJson(JsonElement element) {
		if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
			set(element.getAsInt());
		}
	}
}
