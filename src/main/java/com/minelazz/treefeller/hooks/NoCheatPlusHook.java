package com.minelazz.treefeller.hooks;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.access.IViolationInfo;
import fr.neatmonster.nocheatplus.hooks.NCPHook;
import fr.neatmonster.nocheatplus.hooks.NCPHookManager;
import org.bukkit.entity.Player;

import static com.minelazz.treefeller.TreeFeller.currentFellers;

public class NoCheatPlusHook {

    public static void addNCPSupport() {
        NCPHookManager.addHook(CheckType.BLOCKBREAK, new NCPHook() {
            @Override
            public String getHookName() {
                return "EWG-TreeFeller";
            }

            @Override
            public String getHookVersion() {
                return "1.0";
            }

            @Override
            public boolean onCheckFailure(CheckType checkType, Player player, IViolationInfo iViolationInfo) {
                return (currentFellers.contains(player.getUniqueId()));
            }
        });
        System.out.print("TreeFeller - Added NoCheatPlus support");
    }

}
