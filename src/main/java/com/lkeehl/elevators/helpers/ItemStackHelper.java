package com.lkeehl.elevators.helpers;

import com.lkeehl.elevators.models.ElevatorType;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class ItemStackHelper {

    public static boolean isNotShulkerBox(Material type) {
        return !type.toString().endsWith("SHULKER_BOX");
    }

    public static Material getVariant(Material type, DyeColor color) {
        String name = type.toString().toLowerCase();
        for (DyeColor tColor : DyeColor.values()) {
            if (name.startsWith(tColor.toString().toLowerCase() + "_"))
                name = name.replaceFirst(Pattern.quote(tColor.toString().toLowerCase()), "");
        }
        name = (color.toString() + name).toUpperCase();
        Material variant = Material.matchMaterial(name);
        return variant == null ? type : variant;
    }

    private static ItemStack findElevatorType(ElevatorType elevatorType, ItemStack item, Inventory inv) {
        ItemStack elevator = null;
        for (ItemStack content : inv.getContents()) {
            if (content == null || content.getType().equals(Material.AIR))
                continue;
            if (ElevatorHelper.getElevatorType(content) != null && content.isSimilar(item)) {
                if (!Objects.equals(ElevatorHelper.getElevatorType(content), elevatorType))
                    continue;
                if (content.getAmount() >= elevatorType.getMaxStackSize())
                    continue;
                elevator = content;
                break;
            }
        }
        return elevator;
    }

    public static void giveElevator(Item itemEntity, Inventory inv) {
        ItemStack item = itemEntity.getItemStack();
        ElevatorType elevatorType = ElevatorHelper.getElevatorType(item);
        if (elevatorType == null)
            return;
        BaseElevators.getTag().updateItem(item, elevatorType);
        elevatorType.updateItemDisplay(item);

        while (item.getAmount() > 0) {
            ItemStack elevator = findElevatorType(elevatorType, item, inv);
            if (elevator == null) {
                elevator = item.clone();
                elevator.setAmount(1);
                if (!inv.addItem(elevator).isEmpty())
                    break;
            } else
                elevator.setAmount(elevator.getAmount() + 1);
            item.setAmount(item.getAmount() - 1);
        }
        if (item.getAmount() <= 0)
            itemEntity.remove();
        else
            itemEntity.setItemStack(item);
    }

    private static ItemStack createItem(Material type, int amount) {
        if (type == null)
            return null;
        return new ItemStack(type, amount);
    }

    public static ItemStack createItem(String name, Material type, int amount) {
        ItemStack item = createItem(type, amount);
        if (name == null || item == null || item.getItemMeta() == null)
            return item;
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createItem(String name, Material type, int amount, List<String> lore) {
        ItemStack item = createItem(name, type, amount);
        if (item == null || item.getItemMeta() == null)
            return item;
        ItemMeta meta = item.getItemMeta();
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createItem(String name, Material type, int amount, String... lore) {
        return createItem(name, type, amount, Arrays.asList(lore));
    }

}