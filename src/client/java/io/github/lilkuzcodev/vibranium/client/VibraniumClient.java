package io.github.lilkuzcodev.vibranium.client;

import io.github.lilkuzcodev.vibranium.VibraniumEntities;
import io.github.lilkuzcodev.vibranium.VibraniumMachines;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public class VibraniumClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// vanilla registration, access-widened by Fabric (the fabric registry is deprecated in 26.x)
		EntityRenderers.register(VibraniumEntities.KINETIC_ENERGY_BALL, ThrownItemRenderer::new);
		MenuScreens.register(VibraniumMachines.FABRICATOR_MENU, FabricatorScreen::new);
		MenuScreens.register(VibraniumMachines.EXTRACTOR_MENU, ExtractorScreen::new);
	}
}
