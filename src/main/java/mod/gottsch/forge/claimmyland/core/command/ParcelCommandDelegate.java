package mod.gottsch.forge.claimmyland.core.command;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.forge.claimmyland.core.parcel.NationParcel;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelFactory;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.util.LangUtil;
import mod.gottsch.forge.claimmyland.core.util.ModUtil;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 *
 * @author Mark Gottschling on Mar 28, 2024
 *
 */
public class ParcelCommandDelegate {

    public static int listParcelsByAbandoned(CommandSourceStack source) {
        try {
            List<Component> messages = new ArrayList<>();
            messages.add(Component.literal(""));
            messages.add(Component.translatable(LangUtil.chat("parcel.list.abandoned")).withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD, ChatFormatting.WHITE));
            messages.add(Component.literal(""));

            List<Component> components = formatList(messages, ParcelRegistry.findAbandoned());
            components.forEach(component -> {
                source.sendSuccess(() -> component, false);
            });

        } catch(Exception e) {
            ClaimMyLand.LOGGER.error("an error occurred listing abandoned parcels:", e);
            source.sendFailure(Component.translatable(LangUtil.chat("unexpected_error")).withStyle(ChatFormatting.RED));
        }
        return 1;
    }
    /**
     *
     * @param source
     * @param ownerName
     * @return
     */
    public static int listParcelsByOwner(CommandSourceStack source, String ownerName) {
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(ownerName);
        return listParcelsByOwner(source, player);
    }

    public static int listParcelsByOwner(CommandSourceStack source, ServerPlayer player) {
        List<Component> messages = new ArrayList<>();
        messages.add(Component.literal(""));
        messages.add(Component.translatable(LangUtil.chat("parcel.list"), player.getName().getString()).withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD, ChatFormatting.WHITE));
        messages.add(Component.literal(""));

        List<Component> components = formatList(messages, ParcelRegistry.findByOwner(player.getUUID()));
        components.forEach(component -> {
            source.sendSuccess(() -> component, false);
        });
        return 1;
    }

    public static int listParcelsByNation(CommandSourceStack source, String nationName) {
        try {
            // find the nation by name
            NationParcel nation = (NationParcel) ParcelRegistry.getNations().stream()
                    .filter(n -> nationName.equalsIgnoreCase(((NationParcel) n).getName()))
                    .findFirst().orElseThrow();

            List<Component> messages = new ArrayList<>();
            messages.add(Component.literal(""));
            messages.add(Component.translatable(LangUtil.chat("parcel.list"), nation.getName()).withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD, ChatFormatting.WHITE));
            messages.add(Component.literal(""));

            List<Component> components = formatList(messages, ParcelRegistry.findChildrenByNationId(nation.getNationId()));
            components.forEach(component -> {
                source.sendSuccess(() -> component, false);
            });

        } catch(Exception e) {
            ClaimMyLand.LOGGER.error("an error occurred listing parcels by nation:", e);
            source.sendFailure(Component.translatable(LangUtil.chat("unexpected_error")).withStyle(ChatFormatting.RED));
        }
        return 1;
    }

    static List<Component> formatList(List<Component> messages, List<Parcel> list) {

        if (list.isEmpty()) {
            messages.add(Component.translatable(LangUtil.chat("parcel.list.empty")).withStyle(ChatFormatting.AQUA));
        }
        else {
            list.forEach(parcel -> {
                messages.add(Component.translatable(parcel.getName().toUpperCase() + ": ").withStyle(ChatFormatting.AQUA)
                        .append(Component.translatable(String.format("(%s) to (%s)",
                                formatCoords(parcel.getMinCoords()),
                                formatCoords(parcel.getMaxCoords()))).withStyle(ChatFormatting.GREEN)
                        )
                        .append(Component.translatable(", size: (" + formatCoords(ModUtil.getSize( parcel.getSize()) ) + ")").withStyle(ChatFormatting.WHITE))
                );

//				[STYLE].withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + blockpos.getX() + " " + s1 + " " + blockpos.getZ())).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.coordinates.tooltip"))
            });
        }
        return messages;
    }

    private static String formatCoords(ICoords coords) {
        return String.format("%s, %s, %s", coords.getX(), coords.getY(), coords.getZ());
    }


    public static int addParcel(CommandSourceStack source, String ownerName, BlockPos pos, int xSize, int ySizeUp, int ySizeDown, int zSize, String parcelType) {
        return addParcel(source, ownerName, pos, xSize, ySizeUp, ySizeDown, zSize, parcelType, "");
    }

    // TODO need a command to remove owner from citizen parcel ie make it claimable
    public static int addParcel(CommandSourceStack source, String ownerName, BlockPos pos, int xSize, int ySizeUp, int ySizeDown, int zSize, String parcelType, String nationName) {
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(ownerName);

        // find the nation by name
        UUID nationId = ParcelRegistry.getNations().stream()
                .filter(n -> nationName.equalsIgnoreCase(((NationParcel) n).getName()))
                .findFirst()
                .map(n -> ((NationParcel)n).getNationId()).orElse(null);

        ParcelType type = ParcelType.valueOf(parcelType);
        // validation
        if ((type == ParcelType.CITIZEN || type == ParcelType.ZONE) && nationId == null) {
            source.sendFailure(Component.translatable(LangUtil.chat("parcel.citizen.nationId_required")).withStyle(ChatFormatting.RED));
            return 1;
        }

        if (player != null) {
            // create a relative sized Box
            Box size = new Box(Coords.of(0, -ySizeDown, 0), new Coords(xSize-1, ySizeUp-1, zSize-1));
            ICoords coords = Coords.of(pos);
            Optional<Parcel> parcelOptional = ParcelFactory.create(type, nationId);
            if (parcelOptional.isPresent()) {
                Parcel parcel = parcelOptional.get();
                parcel.setOwnerId(player.getUUID());
                parcel.setCoords(coords);
                parcel.setSize(size);

                // commands override the placement rules
//                boolean canPlace = parcel.canPlaceAt(source.getLevel(), coords);
//                if (canPlace) {
                /*
                 * check if parcel is within another existing parcel
                 */
                Optional<Parcel> registryParcel = ParcelRegistry.findLeastSignificant(coords);

                boolean successfulClaim = registryParcel.map(parentParcel -> parcel.handleEmbeddedClaim(source.getLevel(), parentParcel, parcel.getBox())).orElseGet(() -> parcel.handleClaim(source.getLevel(), parcel.getBox()));
                if (successfulClaim) {
                    source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.add.success")).withStyle(ChatFormatting.GREEN), false);
                    CommandHelper.save(source.getLevel());
                } else {
                    source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.add.failure_with_overlaps")).withStyle(ChatFormatting.RED), false);
                }
//            }
//            else {
//                    source.sendFailure(Component.translatable(LangUtil.chat("parcel.add.failure")).withStyle(ChatFormatting.RED));
//                }
            } else {
                ClaimMyLand.LOGGER.error("unable to create parcel with provided nationid -> {}", nationId);
                source.sendFailure(Component.translatable(LangUtil.chat("unexpected_error")).withStyle(ChatFormatting.RED));
            }
//                // TODO need to refactory parcels/deeds so they can be called to create the parcel
//                // properly. else all the logic has to be repeated here with lots of switch statements.
//                ItemStack deed = switch (ParcelType.valueOf(parcelType)) {
//                    case PLAYER -> DeedFactory.createPlayerDeed(size);
//                    case NATION -> DeedFactory.createNationDeed(source.getLevel(), size);
//                    case CITIZEN -> DeedFactory.createCitizenDeed(size);
////                    {
////                        Optional<Parcel> registryParcel = ParcelRegistry.findLeastSignificant(coords);
////                        if (registryParcel.isPresent() && (registryParcel.get().getType() == ParcelType.NATION || registryParcel.get().getType() == ParcelType.CITIZEN_ZONE)) {
////                            yield DeedFactory.createCitizenDeed(size, ((NationParcel) registryParcel.get()).getNationId());
////                        } else {
////                            yield ItemStack.EMPTY;
////                        }
////                    }
//                    case CITIZEN_ZONE, default -> ItemStack.EMPTY;
//                };
//
//                if (deed == ItemStack.EMPTY) {
//                    // TODO display error
//                    return 1;
//                }
//                // TODO need to run the placement rules.
//                // TODO if player parcel and in citizen zone, convert to citizen.
//                // TODO need to add the nation id if citizen or citizen zone
            // TODO run claim rules
//                Optional<Parcel> registryParcel = ParcelRegistry.findLeastSignificant(coords);
//                boolean canPlaceParcel = registryParcel.map(value -> deed.handleEmbeddedClaim(parcel, value)).orElseGet(this::handlePlacementRules);


//
//                // ensure that it still meets overlap criteria
//                Box parcelBox = parcel.getBox();
//                Box inflatedBox = ModUtil.inflate(parcelBox, parcel.getBufferSize());
//                // TODO need to change when Nation and Citizen parcels are implemented
//                // TODO need a method that checks if placement is valid
//                // ie Parcel.isPlacementValid(parcel) which checks all overlaps if any are person or citizen, or if nation, then check
//                // 1) does this parcel belong to the nation
//                // 2) is totally within bounds of nation
//                if (ParcelRegistry.findBoxes(inflatedBox).isEmpty()) {
//                    ParcelRegistry.add(parcel);
//                    source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.add.success")).withStyle(ChatFormatting.GREEN), false);
//                    CommandHelper.save(source.getLevel());
//                } else {
//                    source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.add.failure_with_overlaps")).withStyle(ChatFormatting.GREEN), false);
//                }
//
//            } else {
//                source.sendFailure(Component.translatable(LangUtil.chat("parcel.add.failure")).withStyle(ChatFormatting.GREEN));
//            }

        }
        else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return 1;
    }

    public static int abandonParcel(CommandSourceStack source, String ownerName, String parcelName) {
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(ownerName);
        if (player != null) {
            List<Parcel> parcels = ParcelRegistry.findByOwner(player.getUUID());
            Optional<Parcel> parcel = parcels.stream().filter(p -> p.getName().equalsIgnoreCase(parcelName)).findFirst();
            if (parcel.isPresent()) {
                // remove the owner from parcel
//                parcel.get().setOwnerId(null);
                if (ParcelRegistry.abandonParcel(parcel.get().getId())) {
                    source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.abandon.success")).withStyle(ChatFormatting.GREEN), false);
                    CommandHelper.save(source.getLevel());
                }
                else {
                    // TODO failure
                }
            } else {
                source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.abandon.failure")).withStyle(ChatFormatting.RED), false);
            }
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return 1;
    }

    public static int removeParcel(CommandSourceStack source, String ownerName, String parcelName) {
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(ownerName);
        if (player != null) {
            List<Parcel> parcels = ParcelRegistry.findByOwner(player.getUUID());
            Optional<Parcel> parcel = parcels.stream().filter(p -> p.getName().equalsIgnoreCase(parcelName)).findFirst();
            if (parcel.isPresent()) {
                // remove the border
                BlockEntity blockEntity = source.getLevel().getBlockEntity(parcel.get().getCoords().toPos());
                if (blockEntity instanceof FoundationStoneBlockEntity) {
                    ((FoundationStoneBlockEntity)blockEntity).removeParcelBorder(parcel.get().getCoords());
                }
                // unregister the parcel
                ParcelRegistry.removeParcel(parcel.get());

                source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.remove.success")).withStyle(ChatFormatting.GREEN), false);
                CommandHelper.save(source.getLevel());
            } else {
                source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.remove.failure")).withStyle(ChatFormatting.RED), false);
            }
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return 1;
    }

    public static int renameParcel(CommandSourceStack source, String ownerName, String parcelName, String newName) {
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(ownerName);
        if (player != null) {
            List<Parcel> parcels = ParcelRegistry.findByOwner(player.getUUID());
            Optional<Parcel> parcel = parcels.stream().filter(p -> p.getName().equalsIgnoreCase(parcelName)).findFirst();
            if (parcel.isPresent()) {
                // TODO ensure that the new name is unique across ALL parcels
                parcel.get().setName(newName);
                source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.rename.success")).withStyle(ChatFormatting.GREEN), false);
                CommandHelper.save(source.getLevel());
            } else {
                source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.rename.failure")).withStyle(ChatFormatting.RED), false);
            }
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return 1;
    }

    /**
     *
     * @param source
     * @param ownerName
     * @param parcelName
     * @param newOwnerName
     * @return
     */
    public static int transferParcel(CommandSourceStack source, String ownerName, String parcelName, String newOwnerName) {
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(ownerName);
        if (player != null) {
            List<Parcel> parcels = ParcelRegistry.findByOwner(player.getUUID());
            Optional<Parcel> parcel = parcels.stream().filter(p -> p.getName().equalsIgnoreCase(parcelName)).findFirst();
            if (parcel.isPresent()) {
                // get the new owner
                if(StringUtils.isNotBlank(newOwnerName)) {
                    ServerPlayer newOwner = source.getServer().getPlayerList().getPlayerByName(newOwnerName);
                    if (newOwner != null) {
//                        parcel.get().setOwnerId(newOwner.getUUID());
                        ParcelRegistry.updateOwner(parcel.get().getId(), newOwner.getUUID());
                    } else {
                        CommandHelper.sendUnableToLocatePlayerMessage(source, newOwnerName);
                        return -1;
                    }
                }
                source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.transfer.success")).withStyle(ChatFormatting.GREEN), false);
                CommandHelper.save(source.getLevel());
            } else {
                source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.transfer.failure")).withStyle(ChatFormatting.RED), false);
            }
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return 1;
    }

    public static int clearOwnerParcels(CommandSourceStack source, String ownerName) {
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(ownerName);
        if (player != null) {
            // remove all properties from player
            ParcelRegistry.removeParcel(player.getUUID());
            source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.remove.success")).withStyle(ChatFormatting.GREEN), false);
            CommandHelper.save(source.getLevel());
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return 1;
    }

    public static int clearAllParcels(CommandSourceStack source) {
        backupParcels(source);
        // TODO add an undo/restore command
        ParcelRegistry.clear();
        // TODO message
        return 1;
    }

    /**
     * TODO flesh out to save to file(s)
     * @param source
     * @return
     */
    public static int backupParcels(CommandSourceStack source) {
        String dump = ParcelRegistry.toJson();
        ClaimMyLand.LOGGER.debug("backup -> {}", dump);
        return 1;
    }
}
