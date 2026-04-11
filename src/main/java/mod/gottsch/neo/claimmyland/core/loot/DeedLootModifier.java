package mod.gottsch.neo.claimmyland.core.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import mod.gottsch.neo.claimmyland.core.config.Config;
import mod.gottsch.neo.claimmyland.core.item.DeedFactory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * @author Mark Gottschling on March 26, 2026
 */
public class DeedLootModifier extends LootModifier {

    public static final MapCodec<DeedLootModifier> CODEC = RecordCodecBuilder.mapCodec(
            inst -> codecStart(inst)
                    .and(Codec.FLOAT.fieldOf("chance").forGetter(m -> m.chance))
                    .and(Codec.STRING.fieldOf("tier").forGetter(m -> m.tier))
                    .apply(inst, DeedLootModifier::new));

    private final float chance;
    private final String tier;

    public DeedLootModifier(LootItemCondition[] conditions, float chance, String tier) {
        super(conditions);
        this.chance = chance;
        this.tier = tier;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot,
                                                 LootContext context) {
        if (!Config.SERVER.general.enableDeedLoot.get()) return generatedLoot;
        if (context.getRandom().nextFloat() < chance) {
            generatedLoot.add(DeedFactory.createTieredDeed(context.getRandom(), tier));
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}