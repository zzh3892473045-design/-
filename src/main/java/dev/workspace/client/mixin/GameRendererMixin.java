package dev.workspace.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;

import dev.workspace.client.module.impl.ZoomModule;

/**
 * Applies Zoom by overriding the return of the private official method
 * {@code GameRenderer#getFov(Camera, float, boolean)}. The third argument
 * is the "use FOV setting" flag; when it is false vanilla is computing a
 * fixed hand FOV and we leave it alone.
 *
 * The vanilla FOV option is never written, so it is restored automatically
 * when Zoom is not active.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void workspaceClient$applyZoom(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Float> cir) {
		if (!useFovSetting) {
			return;
		}
		Float override = ZoomModule.currentFovOrNull();
		if (override != null) {
			cir.setReturnValue(override);
		}
	}
}
