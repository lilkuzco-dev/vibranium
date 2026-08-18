package io.github.lilkuzcodev.vibranium;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/**
 * Godite's look, in the two places it is not a texture: tooltip text and burst particles.
 *
 * <p>Vibranium is one hue everywhere (270 deg, see {@code tools/gen-textures.js}); godite is
 * the same art with the hue swept across the sprite instead of held constant. This class is
 * the in-game half of that idea, so the item you are holding and the words describing it
 * agree with each other.
 */
public final class Rainbow {
	/** Vivid but not neon: full saturation, lightness a little above mid. */
	private static final float SATURATION = 1.0F;
	private static final float LIGHTNESS = 0.58F;

	/** Packed 0xRRGGBB for a point on the wheel; {@code hue} wraps, so any real number works. */
	public static int rgb(double hue) {
		double h = ((hue % 1.0) + 1.0) % 1.0;
		double q = LIGHTNESS < 0.5 ? LIGHTNESS * (1.0 + SATURATION) : LIGHTNESS + SATURATION - LIGHTNESS * SATURATION;
		double p = 2.0 * LIGHTNESS - q;
		return (channel(p, q, h + 1.0 / 3.0) << 16) | (channel(p, q, h) << 8) | channel(p, q, h - 1.0 / 3.0);
	}

	private static int channel(double p, double q, double t) {
		double x = ((t % 1.0) + 1.0) % 1.0;
		double v;
		if (x < 1.0 / 6.0) {
			v = p + (q - p) * 6.0 * x;
		} else if (x < 1.0 / 2.0) {
			v = q;
		} else if (x < 2.0 / 3.0) {
			v = p + (q - p) * (2.0 / 3.0 - x) * 6.0;
		} else {
			v = p;
		}
		return Math.clamp(Math.round(v * 255.0), 0, 255);
	}

	/**
	 * The same text, one hue per character, sweeping a full turn across the line.
	 *
	 * <p>Colouring per character needs the RESOLVED string, so this resolves the component
	 * and falls back to the flat-coloured original if the translation is missing — an
	 * unresolved key rendered one character at a time would put the raw key on the tooltip
	 * in rainbow, which is worse than no rainbow at all.
	 */
	public static Component gradient(Component source, String translationKey, ChatFormatting fallback, boolean bold) {
		String text = source.getString();
		if (text.isEmpty() || text.equals(translationKey)) {
			return source.copy().withStyle(bold ? new ChatFormatting[]{fallback, ChatFormatting.BOLD} : new ChatFormatting[]{fallback});
		}
		MutableComponent out = Component.empty();
		for (int i = 0; i < text.length(); i++) {
			Style style = Style.EMPTY.withColor(TextColor.fromRgb(rgb((double) i / text.length()))).withBold(bold);
			out.append(Component.literal(String.valueOf(text.charAt(i))).withStyle(style));
		}
		return out;
	}

	private Rainbow() {
	}
}
