package dev.workspace.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.OptionInstance;

/**
 * Direct access to the stored value so Fullbright can raise gamma for a
 * single light-map pass without going through {@link OptionInstance#set},
 * which clamps to the 0–1 brightness slider. The official field name is
 * {@code value} (mappings.dev, 1.21.11).
 */
@Mixin(OptionInstance.class)
public interface OptionInstanceAccessor {
	@Accessor("value")
	Object workspaceClient$getValue();

	@Accessor("value")
	void workspaceClient$setValue(Object value);
}
