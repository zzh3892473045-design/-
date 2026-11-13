package dev.workspace.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.renderer.LightTexture;

import dev.workspace.client.module.impl.FullbrightModule;

/**
 * Temporarily writes a high gamma into {@link OptionInstance} around
 * {@code LightTexture#updateLightTexture(float)} (official name, 1.21.11)
 * and restores it on return so the real option is never saved.
 *
 * Uncertainty: 1.21.11 may feed gamma into a GPU light-map path. If that
 * path no longer reads {@code options.gamma().get()} inside this method,
 * Fullbright will have no visible effect; see docs/handoff.md.
 */
@Mixin(LightTexture.class)
public class LightTextureMixin {
	@Unique
	private Double workspaceClient$savedGamma;

	@Inject(method = "updateLightTexture", at = @At("HEAD"))
	private void workspaceClient$boostGamma(float partialTick, CallbackInfo ci) {
		this.workspaceClient$savedGamma = null;
		if (!FullbrightModule.isEffectivelyActive()) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.options == null) {
			return;
		}
		OptionInstance<Double> gamma = client.options.gamma();
		this.workspaceClient$savedGamma = gamma.get();
		((OptionInstanceAccessor) (Object) gamma).workspaceClient$setValue(FullbrightModule.currentGamma());
	}

	@Inject(method = "updateLightTexture", at = @At("RETURN"))
	private void workspaceClient$restoreGamma(float partialTick, CallbackInfo ci) {
		if (this.workspaceClient$savedGamma == null) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client != null && client.options != null) {
			OptionInstance<Double> gamma = client.options.gamma();
			((OptionInstanceAccessor) (Object) gamma).workspaceClient$setValue(this.workspaceClient$savedGamma);
		}
		this.workspaceClient$savedGamma = null;
	}
}
