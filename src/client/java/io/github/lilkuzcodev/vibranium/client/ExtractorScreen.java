package io.github.lilkuzcodev.vibranium.client;

import io.github.lilkuzcodev.vibranium.ExtractorMenu;
import io.github.lilkuzcodev.vibranium.Vibranium;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * 176x206 extractor screen. Background composed from vanilla furnace.png; the
 * purple flame/arrow fills are stashed in the same texture at (180,0)/(180,16).
 * The status line explains why the machine is (not) mining — the GUI-shown
 * halt reasons required by the spec.
 */
public class ExtractorScreen extends AbstractContainerScreen<ExtractorMenu> {
	private static final Identifier TEXTURE = Vibranium.id("textures/gui/container/extractor.png");

	public ExtractorScreen(final ExtractorMenu menu, final Inventory inventory, final Component title) {
		super(menu, inventory, title, 176, 206);
	}

	@Override
	public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		int xo = this.leftPos;
		int yo = this.topPos;
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
		if (this.menu.isLit()) {
			// flame drains bottom-up like the furnace's
			int litHeight = Mth.ceil(this.menu.litProgress() * 13.0F) + 1;
			graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xo + 9, yo + 36 + 14 - litHeight, 180, 14 - litHeight, 14, litHeight, 256, 256);
		}
		int arrowWidth = Mth.ceil(this.menu.mineProgress() * 24.0F);
		if (arrowWidth > 0) {
			graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xo + 110, yo + 49, 180, 16, arrowWidth, 16, 256, 256);
		}
	}

	@Override
	protected void extractLabels(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
		super.extractLabels(graphics, mouseX, mouseY);
		int status = this.menu.status();
		Component line = switch (status) {
			case 1 -> Component.translatable("gui.vibranium.extractor.running", this.menu.mineY());
			case 2 -> Component.translatable("gui.vibranium.extractor.halt_lava");
			case 3 -> Component.translatable("gui.vibranium.extractor.halt_water");
			case 4 -> Component.translatable("gui.vibranium.extractor.halt_unbreakable");
			case 5 -> Component.translatable("gui.vibranium.extractor.done");
			case 6 -> Component.translatable("gui.vibranium.extractor.full");
			default -> Component.translatable("gui.vibranium.extractor.no_fuel");
		};
		boolean bad = status >= 2 && status <= 4;
		graphics.text(this.font, line, 30, 26, bad ? 0xFFB03030 : 0xFF3F3F3F, false);
		if (status == 1 && this.menu.boosted()) {
			graphics.text(this.font, Component.translatable("gui.vibranium.extractor.boost"), 30, 38, 0xFF8834D8, false);
		}
	}
}
