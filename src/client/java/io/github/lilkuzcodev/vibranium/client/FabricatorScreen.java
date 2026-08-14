package io.github.lilkuzcodev.vibranium.client;

import io.github.lilkuzcodev.vibranium.FabricatorMenu;
import io.github.lilkuzcodev.vibranium.Vibranium;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * 176x206 fabricator screen; the background texture is composed by
 * tools/gen-textures.js from vanilla crafting_table.png parts.
 */
public class FabricatorScreen extends AbstractContainerScreen<FabricatorMenu> {
	private static final Identifier TEXTURE = Vibranium.id("textures/gui/container/fabricator.png");

	public FabricatorScreen(final FabricatorMenu menu, final Inventory inventory, final Component title) {
		super(menu, inventory, title, 176, 206);
	}

	@Override
	public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
	}
}
