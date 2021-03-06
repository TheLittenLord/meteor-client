package minegame159.meteorclient.modules;

import me.zero.alpine.listener.Listenable;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.gui.WidgetScreen;
import minegame159.meteorclient.gui.screens.ModuleScreen;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.Settings;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.ISerializable;
import minegame159.meteorclient.utils.NbtUtils;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.Objects;

public abstract class Module implements Listenable, ISerializable<Module> {
    protected final MinecraftClient mc;

    public final Category category;
    public final String name;
    public final String title;
    public final String description;
    public final Color color;

    public final Settings settings = new Settings();

    public boolean serialize = true;

    private int key = -1;

    public Module(Category category, String name, String description) {
        this.mc = MinecraftClient.getInstance();
        this.category = category;
        this.name = name;
        this.title = Utils.nameToTitle(name);
        this.description = description;
        this.color = Color.fromHsv(Utils.random(0.0, 360.0), 0.35, 1);
    }

    public WidgetScreen getScreen() {
        return new ModuleScreen(this);
    }

    public WWidget getWidget() {
        return null;
    }

    public void openScreen() {
        mc.openScreen(getScreen());
    }

    public void doAction(boolean onActivateDeactivate) {
        openScreen();
    }
    public void doAction() {
        doAction(true);
    }

    @Override
    public CompoundTag toTag() {
        if (!serialize) return null;
        CompoundTag tag = new CompoundTag();

        tag.putString("name", name);
        tag.putInt("key", key);
        tag.put("settings", NbtUtils.listToTag(settings));

        return tag;
    }

    @Override
    public Module fromTag(CompoundTag tag) {
        // General
        key = tag.getInt("key");

        // Settings
        ListTag settingsTag = tag.getList("settings", 10);
        for (Tag settingTagI : settingsTag) {
            CompoundTag settingTag = (CompoundTag) settingTagI;
            Setting<?> setting = settings.get(settingTag.getString("name"));
            if (setting != null) setting.fromTag(settingTag);
        }

        return this;
    }

    public void setKey(int key, boolean postEvent) {
        this.key = key;
        if (postEvent) MeteorClient.EVENT_BUS.post(EventStore.moduleBindChangedEvent(this));
    }
    public void setKey(int key) {
        setKey(key, true);
    }

    public int getKey() {
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Module module = (Module) o;
        return Objects.equals(name, module.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
