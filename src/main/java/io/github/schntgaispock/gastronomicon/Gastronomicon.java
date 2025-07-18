package io.github.schntgaispock.gastronomicon;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import net.guizhanss.guizhanlib.slimefun.addon.Scheduler;
import net.guizhanss.minecraft.guizhanlib.updater.GuizhanUpdater;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

import io.github.mooy1.infinitylib.core.AbstractAddon;
import io.github.mooy1.infinitylib.core.AddonConfig;
import io.github.schntgaispock.gastronomicon.api.trees.TreeStructure;
import io.github.schntgaispock.gastronomicon.core.setup.CommandSetup;
import io.github.schntgaispock.gastronomicon.core.setup.ListenerSetup;
import io.github.schntgaispock.gastronomicon.core.setup.ResearchSetup;
import io.github.schntgaispock.gastronomicon.core.setup.ItemSetup;
import io.github.schntgaispock.gastronomicon.integration.DynaTechSetup;
import io.github.schntgaispock.gastronomicon.integration.SlimeHUDSetup;
import io.github.schntgaispock.gastronomicon.util.StringUtil;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.logging.Level;

@Getter
public class Gastronomicon extends AbstractAddon {

    private static @Getter Gastronomicon instance;
    private static FoliaLib foliaLib;

    @Getter
    private Scheduler scheduler;
    private AddonConfig playerData;
    private AddonConfig customFood;

    public Gastronomicon() {
        super("SlimefunGuguProject", "Gastronomicon", "master", "options.auto-update");
    }

    @Override
    @SuppressWarnings("deprecation")
    public void enable() {
        foliaLib = new FoliaLib(this);
        instance = this;
//        scheduler = new Scheduler(this);

        if (!getServer().getPluginManager().isPluginEnabled("GuizhanLibPlugin")) {
            getLogger().log(Level.SEVERE, "本插件需要 鬼斩前置库插件(GuizhanLibPlugin) 才能运行!");
            getLogger().log(Level.SEVERE, "从此处下载: https://50L.cc/gzlib");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

//        if (getConfig().getBoolean("options.auto-update") && getPluginVersion().startsWith("Build")) {
//            GuizhanUpdater.start(this, getFile(), "SlimegunGuguProject", "Gastronomicon", "master");
//        }

        getLogger().info("#======================================#");
        getLogger().info("#    Gastronomicon by SchnTgaiSpock    #");
        getLogger().info("#   美食家    粘液科技简中汉化组汉化   #");
        getLogger().info("#======================================#");

        final Metrics metrics = new Metrics(this, 16941);

        metrics.addCustomChart(
            new SimplePie("exoticgardenInstalled", () -> Boolean.toString(isPluginEnabled("ExoticGarden"))));

        ItemSetup.setup();
        ResearchSetup.setup();
        ListenerSetup.setup();
        CommandSetup.setup();

        if (isPluginEnabled("SlimeHUD")) {
            try {
                info("检测到服务器已安装 SlimeHUD!");
                info("接入相关功能...");
                SlimeHUDSetup.setup();
            } catch (Exception e) {
                warn("该服务器安装的 SlimeHUD 版本不兼容");
                warn("请更新 SlimeHUD 至最新版本!");
            }
        }

        // If disable-exotic-garden-recipes is true "!" will change it to false and the rest of the code won't run.
        // If disable-exotic-garden-recipes is false "!" will change it to true and the rest of the code will run checking for ExoticGarden.

        if (!getConfig().getBoolean("disable-exotic-garden-recipes") && !isPluginEnabled("ExoticGarden")) {
            warn("检测到服务器未安装 异域花园(ExoticGarden)!");
            warn("需要异域花园物品的配方将被隐藏。");
        }

        if (isPluginEnabled("DynaTech") && !getConfig().getBoolean("disable-dynatech-integration")) {
            try {
                info("检测到服务器已安装 动力科技(DynaTech)!");
                info("正在向动力科技添加相关作物...");
                DynaTechSetup.setup();
            } catch (Exception e) {
                warn("该服务器安装的 DynaTech 版本不兼容");
                warn("请更新 DynaTech 至最新版本!");
            }
        }

        playerData = new AddonConfig("player.yml");
        customFood = new AddonConfig("custom-food.yml");

        TreeStructure.loadTrees();
    }

    @Override
    public void disable() {
        instance = null;
        if (getPlayerData() != null) {
            getPlayerData().save();
        }
    }

    public static NamespacedKey key(@Nonnull String name) {
        return new NamespacedKey(Gastronomicon.getInstance(), name);
    }

    public static boolean isPluginEnabled(String name) {
        return getInstance().getServer().getPluginManager().isPluginEnabled(name);
    }

    public static WrappedTask scheduleSyncDelayedTask(Runnable runnable, long delay) {
        return getFoliaLib().getScheduler().runLater(runnable, delay);
    }

    public static WrappedTask scheduleSyncRepeatingTask(Runnable runnable, long delay, long interval) {
        return getFoliaLib().getScheduler().runTimer(runnable, delay, interval);
    }

    public static boolean checkPermission(Player player, @Nonnull String permissionNode, @Nullable String message) {
        if (player.hasPermission(permissionNode)) {
            return true;
        }

        if (message != null)
            Gastronomicon.sendMessage(player, message);
        return false;

    }

    public static void info(String message) {
        getInstance().getLogger().info(message);
    }

    public static void warn(String message) {
        getInstance().getLogger().warning(message);
    }

    public static void error(String message) {
        getInstance().getLogger().severe(message);
    }

    public static void sendMessage(Player player, String message) {
        player.sendMessage(/* ChatColor.of("#c91df4") + "§l美食家§7§l> §7" + */ StringUtil.formatColors(message));
    }

    public static void sendMessage(Player player, Component message) {
        final Component text = Component.text()
            .content("美食家")
            .color(TextColor.color(0xc9, 0x1d, 0xf4))
            .decorate(TextDecoration.BOLD)
            .append(Component.text()
                .content(">")
                .color(TextColor.color(0xaa, 0xaa, 0xaa))
                .decorate(TextDecoration.BOLD)
                .appendSpace()
                .asComponent())
            .asComponent();
        player.sendMessage(text);
    }

    public static FoliaLib getFoliaLib() {
        return foliaLib;
    }

    public static WrappedTask scheduleSyncDelayedTaskAtLocation(Runnable runnable, long delay, Location location) {
        return getFoliaLib().getScheduler().runAtLocationLater(location, runnable, delay);
    }

    public static WrappedTask scheduleSyncRepeatingTaskAtLocation(Runnable runnable, long delay, long interval, Location location) {
        return getFoliaLib().getScheduler().runAtLocationTimer(location, runnable, delay, interval);
    }
}
