package io.github.schntgaispock.gastronomicon.core.slimefun.items.workstations.manual;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import net.guizhanss.guizhanlib.slimefuncn.utils.NewBlockStorageUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import io.github.schntgaispock.gastronomicon.Gastronomicon;
import io.github.schntgaispock.gastronomicon.core.slimefun.GastroGroups;
import io.github.schntgaispock.gastronomicon.util.NumberUtil;
import io.github.thebusybiscuit.slimefun4.api.events.BlockPlacerPlaceEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.handlers.SimpleBlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import lombok.Getter;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;

@Getter
@SuppressWarnings("deprecation")
public abstract class HuntingTrap extends SimpleSlimefunItem<BlockUseHandler> {

    private final Map<Location, Boolean> triggeredTraps = new HashMap<>();

    protected HuntingTrap(SlimefunItemStack item, ItemStack[] recipe) {
        super(GastroGroups.TOOLS, item, RecipeType.ENHANCED_CRAFTING_TABLE, recipe);
    }

    @Override
    public void preRegister() {
        super.preRegister();

        addItemHandler(new BlockPlaceHandler(true) {

            @Override
            public void onBlockPlacerPlace(BlockPlacerPlaceEvent e) {
                startCatch(e.getBlock().getLocation());
            }

            @Override
            public void onPlayerPlace(BlockPlaceEvent e) {
                startCatch(e.getBlock().getLocation());
            }

        });

        addItemHandler(new SimpleBlockBreakHandler() {
            @Override
            public void onBlockBreak(Block b) {
                Slimefun.getDatabaseManager().getBlockDataController().removeBlock(b.getLocation());
                if (triggeredTraps.containsKey(b.getLocation()) && triggeredTraps.get(b.getLocation()))
                    dropCatch(b.getLocation());
                triggeredTraps.remove(b.getLocation());
            }
        });

        addItemHandler(new BlockTicker() {
            
            private boolean active = true;

            @Override
            public void tick(Block b, SlimefunItem item, Config config) {
                if (!active) return;

                if (triggeredTraps.containsKey(b.getLocation())) {
                    if (triggeredTraps.get(b.getLocation())) {
                        b.getWorld().spawnParticle(
                            Particle.WAX_OFF,
                            b.getLocation().getX() + 0.5,
                            b.getLocation().getY() + 0.25,
                            b.getLocation().getZ() + 0.5,
                            4,
                            0.2, 0.05, 0.2, 0,
                            null,
                            true);
                    }
                } else {
                    active = startCatch(b.getLocation());
                }
            }

            @Override
            public boolean isSynchronized() {
                return false;
            }

        });
    }

    @Override
    public BlockUseHandler getItemHandler() {
        return e -> {
            e.cancel();
            if (e.getClickedBlock().isEmpty())
                return;
            final Location l = e.getClickedBlock().get().getLocation();
            if (triggeredTraps.containsKey(l) && triggeredTraps.get(l)) {
                dropCatch(l);
                startCatch(l);
            }
        };
    }

    protected abstract ItemStack getCatch(Location l);

    protected abstract boolean canCatch(Location l);

    private boolean startCatch(Location l) {
        triggeredTraps.put(l, false);
        if (!canCatch(l))
            return false;

        Gastronomicon.scheduleSyncDelayedTaskAtLocation(() -> {
            if (!NewBlockStorageUtil.hasBlock(l)) {
                return;
            }
            final String id = StorageCacheUtils.getBlock(l).getSfId();
            if (id.equals(getId())) {
                l.getWorld().playSound(l, Sound.ENTITY_EVOKER_FANGS_ATTACK, SoundCategory.BLOCKS, 1f, 1.5f);
                triggeredTraps.put(l, true);
            }
        }, 20 * (long) NumberUtil.clamp(ThreadLocalRandom.current().nextGaussian(120, 30), 60, 180), l);

        return true;
    }

    private void dropCatch(Location l) {
        l.getWorld().dropItemNaturally(l, getCatch(l));
    }
}
