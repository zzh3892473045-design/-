package dev.workspace.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;

import dev.workspace.client.adapter.ClientAdapters;

/**
 * After vanilla rebuilds {@link KeyboardInput#tick()} from the current key
 * bindings, optionally replace {@code keyPresses} with a copy that has
 * {@code shift=true}. That is equivalent to holding the sneak key, so
 * vanilla sneak speed, pose, collision, and the input packet all agree.
 * When ToggleSneak is off, in HOLD with the key released, or while riding,
 * this injection leaves the vanilla Input untouched.
 */
@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {
	@Inject(method = "tick()V", at = @At("RETURN"))
	private void workspaceClient$applyToggleSneak(CallbackInfo ci) {
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return;
		}
		LocalPlayer player = client.player;
		if (!ClientAdapters.shouldForceVanillaSneak(player)) {
			return;
		}
		KeyboardInput self = (KeyboardInput) (Object) this;
		Input current = self.keyPresses;
		if (current == null) {
			return;
		}
		self.keyPresses = new Input(
			current.forward(),
			current.backward(),
			current.left(),
			current.right(),
			current.jump(),
			true,
			current.sprint()
		);
	}
}
