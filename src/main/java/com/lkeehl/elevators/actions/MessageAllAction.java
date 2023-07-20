package com.lkeehl.elevators.actions;

import com.lkeehl.elevators.models.ElevatorAction;
import com.lkeehl.elevators.models.ElevatorActionGrouping;
import com.lkeehl.elevators.models.ElevatorType;
import org.bukkit.Bukkit;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public class MessageAllAction extends ElevatorAction {

    private static final ElevatorActionGrouping<String> messageGrouping = new ElevatorActionGrouping<>("", i -> i, "message","m");

    public MessageAllAction(ElevatorType elevatorType) {
        super(elevatorType, "message-all","message", messageGrouping);
    }

    @Override
    protected void onInitialize(String value) {

    }

    @Override
    public void execute(ShulkerBox from, ShulkerBox to, ElevatorType elevator, Player player) {
        String value = elevator.formatPlaceholders(player,from,to, BaseUtil.formatColors(this.getGroupingObject(messageGrouping)));
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(value));
    }

    @Override
    public CompletableFuture<Boolean> openCreate(ElevatorType elevatorType, Player player, byte direction) {
        return new CompletableFuture<>();
    }
}