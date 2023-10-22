package makeo.gadomancy.common.familiar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import baubles.api.BaublesApi;
import makeo.gadomancy.common.registration.RegisteredFamiliarAI_Old;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by HellFirePvP @ 31.10.2015 14:08
 */
public class FamiliarAIController_Old {

    private static final Random RAND = new Random();

    private static Map<EntityPlayer, LinkedList<EntityLivingBase>> targetMap = new HashMap<EntityPlayer, LinkedList<EntityLivingBase>>();

    private List<FamiliarAIProcess_Old> availableTasks = new ArrayList<FamiliarAIProcess_Old>();
    private Map<FamiliarAIProcess_Old, Integer> cooldownProcesses = new HashMap<FamiliarAIProcess_Old, Integer>();
    private LinkedList<FamiliarAIProcess_Old> requestedLoop = new LinkedList<FamiliarAIProcess_Old>();
    private FamiliarAIProcess_Old runningTask;
    private int ticksInTask;
    private EntityPlayer owningPlayer;

    public FamiliarAIController_Old(EntityPlayer owningPlayer) {
        this.owningPlayer = owningPlayer;
    }

    public void registerDefaultTasks() {
        this.availableTasks.add(RegisteredFamiliarAI_Old.familiarAIIdle);
        this.availableTasks.add(RegisteredFamiliarAI_Old.familiarAIZapAttackingMonsters);
    }

    public void scheduleTick() {
        this.reduceRunningCooldowns();
        if (this.runningTask == null) {
            this.selectNewTask();
        } else {
            ItemStack famStack = BaublesApi.getBaubles(this.owningPlayer).getStackInSlot(0);
            this.runningTask.tick(this.ticksInTask, this.owningPlayer.worldObj, this.owningPlayer, famStack);
            this.ticksInTask++;
            if (this.ticksInTask >= this.runningTask.getDuration()) {
                if (this.runningTask.getCooldownDuration(famStack) > 0)
                    this.cooldownProcesses.put(this.runningTask, this.runningTask.getCooldownDuration(famStack));
                if (this.runningTask.tryLoop() && !this.requestedLoop.contains(this.runningTask))
                    this.requestedLoop.addLast(this.runningTask);
                this.runningTask = null;
                this.ticksInTask = 0;
            }
        }
    }

    private void selectNewTask() {
        if (!this.requestedLoop.isEmpty()) {
            Iterator<FamiliarAIProcess_Old> it = this.requestedLoop.iterator();
            while (it.hasNext()) {
                FamiliarAIProcess_Old process = it.next();
                if (!this.cooldownProcesses.containsKey(process)) {
                    this.runningTask = process;
                    it.remove();
                    return;
                }
            }
        }
        int size = this.availableTasks.size();
        int randIndex = FamiliarAIController_Old.RAND.nextInt(size);
        for (int i = 0; i < size; i++) {
            int index = (randIndex + i) % size;
            FamiliarAIProcess_Old process = this.availableTasks.get(index);
            if (process.canRun(
                    this.owningPlayer.worldObj,
                    this.owningPlayer.posX,
                    this.owningPlayer.posY,
                    this.owningPlayer.posZ,
                    this.owningPlayer,
                    BaublesApi.getBaubles(this.owningPlayer).getStackInSlot(0))
                    && !this.cooldownProcesses.containsKey(process)) {
                this.runningTask = process;
            }
        }
    }

    private void reduceRunningCooldowns() {
        Iterator<FamiliarAIProcess_Old> itProcesses = this.cooldownProcesses.keySet().iterator();
        while (itProcesses.hasNext()) {
            FamiliarAIProcess_Old process = itProcesses.next();
            int cd = this.cooldownProcesses.get(process) - 1;
            if (cd <= 0) {
                itProcesses.remove();
            } else {
                this.cooldownProcesses.put(process, cd);
            }
        }
    }

    public EntityPlayer getOwningPlayer() {
        return this.owningPlayer;
    }

    public static boolean hasLastTargetter(EntityPlayer player) {
        return FamiliarAIController_Old.targetMap.containsKey(player)
                && FamiliarAIController_Old.targetMap.get(player).size() >= 1;
    }

    public static LinkedList<EntityLivingBase> getLastTargetters(EntityPlayer player) {
        if (!FamiliarAIController_Old.targetMap.containsKey(player)) return null;
        if (FamiliarAIController_Old.targetMap.get(player).size() < 1) return null;
        return FamiliarAIController_Old.targetMap.get(player);
    }

    public static void cleanTargetterList(EntityPlayer player) {
        if (!FamiliarAIController_Old.targetMap.containsKey(player)) return;
        Iterator<EntityLivingBase> it = FamiliarAIController_Old.targetMap.get(player).iterator();
        while (it.hasNext()) {
            EntityLivingBase living = it.next();
            if (living.isDead) it.remove();
        }
    }

    public static void notifyTargetEvent(EntityLivingBase targetter, EntityPlayer targetted) {
        LinkedList<EntityLivingBase> targetters;
        boolean needsUpdate = false;
        if (FamiliarAIController_Old.targetMap.containsKey(targetted)) {
            targetters = FamiliarAIController_Old.targetMap.get(targetted);
        } else {
            targetters = new LinkedList<EntityLivingBase>();
            needsUpdate = true;
        }

        if (!targetters.contains(targetter)) {
            targetters.addLast(targetter);
        }
        if (needsUpdate) {
            FamiliarAIController_Old.targetMap.put(targetted, targetters);
        }
    }
}
