package com.minelazz.treefeller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Material;

import java.io.*;
import java.util.HashMap;

public class PluginSettings {
    private static Gson gson = new GsonBuilder().enableComplexMapKeySerialization().setPrettyPrinting().create();

    public boolean mcMMOTreeFeller = false;
    public boolean instantTreeCut = false;
    public boolean instantToInventory = true;
    public boolean plantSapling = true;
    public boolean particles = true;

    public int maxLogBlocksPerCut = 90;
    public int delay = 0;

    public HashMap<Material, Integer> cuttingSpeed = new HashMap<>();


    public PluginSettings() {
        cuttingSpeed.put(Material.DIAMOND_AXE, 0);
        cuttingSpeed.put(Material.IRON_AXE, 1);
        cuttingSpeed.put(Material.GOLD_AXE, 1);
        cuttingSpeed.put(Material.STONE_AXE, 3);
        cuttingSpeed.put(Material.WOOD_AXE, 4);
    }

    public static void load() {
        File folder = TreeFeller.instance.getDataFolder(), settingsFile = new File(TreeFeller.instance.getDataFolder().getAbsolutePath() + "/settings.json");
        try {
            if (!folder.exists())
                folder.mkdirs();
            if (!settingsFile.exists())
                settingsFile.createNewFile();
            BufferedReader br = new BufferedReader(new FileReader(TreeFeller.instance.getDataFolder().getAbsolutePath() + "/settings.json"));
            TreeFeller.settings = gson.fromJson(br, PluginSettings.class);
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        if (TreeFeller.settings == null) {
            TreeFeller.settings = new PluginSettings();
        }
    }

    public void save() {
        String json = gson.toJson(this);
        try {
            FileWriter writer = new FileWriter(TreeFeller.instance.getDataFolder().getAbsolutePath() + "/settings.json");
            writer.write(json);
            writer.close();
        } catch (IOException e) {
            System.out.print("[EWG - TreeFeller] Something went wrong while saving the config file!");
            e.printStackTrace();
        }
    }
}
