package com.minelazz.treefeller.hooks.mcmmo;

import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.skills.SkillType;
import com.gmail.nossr50.skills.SkillManager;
import com.gmail.nossr50.util.player.UserManager;
import com.minelazz.treefeller.Utils;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.lang.reflect.Field;
import java.util.Map;


public class McMMOHook implements Listener {

    /**
     *
     * This method will inject a custom handler for woodcutting
     * into the mcMMO handler for a player.
     *
     * @param event fired if a block is broken on the server.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onMCMMOBlockBreakEvent(BlockBreakEvent event) {
        if (Utils.isWood(event.getBlock().getType())){
            return;
        }
        McMMOPlayer player = UserManager.getPlayer(event.getPlayer());

        try { //Nasty hook...
            Class clazz = player.getClass();
            Field field = clazz.getDeclaredField("skillManagers");
            field.setAccessible(true);
            Map<SkillType, SkillManager> skillMap = (Map<SkillType, SkillManager>) field.get(player);
            if (!(skillMap.get(SkillType.WOODCUTTING) instanceof WoodCutterHook)) {
                skillMap.remove(SkillType.WOODCUTTING);
                skillMap.put(SkillType.WOODCUTTING, new WoodCutterHook(player));
            }
        } catch (Exception e) {

        }

    }
}