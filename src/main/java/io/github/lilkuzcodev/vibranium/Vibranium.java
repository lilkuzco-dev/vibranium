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
		VibraniumBlocks.init();
		VibraniumItems.init();
		VibraniumWorldgen.init();
		VibraniumCensusCommand.init();
		LOGGER.info("Vibranium initialized");
	}
}
