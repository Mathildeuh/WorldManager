package fr.mathildeuh.worldmanager.util;

import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.MaterialData;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ItemBuilder {

    private ItemStack item;
    private ItemMeta meta;
    private Material material = Material.STONE;
    private int amount = 1;
    private MaterialData data;
    private short damage = 0;
    private Map<Enchantment, Integer> enchantments = new HashMap<>();
    private String displayname;
    private List<String> lore = new ArrayList<>();
    private List<ItemFlag> flags = new ArrayList<>();

    private boolean andSymbol = true;
    private boolean unsafeStackSize = false;


    public ItemBuilder(Material material) {
        if (material == null) {
            material = Material.AIR;
        }
        this.item = new ItemStack(material);
        this.material = material;
    }


    public ItemBuilder(Material material, int amount) {
        if (material == null) {
            material = Material.AIR;
        }
        if (((amount > material.getMaxStackSize()) || (amount <= 0)) && (!unsafeStackSize)) {
            amount = 1;
        }
        this.amount = amount;
        this.item = new ItemStack(material, amount);
        this.material = material;
    }


    public ItemBuilder(Material material, int amount, String displayname) {
        if (material == null) {
            material = Material.AIR;
        }
        this.item = new ItemStack(material, amount);
        this.material = material;
        if (((amount > material.getMaxStackSize()) || (amount <= 0)) && (!unsafeStackSize)) {
            amount = 1;
        }
        this.amount = amount;
        this.displayname = displayname;
    }


    public ItemBuilder(Material material, String displayname) {
        if (material == null) {
            material = Material.AIR;
        }
        this.item = new ItemStack(material);
        this.material = material;
        this.displayname = displayname;
    }


    public ItemBuilder(ItemStack item) {
        this.item = item;
        this.material = item.getType();
        this.amount = item.getAmount();
        this.data = item.getData();
        this.damage = item.getDurability();
        this.enchantments = item.getEnchantments();

        if (item.hasItemMeta()) {
            this.meta = item.getItemMeta();
            this.displayname = item.getItemMeta().getDisplayName();
            this.lore = item.getItemMeta().getLore();
            for (ItemFlag f : item.getItemMeta().getItemFlags()) {
                flags.add(f);
            }
        }
    }


    public ItemBuilder(FileConfiguration cfg, String path) {
        this(cfg.getItemStack(path));
    }


    @Deprecated
    public ItemBuilder(ItemBuilder builder) {
        this.item = builder.item;
        this.meta = builder.meta;
        this.material = builder.material;
        this.amount = builder.amount;
        this.damage = builder.damage;
        this.data = builder.data;
        this.damage = builder.damage;
        this.enchantments = builder.enchantments;
        this.displayname = builder.displayname;
        this.lore = builder.lore;
        this.flags = builder.flags;
    }

    public static void toConfig(FileConfiguration cfg, String path, ItemBuilder builder) {
        cfg.set(path, builder.build());
    }

    public static String toJson(ItemBuilder builder) {
        return new Gson().toJson(builder);
    }

    public static ItemBuilder fromJson(String json) {
        return new Gson().fromJson(json, ItemBuilder.class);
    }

    public ItemBuilder amount(int amount) {
        if (((amount > material.getMaxStackSize()) || (amount <= 0)) && (!unsafeStackSize))
            amount = 1;
        this.amount = amount;
        return this;
    }

    public ItemBuilder data(MaterialData data) {
        this.data = data;
        return this;
    }

    @Deprecated
    public ItemBuilder damage(short damage) {
        this.damage = damage;
        return this;
    }

    public ItemBuilder durability(short damage) {
        this.damage = damage;
        return this;
    }

    public ItemBuilder material(Material material) {
        this.material = material;
        return this;
    }

    public ItemBuilder meta(ItemMeta meta) {
        this.meta = meta;
        return this;
    }

    public ItemBuilder enchant(Enchantment enchant, int level) {
        enchantments.put(enchant, level);
        return this;
    }

    public ItemBuilder enchant(Map<Enchantment, Integer> enchantments) {
        this.enchantments = enchantments;
        return this;
    }

    public ItemBuilder name(String displayname) {
        this.displayname = andSymbol ? ChatColor.translateAlternateColorCodes('&', displayname) : displayname;
        return this;
    }

    public ItemBuilder lore(String line) {
        lore.add(andSymbol ? ChatColor.translateAlternateColorCodes('&', line) : line);
        return this;
    }

    public ItemBuilder lore(List<String> lore) {

        List<String> translatedLore = new ArrayList<>();
        for (String line : lore) {
            translatedLore.add(ChatColor.translateAlternateColorCodes('&', line));
        }

        this.lore = translatedLore;
        return this;
    }

    public ItemBuilder lore(String... lines) {
        for (String line : lines) {
            lore(andSymbol ? ChatColor.translateAlternateColorCodes('&', line) : line);
        }
        return this;
    }

    public ItemBuilder lore(String line, int index) {
        lore.set(index, andSymbol ? ChatColor.translateAlternateColorCodes('&', line) : line);
        return this;
    }

    public ItemBuilder flag(ItemFlag flag) {
        flags.add(flag);
        return this;
    }

    public ItemBuilder flag(List<ItemFlag> flags) {
        this.flags = flags;
        return this;
    }

    public ItemBuilder unbreakable(boolean unbreakable) {
        meta.setUnbreakable(unbreakable);
        return this;
    }

    public ItemBuilder glow() {
        enchant(material != Material.BOW ? Enchantment.INFINITY : Enchantment.CHANNELING, 1);
        flag(ItemFlag.HIDE_ENCHANTS);
        return this;
    }

    @Deprecated
    public ItemBuilder owner(String user) {
        if ((material == Material.LEGACY_SKULL_ITEM) || (material == Material.PLAYER_HEAD)) {
            SkullMeta smeta = (SkullMeta) meta;
            smeta.setOwner(user);
            meta = smeta;
        }
        return this;
    }

    @Deprecated
    public Unsafe unsafe() {
        return new Unsafe(this);
    }

    @Deprecated
    public ItemBuilder replaceAndSymbol() {
        replaceAndSymbol(!andSymbol);
        return this;
    }

    public ItemBuilder replaceAndSymbol(boolean replace) {
        andSymbol = replace;
        return this;
    }

    public ItemBuilder toggleReplaceAndSymbol() {
        replaceAndSymbol(!andSymbol);
        return this;
    }

    public ItemBuilder unsafeStackSize(boolean allow) {
        this.unsafeStackSize = allow;
        return this;
    }

    public ItemBuilder toggleUnsafeStackSize() {
        unsafeStackSize(!unsafeStackSize);
        return this;
    }

    public String getDisplayname() {
        return displayname;
    }

    public int getAmount() {
        return amount;
    }

    public Map<Enchantment, Integer> getEnchantments() {
        return enchantments;
    }

    @Deprecated
    public short getDamage() {
        return damage;
    }

    public short getDurability() {
        return damage;
    }

    public List<String> getLores() {
        return lore;
    }

    public boolean getAndSymbol() {
        return andSymbol;
    }

    public List<ItemFlag> getFlags() {
        return flags;
    }

    public Material getMaterial() {
        return material;
    }

    public ItemMeta getMeta() {
        return meta;
    }

    public MaterialData getData() {
        return data;
    }

    @Deprecated
    public List<String> getLore() {
        return lore;
    }

    public ItemBuilder toConfig(FileConfiguration cfg, String path) {
        cfg.set(path, build());
        return this;
    }

    public ItemBuilder fromConfig(FileConfiguration cfg, String path) {
        return new ItemBuilder(cfg, path);
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    public ItemBuilder applyJson(String json, boolean overwrite) {
        ItemBuilder b = new Gson().fromJson(json, ItemBuilder.class);
        if (overwrite)
            return b;
        if (b.displayname != null)
            displayname = b.displayname;
        if (b.data != null)
            data = b.data;
        if (b.material != null)
            material = b.material;
        if (b.lore != null)
            lore = b.lore;
        if (b.enchantments != null)
            enchantments = b.enchantments;
        if (b.item != null)
            item = b.item;
        if (b.flags != null)
            flags = b.flags;
        damage = b.damage;
        amount = b.amount;
        return this;
    }


    public ItemStack build() {
        item.setType(material);
        item.setAmount(amount);
        item.setDurability(damage);
        meta = item.getItemMeta();
        if (data != null) {
            item.setData(data);
        }
        if (enchantments.size() > 0) {
            item.addUnsafeEnchantments(enchantments);
        }
        if (displayname != null) {
            meta.setDisplayName(displayname);
        }
        if (lore.size() > 0) {
            meta.setLore(lore);
        }
        if (flags.size() > 0) {
            for (ItemFlag f : flags) {
                meta.addItemFlags(f);
            }
        }
        item.setItemMeta(meta);
        return item;
    }


    public class Unsafe {


        protected final ReflectionUtils utils = new ReflectionUtils();


        protected final ItemBuilder builder;


        public Unsafe(ItemBuilder builder) {
            this.builder = builder;
        }


        public Unsafe setString(String key, String value) {
            builder.item = utils.setString(builder.item, key, value);
            return this;
        }


        public String getString(String key) {
            return utils.getString(builder.item, key);
        }


        public Unsafe setInt(String key, int value) {
            builder.item = utils.setInt(builder.item, key, value);
            return this;
        }


        public int getInt(String key) {
            return utils.getInt(builder.item, key);
        }


        public Unsafe setDouble(String key, double value) {
            builder.item = utils.setDouble(builder.item, key, value);
            return this;
        }


        public double getDouble(String key) {
            return utils.getDouble(builder.item, key);
        }


        public Unsafe setBoolean(String key, boolean value) {
            builder.item = utils.setBoolean(builder.item, key, value);
            return this;
        }


        public boolean getBoolean(String key) {
            return utils.getBoolean(builder.item, key);
        }


        public boolean containsKey(String key) {
            return utils.hasKey(builder.item, key);
        }


        public ItemBuilder builder() {
            return builder;
        }


        public class ReflectionUtils {

            public String getString(ItemStack item, String key) {
                Object compound = getNBTTagCompound(getItemAsNMSStack(item));
                if (compound == null) {
                    compound = getNewNBTTagCompound();
                }
                try {
                    return (String) compound.getClass().getMethod("getString", String.class).invoke(compound, key);
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
                    ex.printStackTrace();
                }
                return null;
            }

            public ItemStack setString(ItemStack item, String key, String value) {
                Object nmsItem = getItemAsNMSStack(item);
                Object compound = getNBTTagCompound(nmsItem);
                if (compound == null) {
                    compound = getNewNBTTagCompound();
                }
                try {
                    compound.getClass().getMethod("setString", String.class, String.class).invoke(compound, key, value);
                    nmsItem = setNBTTag(compound, nmsItem);
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
                    ex.printStackTrace();
                }
                return getItemAsBukkitStack(nmsItem);
            }

            public int getInt(ItemStack item, String key) {
                Object compound = getNBTTagCompound(getItemAsNMSStack(item));
                if (compound == null) {
                    compound = getNewNBTTagCompound();
                }
                try {
                    return (Integer) compound.getClass().getMethod("getInt", String.class).invoke(compound, key);
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
                    ex.printStackTrace();
                }
                return -1;
            }

            public ItemStack setInt(ItemStack item, String key, int value) {
                Object nmsItem = getItemAsNMSStack(item);
                Object compound = getNBTTagCompound(nmsItem);
                if (compound == null) {
                    compound = getNewNBTTagCompound();
                }
                try {
                    compound.getClass().getMethod("setInt", String.class, Integer.class).invoke(compound, key, value);
                    nmsItem = setNBTTag(compound, nmsItem);
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
                    ex.printStackTrace();
                }
                return getItemAsBukkitStack(nmsItem);
            }

            public double getDouble(ItemStack item, String key) {
                Object compound = getNBTTagCompound(getItemAsNMSStack(item));
                if (compound == null) {
                    compound = getNewNBTTagCompound();
                }
                try {
                    return (Double) compound.getClass().getMethod("getDouble", String.class).invoke(compound, key);
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
                    ex.printStackTrace();
                }
                return Double.NaN;
            }

            public ItemStack setDouble(ItemStack item, String key, double value) {
                Object nmsItem = getItemAsNMSStack(item);
                Object compound = getNBTTagCompound(nmsItem);
                if (compound == null) {
                    compound = getNewNBTTagCompound();
                }
                try {
                    compound.getClass().getMethod("setDouble", String.class, Double.class).invoke(compound, key, value);
                    nmsItem = setNBTTag(compound, nmsItem);
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
                    ex.printStackTrace();
                }
                return getItemAsBukkitStack(nmsItem);
            }

            public boolean getBoolean(ItemStack item, String key) {
                Object compound = getNBTTagCompound(getItemAsNMSStack(item));
                if (compound == null) {
                    compound = getNewNBTTagCompound();
                }
                try {
                    return (Boolean) compound.getClass().getMethod("getBoolean", String.class).invoke(compound, key);
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
                    ex.printStackTrace();
                }
                return false;
            }

            public ItemStack setBoolean(ItemStack item, String key, boolean value) {
                Object nmsItem = getItemAsNMSStack(item);
                Object compound = getNBTTagCompound(nmsItem);
                if (compound == null) {
                    compound = getNewNBTTagCompound();
                }
                try {
                    compound.getClass().getMethod("setBoolean", String.class, Boolean.class).invoke(compound, key,
                            value);
                    nmsItem = setNBTTag(compound, nmsItem);
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
                    ex.printStackTrace();
                }
                return getItemAsBukkitStack(nmsItem);
            }

            public boolean hasKey(ItemStack item, String key) {
                Object compound = getNBTTagCompound(getItemAsNMSStack(item));
                if (compound == null) {
                    compound = getNewNBTTagCompound();
                }
                try {
                    return (Boolean) compound.getClass().getMethod("hasKey", String.class).invoke(compound, key);
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    e.printStackTrace();
                }
                return false;
            }

            public Object getNewNBTTagCompound() {
                String ver = Bukkit.getServer().getClass().getPackage().getName().split(".")[3];
                try {
                    return Class.forName("net.minecraft.server." + ver + ".NBTTagCompound").newInstance();
                } catch (ClassNotFoundException | IllegalAccessException | InstantiationException ex) {
                    ex.printStackTrace();
                }
                return null;
            }

            public Object setNBTTag(Object tag, Object item) {
                try {
                    item.getClass().getMethod("setTag", item.getClass()).invoke(item, tag);
                    return item;
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
                    ex.printStackTrace();
                }
                return null;
            }

            public Object getNBTTagCompound(Object nmsStack) {
                try {
                    return nmsStack.getClass().getMethod("getTag").invoke(nmsStack);
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
                    ex.printStackTrace();
                }
                return null;
            }

            public Object getItemAsNMSStack(ItemStack item) {
                try {
                    Method m = getCraftItemStackClass().getMethod("asNMSCopy", ItemStack.class);
                    return m.invoke(getCraftItemStackClass(), item);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                    ex.printStackTrace();
                }
                return null;
            }

            public ItemStack getItemAsBukkitStack(Object nmsStack) {
                try {
                    Method m = getCraftItemStackClass().getMethod("asCraftMirror", nmsStack.getClass());
                    return (ItemStack) m.invoke(getCraftItemStackClass(), nmsStack);
                } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {
                    ex.printStackTrace();
                }
                return null;
            }

            public Class<?> getCraftItemStackClass() {
                String ver = Bukkit.getServer().getClass().getPackage().getName().split(".")[3];
                try {
                    return Class.forName("org.bukkit.craftbukkit." + ver + ".inventory.CraftItemStack");
                } catch (ClassNotFoundException ex) {
                    ex.printStackTrace();
                }
                return null;
            }
        }
    }
}
