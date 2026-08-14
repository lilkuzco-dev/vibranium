package io.github.lilkuzcodev.vibranium;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * A shaped recipe for the fabricator's own "fabricating" recipe type. JSON shape
 * mirrors vanilla {@code minecraft:crafting_shaped} (key / pattern / result), but
 * the pattern may be up to {@link #MAX_SIZE}x{@link #MAX_SIZE}. The vanilla
 * {@link ShapedRecipePattern} match logic handles arbitrary sizes — only its JSON
 * codec is capped at 3x3, so this class supplies its own pattern codec.
 *
 * <p>Adding a fabricating recipe = dropping a JSON file in
 * {@code data/<ns>/recipe/} with {@code "type": "vibranium:fabricating"}. A
 * future SUPER FABRICATOR tier with a larger grid only needs MAX_SIZE raised and
 * a menu registered with the bigger dimensions.
 */
public class FabricatingRecipe implements Recipe<CraftingInput> {
	public static final int MAX_SIZE = 5;

	private final String group;
	private final ShapedRecipePattern pattern;
	private final ItemStackTemplate result;
	private @Nullable PlacementInfo placementInfo;

	public FabricatingRecipe(final String group, final ShapedRecipePattern pattern, final ItemStackTemplate result) {
		this.group = group;
		this.pattern = pattern;
		this.result = result;
	}

	@Override
	public boolean matches(final CraftingInput input, final Level level) {
		return this.pattern.matches(input);
	}

	@Override
	public ItemStack assemble(final CraftingInput input) {
		return this.result.create();
	}

	@Override
	public boolean showNotification() {
		return true;
	}

	@Override
	public String group() {
		return this.group;
	}

	@Override
	public RecipeSerializer<FabricatingRecipe> getSerializer() {
		return VibraniumMachines.FABRICATING_SERIALIZER;
	}

	@Override
	public RecipeType<FabricatingRecipe> getType() {
		return VibraniumMachines.FABRICATING;
	}

	@Override
	public PlacementInfo placementInfo() {
		if (this.placementInfo == null) {
			this.placementInfo = PlacementInfo.createFromOptionals(this.pattern.ingredients());
		}
		return this.placementInfo;
	}

	@Override
	public RecipeBookCategory recipeBookCategory() {
		// not shown in any vanilla recipe book (display() stays empty), but the
		// interface requires a category
		return RecipeBookCategories.CRAFTING_MISC;
	}

	// ---------- codecs ----------
	// Same JSON layout as vanilla shaped recipes, with the row/column cap raised
	// to MAX_SIZE. Decode-only for the pattern: recipes are hand-written JSON and
	// never re-encoded at runtime.
	private static final Codec<List<String>> PATTERN_CODEC = Codec.STRING.listOf().comapFlatMap(rows -> {
		if (rows.isEmpty()) {
			return DataResult.error(() -> "Invalid fabricating pattern: empty pattern not allowed");
		}
		if (rows.size() > MAX_SIZE) {
			return DataResult.error(() -> "Invalid fabricating pattern: too many rows, " + MAX_SIZE + " is maximum");
		}
		int width = rows.getFirst().length();
		for (String row : rows) {
			if (row.length() > MAX_SIZE) {
				return DataResult.error(() -> "Invalid fabricating pattern: too many columns, " + MAX_SIZE + " is maximum");
			}
			if (row.length() != width) {
				return DataResult.error(() -> "Invalid fabricating pattern: each row must be the same width");
			}
		}
		return DataResult.success(rows);
	}, Function.identity());
	private static final Codec<Character> SYMBOL_CODEC = Codec.STRING.comapFlatMap(symbol -> {
		if (symbol.length() != 1) {
			return DataResult.error(() -> "Invalid key entry: '" + symbol + "' is an invalid symbol (must be 1 character only).");
		}
		return " ".equals(symbol) ? DataResult.error(() -> "Invalid key entry: ' ' is a reserved symbol.") : DataResult.success(symbol.charAt(0));
	}, String::valueOf);

	private record RawPattern(Map<Character, Ingredient> key, List<String> pattern) {
		static final MapCodec<RawPattern> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				ExtraCodecs.strictUnboundedMap(SYMBOL_CODEC, Ingredient.CODEC).fieldOf("key").forGetter(RawPattern::key),
				PATTERN_CODEC.fieldOf("pattern").forGetter(RawPattern::pattern)
		).apply(i, RawPattern::new));
	}

	private static final MapCodec<ShapedRecipePattern> PATTERN_MAP_CODEC = RawPattern.MAP_CODEC.flatXmap(
			raw -> {
				try {
					return DataResult.success(ShapedRecipePattern.of(raw.key(), raw.pattern()));
				} catch (IllegalStateException e) {
					return DataResult.error(e::getMessage);
				}
			},
			pattern -> DataResult.error(() -> "Cannot re-encode a fabricating pattern"));

	public static final MapCodec<FabricatingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.optionalFieldOf("group", "").forGetter(FabricatingRecipe::group),
			PATTERN_MAP_CODEC.forGetter(r -> r.pattern),
			ItemStackTemplate.CODEC.fieldOf("result").forGetter(r -> r.result)
	).apply(i, FabricatingRecipe::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, FabricatingRecipe> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, FabricatingRecipe::group,
			ShapedRecipePattern.STREAM_CODEC, r -> r.pattern,
			ItemStackTemplate.STREAM_CODEC, r -> r.result,
			FabricatingRecipe::new);
}
