package com.aggelosdaldon.simpleironpipes;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
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
    public static final RegistryObject<Item> PIPE_ITEM = ITEMS.register("iron_pipe", () -> new BlockItem(IRON_PIPE.get(), new Item.Properties()));
    public static final RegistryObject<Item> JUNCTION_ITEM = ITEMS.register("routing_junction", () -> new BlockItem(ROUTING_JUNCTION.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<IronPipeBlockEntity>> PIPE_ENTITY =
            BLOCK_ENTITIES.register("iron_pipe_entity", () ->
                    BlockEntityType.Builder.of(IronPipeBlockEntity::new, IRON_PIPE.get(), ROUTING_JUNCTION.get()).build(null));

    public IronPipes() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        MENUS.register(bus);
        BLOCK_ENTITIES.register(bus);
        bus.addListener(this::addCreativeItems);
    }
    private void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.accept(PIPE_ITEM);
            event.accept(JUNCTION_ITEM);
        }
    }
}
