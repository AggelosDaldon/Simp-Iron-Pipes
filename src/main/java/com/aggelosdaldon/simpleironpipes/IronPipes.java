package com.aggelosdaldon.simpleironpipes;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(IronPipes.MOD_ID)
public final class IronPipes {
    public static final String MOD_ID = "simple_iron_pipes";
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MOD_ID);
    public static final RegistryObject<SoundEvent> ITEM_IN_SOUND = sound("block.iron_pipe.item_in");
    public static final RegistryObject<SoundEvent> ITEM_OUT_SOUND = sound("block.iron_pipe.item_out");
    public static final RegistryObject<SoundEvent> ITEM_TURN_SOUND = sound("block.iron_pipe.turn");
    public static final RegistryObject<SoundEvent> ITEM_LAUNCH_SOUND = sound("block.iron_pipe.launch");
    // New deeper iron-transfer sound events; original events remain for resource-pack compatibility.
    public static final RegistryObject<SoundEvent> HEAVY_ITEM_IN_SOUND = sound("block.iron_pipe.heavy_item_in");
    public static final RegistryObject<SoundEvent> HEAVY_ITEM_OUT_SOUND = sound("block.iron_pipe.heavy_item_out");
    public static final RegistryObject<SoundEvent> HEAVY_ITEM_LAUNCH_SOUND = sound("block.iron_pipe.heavy_launch");
    public static final RegistryObject<SoundEvent> HEAVY_ITEM_TURN_SOUND = sound("block.iron_pipe.heavy_turn");
    private static RegistryObject<SoundEvent> sound(String id) {
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, id)));
    }

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final RegistryObject<Block> IRON_PIPE = BLOCKS.register("iron_pipe", () ->
            new IronPipeBlock(false, BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(2.5F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<Block> IRON_FITTING = BLOCKS.register("iron_fitting", () ->
            new IronPipeBlock(true, BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(2.5F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    public static final RegistryObject<Item> PIPE_ITEM = ITEMS.register("iron_pipe",
            () -> new BlockItem(IRON_PIPE.get(), new Item.Properties()));
    public static final RegistryObject<Item> FITTING_ITEM = ITEMS.register("iron_fitting",
            () -> new BlockItem(IRON_FITTING.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<IronPipeBlockEntity>> PIPE_ENTITY =
            BLOCK_ENTITIES.register("iron_inventory", () ->
                    BlockEntityType.Builder.of(IronPipeBlockEntity::new, IRON_PIPE.get(), IRON_FITTING.get()).build(null));

    public static final RegistryObject<CreativeModeTab> IRON_TAB =
            TABS.register("iron_pipes", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.simple_iron_pipes"))
                    .icon(() -> new ItemStack(PIPE_ITEM.get()))
                    .displayItems((params, output) -> {
                        output.accept(PIPE_ITEM.get());
                        output.accept(FITTING_ITEM.get());
                    }).build());

    public IronPipes() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        SOUNDS.register(eventBus);
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
        TABS.register(eventBus);
    }
}
