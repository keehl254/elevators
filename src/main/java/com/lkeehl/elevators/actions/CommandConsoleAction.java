package com.lkeehl.elevators.actions;

import com.lkeehl.elevators.models.ElevatorAction;
import com.lkeehl.elevators.models.ElevatorActionGrouping;
import com.lkeehl.elevators.models.ElevatorType;
import org.bukkit.Bukkit;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public class CommandConsoleAction extends ElevatorAction {

    private static final ElevatorActionGrouping<String> commandGrouping = new ElevatorActionGrouping<>("", i -> i, "command","c");

    public CommandConsoleAction(ElevatorType elevatorType) {
        super(elevatorType, "command-console", "command", commandGrouping);
    }

    @Override
    protected void onInitialize(String value) {

    }

    @Override
    public void execute(ShulkerBox from, ShulkerBox to, ElevatorType elevator, Player player) {
        String value = elevator.formatPlaceholders(player,from,to, this.value);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), PlaceHolders.request(player.getUniqueId(), value));
    }

    @Override
    public CompletableFuture<Boolean> openCreate(ElevatorType elevatorType, Player player, byte direction) {
        return new CompletableFuture<>();
    }
}