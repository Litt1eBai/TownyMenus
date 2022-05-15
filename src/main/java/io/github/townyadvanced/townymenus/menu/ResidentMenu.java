package io.github.townyadvanced.townymenus.menu;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.SpawnType;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.utils.SpawnUtil;
import io.github.townyadvanced.townymenus.gui.MenuHelper;
import io.github.townyadvanced.townymenus.gui.MenuInventory;
import io.github.townyadvanced.townymenus.gui.MenuItem;
import io.github.townyadvanced.townymenus.gui.action.ClickAction;
import io.github.townyadvanced.townymenus.utils.Time;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class ResidentMenu {
    public static Supplier<MenuInventory> createResidentMenu(@NotNull Player player) {
        return () -> MenuInventory.builder()
                .rows(3)
                .title(Component.text("Resident Menu"))
                .addItem(MenuItem.builder(Material.PLAYER_HEAD)
                        .name(Component.text("View Friends", NamedTextColor.GREEN))
                        .lore(Component.text("Click to see your friends list.", NamedTextColor.GRAY))
                        .slot(11)
                        .action(ClickAction.paginate(Component.text("Resident Friends"), () -> formatFriendsView(player)))
                        .build())
                .addItem(formatResidentInfo(player.getUniqueId(), 13))
                .addItem(MenuItem.builder(Material.RED_BED)
                        .name(Component.text("Spawn", NamedTextColor.GREEN))
                        .lore(player.hasPermission(PermissionNodes.TOWNY_COMMAND_RESIDENT_SPAWN.getNode())
                                ? Component.text("Click to teleport to your spawn!", NamedTextColor.GRAY)
                                : Component.text("✖ You do not have enough permissions to use this!", NamedTextColor.RED))
                        .action(ClickAction.confirmation(ClickAction.run(() -> {
                            if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_RESIDENT_SPAWN.getNode()))
                                return;

                            Resident resident = TownyAPI.getInstance().getResident(player);
                            if (resident == null)
                                return;

                            try {
                                SpawnUtil.sendToTownySpawn(player, new String[]{}, resident, Translatable.of("msg_err_cant_afford_tp").forLocale(player), false, true, SpawnType.RESIDENT);
                            } catch (TownyException e) {
                                TownyMessaging.sendErrorMsg(player, e.getMessage(player));
                            }
                        })))
                        .slot(15)
                        .build())
                .addItem(MenuHelper.backButton()
                        .slot(26)
                        .build())
                .build();
    }

    /**
     * @param uuid The uuid of the resident
     * @return A formatted menu item, or an 'error' item if the resident isn't registered.
     */
    public static MenuItem formatResidentInfo(@NotNull UUID uuid, int slot) {
        Resident resident = TownyAPI.getInstance().getResident(uuid);

        if (resident == null)
            return MenuItem.builder(Material.PLAYER_HEAD)
                    .skullOwner(uuid)
                    .name(Component.text("Error", NamedTextColor.DARK_RED))
                    .lore(Component.text("Unknown or invalid resident.", NamedTextColor.RED))
                    .slot(slot)
                    .build();

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Status: ", NamedTextColor.DARK_GREEN).append(resident.isOnline() ? Component.text("● Online", NamedTextColor.GREEN) : Component.text("● Offline", NamedTextColor.RED)));

        if (resident.hasTown()) {
            lore.add(Component.text((resident.isMayor() ? "Mayor" : "Resident") + " of ", NamedTextColor.DARK_GREEN).append(Component.text(resident.getTownOrNull().getName(), NamedTextColor.GREEN)));

            if (resident.hasNation())
                lore.add(Component.text((resident.isKing() ? "Leader" : "Member") + " of ", NamedTextColor.DARK_GREEN).append(Component.text(resident.getNationOrNull().getName(), NamedTextColor.GREEN)));
        }

        lore.add(Component.text("Registered ", NamedTextColor.DARK_GREEN).append(Component.text(Time.formatRegistered(resident.getRegistered()), NamedTextColor.GREEN)));

        if (!resident.isOnline())
            lore.add(Component.text("Last online ", NamedTextColor.DARK_GREEN).append(Component.text(Time.formatLastOnline(resident.getLastOnline()), NamedTextColor.GREEN)));

        return MenuItem.builder(Material.PLAYER_HEAD)
                .skullOwner(uuid)
                .name(Component.text(resident.getName(), resident.isOnline() ? NamedTextColor.GREEN : NamedTextColor.YELLOW))
                .lore(lore)
                .slot(slot)
                .build();
    }

    public static List<MenuItem> formatFriendsView(@NotNull Player player) {
        Resident resident = TownyAPI.getInstance().getResident(player);

        if (resident == null || resident.getFriends().size() == 0)
            return Collections.singletonList(MenuItem.builder(Material.BARRIER)
                    .name(Component.text("Error", NamedTextColor.RED))
                    .lore(Component.text("You do not have any friends to list.", NamedTextColor.GRAY))
                    .build());

        List<MenuItem> friends = new ArrayList<>();
        for (Resident friend : resident.getFriends())
            friends.add(formatResidentInfo(friend.getUUID(), 0));

        return friends;
    }
}