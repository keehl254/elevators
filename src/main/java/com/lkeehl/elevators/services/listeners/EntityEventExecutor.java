package com.lkeehl.elevators.services.listeners;

import com.lkeehl.elevators.events.ElevatorUseEvent;
import com.lkeehl.elevators.helpers.*;
import com.lkeehl.elevators.models.Elevator;
import com.lkeehl.elevators.models.ElevatorEventData;
import com.lkeehl.elevators.models.ElevatorType;
import com.lkeehl.elevators.services.ConfigService;
import com.lkeehl.elevators.services.ElevatorRecipeService;
import com.lkeehl.elevators.services.HookService;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.Random;

public class EntityEventExecutor {

    private static final Random random = new Random();

    public static void onJoin(PlayerJoinEvent e) {
        ElevatorRecipeService.discoverRecipesForPlayer(e.getPlayer());
    }
    public static void onJumpDefault(PlayerMoveEvent event) {

        if (event.getPlayer().isFlying()) return;
        if (((Math.ceil((event.getFrom().getY() % 1) * 10000.0) / 10000.0) % 0.0625) != 0) return;
        if (event.getFrom().getY() >= event.getTo().getY()) return;
        if (((Math.ceil((event.getTo().getY() - event.getFrom().getY()) * 10000.0) / 10000.0) % 0.0625) == 0) return;

        Block block = event.getPlayer().getLocation().subtract(0, 1, 0).getBlock();
        if (!(block.getState() instanceof ShulkerBox box)) return;

        ElevatorType elevatorType = ElevatorHelper.getElevatorType(box.getBlock());
        if (elevatorType == null) return;

        if (ConfigService.isWorldDisabled(event.getPlayer().getWorld())) {
            if (ElevatorHelper.hasOrAddPlayerCoolDown(event.getPlayer(), "message"))
                MessageHelper.sendWorldDisabledMessage(event.getPlayer(), new ElevatorEventData(elevatorType));
            return;
        }

        Elevator elevator = new Elevator(box, elevatorType);
        if(!HookService.canUseElevator(event.getPlayer(), elevator, true))
            return;

        ElevatorEventData closest = ElevatorHelper.findDestinationElevator(event.getPlayer(), elevator, (byte) 1);
        if (closest == null) return;

        if (!ElevatorPermHelper.canUseElevator(event.getPlayer(), closest)) {
            if (ElevatorHelper.hasOrAddPlayerCoolDown(event.getPlayer(), "message"))
                MessageHelper.sendCantUseMessage(event.getPlayer(), closest);
            return;
        }

        ElevatorUseEvent useEvent = new ElevatorUseEvent(event.getPlayer(), box, closest);
        Bukkit.getPluginManager().callEvent(useEvent);
        if (useEvent.isCancelled()) return;

        ElevatorHelper.onElevatorUse(event.getPlayer(), closest);
    }

    public static void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;

        Block block = event.getPlayer().getLocation().getBlock().getLocation().subtract(0, 1, 0).getBlock();
        if (!(block.getState() instanceof ShulkerBox box)) return;

        ElevatorType elevatorType = ElevatorHelper.getElevatorType(box.getBlock());
        if (elevatorType == null) return;

        if (ConfigService.isWorldDisabled(event.getPlayer().getWorld())) {
            MessageHelper.sendWorldDisabledMessage(event.getPlayer(), new ElevatorEventData(elevatorType));
            return;
        }

        Elevator elevator = new Elevator(box, elevatorType);
        if(!HookService.canUseElevator(event.getPlayer(), elevator, true))
            return;

        ElevatorEventData closest = ElevatorHelper.findDestinationElevator(event.getPlayer(), elevator, (byte) -1);
        if (closest == null) return;

        if (!ElevatorPermHelper.canUseElevator(event.getPlayer(), closest)) {
            if (ElevatorHelper.hasOrAddPlayerCoolDown(event.getPlayer(), "message"))
                MessageHelper.sendCantUseMessage(event.getPlayer(), closest);
            return;
        }

        ElevatorUseEvent useEvent = new ElevatorUseEvent(event.getPlayer(), box, closest);
        Bukkit.getPluginManager().callEvent(useEvent);
        if (useEvent.isCancelled()) return;

        ElevatorHelper.onElevatorUse(event.getPlayer(), closest);
    }

    public static void onRightClick(PlayerInteractEvent event) {
        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
        if (event.isBlockInHand() && event.getPlayer().isSneaking()) return;
        if (event.getClickedBlock() == null) return;
        if (!(event.getClickedBlock().getState() instanceof ShulkerBox box)) return;

        ElevatorType elevatorType = ElevatorHelper.getElevatorType(box.getBlock());
        if (elevatorType == null) return;

        if (event.getPlayer().isSneaking()) {
            if (event.getHand() == null || !event.getHand().equals(EquipmentSlot.HAND))
                return;
            InventoryHelper.openInteractMenu(event.getPlayer(), new Elevator(box, elevatorType));
        }

    }

    @SuppressWarnings("deprecation") // I hate that Paper deprecated ChatColor >_>
    public static void onPickup(EntityPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        if (ItemStackHelper.isNotShulkerBox(item.getType())) return;
        if (!ElevatorHelper.isElevator(item)) return;
        if (!(event.getEntity() instanceof Player player)) return;

        ItemMeta itemMeta = item.getItemMeta();
        if(itemMeta == null) return; // To appease the god that is intellisense.

        ElevatorType elevatorType = ElevatorHelper.getElevatorType(item);
        if(elevatorType.getMaxStackSize() <= 1) return; // I really wish Minecraft would support custom item maxStackSizes already. Returning here gives the most natural pickup.

        int pickupAmount = item.getAmount();

        Map<ItemStack, Integer> leftoverItems = ItemStackHelper.addElevatorToInventory(elevatorType, item.getAmount(), item.getType(), player.getInventory(), itemMeta.getDisplayName(), itemMeta.getLore());

        if(leftoverItems.size() >= 1) { // The itemstack helper will add to all possible places. If it failed, there is no space. This event shouldn't fire again if that is the case.
            int itemsLeft = leftoverItems.values().stream().mapToInt(i->i).sum();
            item.setAmount(itemsLeft);

            event.setCancelled(true);
            return;
        }
        // Our itemstack helper managed to add all the items :)
        event.setCancelled(true);
        player.incrementStatistic(Statistic.PICKUP, item.getType(), pickupAmount); // Custom statistics for elevators might be nice, too :P
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.2F, ((random.nextFloat() - random.nextFloat()) * 0.7F + 1.0F) * 2.0F);

        event.getItem().remove();

    }

}
