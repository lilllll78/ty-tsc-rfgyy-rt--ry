package fr.chestesp;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class EspMenuScreen extends Screen {

    public EspMenuScreen() {
        super(Text.literal("Chest ESP - Menu"));
    }

    @Override
    protected void init() {
        int w = 200, h = 20;
        int x = this.width / 2 - w / 2;
        int y = this.height / 2 - 55;

        addToggle(x, y, w, h, "Chest ESP", () -> Config.chestEsp, v -> Config.chestEsp = v);
        addToggle(x, y + 25, w, h, "Anchor ESP", () -> Config.anchorEsp, v -> Config.anchorEsp = v);
        addToggle(x, y + 50, w, h, "Lignes (tracers)", () -> Config.tracers, v -> Config.tracers = v);
        addToggle(x, y + 75, w, h, "Anchor AutoFarm", () -> Config.anchorFarm, v -> Config.anchorFarm = v);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Fermer"), b -> this.close())
                .dimensions(x, y + 110, w, h).build());
    }

    private void addToggle(int x, int y, int w, int h, String name, BooleanSupplier get, Consumer<Boolean> set) {
        this.addDrawableChild(ButtonWidget.builder(label(name, get.getAsBoolean()), b -> {
            set.accept(!get.getAsBoolean());
            b.setMessage(label(name, get.getAsBoolean()));
        }).dimensions(x, y, w, h).build());
    }

    private static Text label(String name, boolean on) {
        return Text.literal(name + " : " + (on ? "§aON" : "§cOFF"));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ChestEspClient.menuKey != null && ChestEspClient.menuKey.matchesKey(keyCode, scanCode)) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 85, 0xFFFFFF);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
