package dev.workspace.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class NumberSetting extends Setting<Double> {
	private final double min;
	private final double max;
	private final double step;
	private double value;

	public NumberSetting(String id, String displayName, double defaultValue, double min, double max, double step) {
		super(id, displayName);
		if (min > max) {
			throw new IllegalArgumentException("min must be <= max");
		}
		if (step <= 0.0) {
			throw new IllegalArgumentException("step must be positive");
		}
		this.min = min;
		this.max = max;
		this.step = step;
		this.value = snap(defaultValue);
	}

	public double min() {
		return this.min;
	}

	public double max() {
		return this.max;
	}

	public double step() {
		return this.step;
	}

	@Override
	public Double get() {
		return this.value;
	}

	public double asDouble() {
		return this.value;
	}

	public int asInt() {
		return (int) Math.round(this.value);
	}

	public float asFloat() {
		return (float) this.value;
	}

	@Override
	public void set(Double value) {
		if (value == null) {
			return;
		}
		double next = snap(value);
		if (Double.compare(this.value, next) == 0) {
			return;
		}
		this.value = next;
		notifyChanged();
	}

	public void setFromRatio(double ratio) {
		double clamped = Math.max(0.0, Math.min(1.0, ratio));
		set(this.min + clamped * (this.max - this.min));
	}

	public double ratio() {
		if (this.max == this.min) {
			return 0.0;
		}
		return (this.value - this.min) / (this.max - this.min);
	}

	private double snap(double raw) {
		double clamped = Math.max(this.min, Math.min(this.max, raw));
		double steps = Math.round((clamped - this.min) / this.step);
		double snapped = this.min + steps * this.step;
		return Math.max(this.min, Math.min(this.max, snapped));
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(this.value);
	}

	@Override
	public void fromJson(JsonElement element) {
		if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
			set(element.getAsDouble());
		}
	}
}
