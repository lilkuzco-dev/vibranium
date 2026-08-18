package io.github.lilkuzcodev.vibranium;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Vibranium implements ModInitializer {
	public static final String MOD_ID = "vibranium";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		VibraniumComponents.init(); // before anything touches ItemStacks
		VibraniumBlocks.init();
		VibraniumItems.init();
		VibraniumMachines.init();
		VibraniumEntities.init();
		KineticWard.init(); // armor damage listener; items must already be registered
		VibraniumWorldgen.init();
		VibraniumCensusCommand.init();
		VibraniumSelfTestCommand.init();
		VibraniumGearSelfTestCommand.init();
		LOGGER.info("Vibranium initialized");
	}
}
