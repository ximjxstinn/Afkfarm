package de.ximjxstinn.afkfarm;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class AfkFarmClient implements ClientModInitializer {
    public static final String MOD_ID = "afkfarm";
    public static AfkConfig CONFIG;

    private static KeyBinding toggleKey;
    private static final AfkFarmController CONTROLLER = new AfkFarmController();

    @Override
    public void onInitializeClient() {
        CONFIG = AfkConfig.load();

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.afkfarm.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                KeyBinding.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                CONTROLLER.toggle(client);
            }
            CONTROLLER.tick(client);
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                AfkCommands.register(dispatcher, CONTROLLER));

        // Beim Verlassen des Servers automatisch ausschalten
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> CONTROLLER.disable(client, false));
    }
}
