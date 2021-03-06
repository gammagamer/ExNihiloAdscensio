package exnihiloadscensio.registries;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import exnihiloadscensio.blocks.ENBlocks;
import exnihiloadscensio.json.CustomItemStackJson;
import exnihiloadscensio.util.ItemInfo;
import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

public class HammerRegistry
{
    @Getter
    private static HashMap<ItemInfo, List<HammerReward>> registry = new HashMap<ItemInfo, List<HammerReward>>();
    private static Map<ItemInfo, List<HammerReward>> externalRegistry = new HashMap<>();
    
    private static Gson gson = new GsonBuilder().setPrettyPrinting().registerTypeAdapter(ItemStack.class, new CustomItemStackJson()).create();
    
    public static void loadJson(File file)
    {
        registry.clear();
        
        if (file.exists())
        {
            try
            {
                FileReader fr = new FileReader(file);
                HashMap<String, ArrayList<HammerReward>> gsonInput = gson.fromJson(fr, new TypeToken<HashMap<String, ArrayList<HammerReward>>>(){}.getType());
                
                Iterator<String> it = gsonInput.keySet().iterator();
                
                while (it.hasNext())
                {
                    String s = (String) it.next();
                    ItemInfo stack = new ItemInfo(s);
                    registry.put(stack, gsonInput.get(s));
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            registerDefaults();
            saveJson(file);
        }
        
        for (Map.Entry<ItemInfo, List<HammerReward>> entry : externalRegistry.entrySet())
        {
            List<HammerReward> rewards = registry.get(entry.getKey());
            
            if (rewards == null)
            {
                rewards = new ArrayList<>();
            }
            
            rewards.addAll(entry.getValue());
            registry.put(entry.getKey(), rewards);
        }
    }
    
    public static void saveJson(File file)
    {
        try
        {
            FileWriter fw = new FileWriter(file);
            
            gson.toJson(registry, fw);
            
            fw.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    @Deprecated
    /**
     * Use register instead
     */
    public static void addHammerRecipe(IBlockState state, ItemStack reward, int miningLevel, float chance, float fortuneChance)
    {
        register(state, reward, miningLevel, chance, fortuneChance);
    }
    
    /**
     * Adds a new Hammer recipe for use with Ex Nihilo hammers.
     * 
     * @param state
     *            Block State
     * @param reward
     *            Reward
     * @param miningLevel
     *            Mining level of hammer. 0 = Wood/Gold, 1 = Stone, 2 = Iron, 3 = Diamond. Can be higher, but will need corresponding tool material.
     * @param chance
     *            Chance of drop
     * @param fortuneChance
     *            Chance of drop per level of fortune
     */
    public static void register(IBlockState state, ItemStack reward, int miningLevel, float chance, float fortuneChance)
    {
        registerInternal(state, reward, miningLevel, chance, fortuneChance);
        
        ItemInfo key = new ItemInfo(state);
        
        List<HammerReward> rewards = externalRegistry.get(key);
        
        if (rewards == null)
        {
            rewards = new ArrayList<>();
        }
        
        rewards.add(new HammerReward(reward, miningLevel, chance, fortuneChance));
        externalRegistry.put(key, rewards);
    }
    
    private static void registerInternal(IBlockState state, ItemStack reward, int miningLevel, float chance, float fortuneChance)
    {
        ItemInfo key = new ItemInfo(state);
        
        List<HammerReward> rewards = registry.get(key);
        
        if (rewards == null)
        {
            rewards = new ArrayList<>();
        }
        
        rewards.add(new HammerReward(reward, miningLevel, chance, fortuneChance));
        registry.put(key, rewards);
    }
    
    public static List<ItemStack> getRewardDrops(Random random, IBlockState block, int miningLevel, int fortuneLevel)
    {
        List<ItemStack> rewards = new ArrayList<ItemStack>();
        
        for (HammerReward reward : getRewards(block))
        {
            if (miningLevel >= reward.getMiningLevel())
            {
                if (random.nextFloat() <= reward.getChance() + (reward.getFortuneChance() * fortuneLevel))
                {
                    rewards.add(reward.getStack().copy());
                }
            }
        }
        
        return rewards;
    }
    
    public static List<HammerReward> getRewards(IBlockState block)
    {
        return registry.getOrDefault(new ItemInfo(block), Collections.EMPTY_LIST);
    }
    
    public static boolean registered(Block block)
    {
        return registry.containsKey(new ItemInfo(block.getDefaultState()));
    }
    
    public static void registerDefaults()
    {
        registerInternal(Blocks.COBBLESTONE.getDefaultState(), new ItemStack(Blocks.GRAVEL, 1), 0, 1.0F, 0.0F);
        registerInternal(Blocks.GRAVEL.getDefaultState(), new ItemStack(Blocks.SAND, 1), 0, 1.0F, 0.0F);
        registerInternal(Blocks.SAND.getDefaultState(), new ItemStack(ENBlocks.dust, 1), 0, 1.0F, 0.0F);
        registerInternal(Blocks.NETHERRACK.getDefaultState(), new ItemStack(ENBlocks.netherrackCrushed, 1), 0, 1.0F, 0.0F);
        registerInternal(Blocks.END_STONE.getDefaultState(), new ItemStack(ENBlocks.endstoneCrushed, 1), 0, 1.0F, 0.0F);
    }
    
    // Legacy
    @Deprecated
    public static ArrayList<HammerReward> getRewards(IBlockState state, int miningLevel)
    {
        List<HammerReward> mapList = registry.getOrDefault(new ItemInfo(state), Collections.EMPTY_LIST);
        ArrayList<HammerReward> ret = new ArrayList<HammerReward>();
        
        for (HammerReward reward : mapList)
        {
            if (reward.getMiningLevel() <= miningLevel)
                ret.add(reward);
        }
        
        return ret;
    }
}
