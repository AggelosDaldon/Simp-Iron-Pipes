package com.aggelosdaldon.simpleironpipes;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraft.world.inventory.MenuType;
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
    private static RegistryObject<SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, name)));
    }
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MOD_ID);
    public static final RegistryObject<MenuType<RoutingMenu>> ROUTING_MENU =
            MENUS.register("routing_menu", () -> IForgeMenuType.create(RoutingMenu::new));
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);
    public static final RegistryObject<Block> IRON_PIPE = BLOCKS.register("iron_pipe",
            () -> new IronPipeBlock(false, BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5F).requiresCorrectToolForDrops().sound(net.minecraft.world.level.block.SoundType.METAL).noOcclusion()));
    public static final RegistryObject<Block> ROUTING_JUNCTION = BLOCKS.register("routing_junction",
            () -> new IronPipeBlock(true, BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<Block> IRON_EXTRACTOR = BLOCKS.register("iron_extractor",
            () -> new IronPipeBlock(IronPipeBlock.Mode.EXTRACTOR, BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F).requiresCorrectToolForDrops().sound(net.minecraft.world.level.block.SoundType.METAL).noOcclusion()));
    public static final RegistryObject<Block> IRON_OUTLET = BLOCKS.register("iron_outlet",
            () -> new IronPipeBlock(IronPipeBlock.Mode.OUTLET, BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F).requiresCorrectToolForDrops().sound(net.minecraft.world.level.block.SoundType.METAL).noOcclusion()));
    public static final RegistryObject<Item> EXTRACTOR_ITEM = ITEMS.register("iron_extractor", () -> new BlockItem(IRON_EXTRACTOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> OUTLET_ITEM = ITEMS.register("iron_outlet", () -> new BlockItem(IRON_OUTLET.get(), new Item.Properties()));
    public static final RegistryObject<Item> PIPE_ITEM = ITEMS.register("iron_pipe", () -> new BlockItem(IRON_PIPE.get(), new Item.Properties()));
    public static final RegistryObject<Item> JUNCTION_ITEM = ITEMS.register("routing_junction", () -> new BlockItem(ROUTING_JUNCTION.get(), new Item.Properties()));
    public static final RegistryObject<CreativeModeTab> IRON_PIPES_TAB =
            CREATIVE_TABS.register("iron_pipes", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.simple_iron_pipes"))
                    .icon(() -> new ItemStack(PIPE_ITEM.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(PIPE_ITEM.get());
                        output.accept(JUNCTION_ITEM.get());
                        output.accept(EXTRACTOR_ITEM.get());
                        output.accept(OUTLET_ITEM.get());
                    }).build());
    public static final RegistryObject<BlockEntityType<IronPipeBlockEntity>> PIPE_ENTITY =
            BLOCK_ENTITIES.register("iron_pipe_entity", () ->
                    BlockEntityType.Builder.of(IronPipeBlockEntity::new, IRON_PIPE.get(), ROUTING_JUNCTION.get(), IRON_EXTRACTOR.get(), IRON_OUTLET.get()).build(null));

    public IronPipes() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        SOUNDS.register(bus);
        CREATIVE_TABS.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        MENUS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }
}
