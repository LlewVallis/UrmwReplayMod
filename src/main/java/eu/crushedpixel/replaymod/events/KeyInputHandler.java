package eu.crushedpixel.replaymod.events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.entities.CameraEntity.MoveDirection;
import eu.crushedpixel.replaymod.gui.elements.GuiMouseInput;
import eu.crushedpixel.replaymod.registry.KeybindRegistry;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.replay.spectate.SpectateHandler;
import eu.crushedpixel.replaymod.video.ReplayScreenshot;

public class KeyInputHandler {

	private Minecraft mc = Minecraft.getMinecraft();

	@SubscribeEvent
	public void onKeyInput(KeyInputEvent event) {

		if(!ReplayHandler.isInReplay()) return;
		if(mc.currentScreen != null) {
			return;
		}
		
		boolean found = false;
		
		KeyBinding[] keyBindings = Minecraft.getMinecraft().gameSettings.keyBindings;
		for(KeyBinding kb : keyBindings) {
			if(!kb.isKeyDown()) {
				continue;
			}
			try {
				boolean speedup = false;
				
				if(ReplayHandler.isCamera()) {
					if(kb.getKeyDescription().equals("key.forward")) {
						ReplayHandler.getCameraEntity().setMovement(MoveDirection.FORWARD);
						speedup = true;
					}

					if(kb.getKeyDescription().equals("key.back")) {
						ReplayHandler.getCameraEntity().setMovement(MoveDirection.BACKWARD);
						speedup = true;
					}

					if(kb.getKeyDescription().equals("key.jump")) {
						ReplayHandler.getCameraEntity().setMovement(MoveDirection.UP);
						speedup = true;
					}

					if(kb.getKeyDescription().equals("key.left")) {
						ReplayHandler.getCameraEntity().setMovement(MoveDirection.LEFT);
						speedup = true;
					}

					if(kb.getKeyDescription().equals("key.right")) {
						ReplayHandler.getCameraEntity().setMovement(MoveDirection.RIGHT);
						speedup = true;
					}
				}
				if(kb.getKeyDescription().equals("key.sneak")) {
					if(ReplayHandler.isCamera()) {
						ReplayHandler.getCameraEntity().setMovement(MoveDirection.DOWN);
						speedup = true;
					} else {
						ReplayHandler.spectateCamera();
					}
				}
				
				if(speedup) {
					ReplayHandler.getCameraEntity().speedUp();
				}

				if(kb.getKeyDescription().equals("key.chat")) {
					mc.displayGuiScreen(new GuiMouseInput());
					break;
				}

				//Custom registered handlers
				if(kb.getKeyDescription().equals(KeybindRegistry.KEY_THUMBNAIL) && kb.isPressed() && !found) {
					System.out.println("thumbnail key pressed");
					ReplayScreenshot.prepareScreenshot();
					GuiReplayOverlay.requestScreenshot();
				}

				if(kb.getKeyDescription().equals(KeybindRegistry.KEY_SPECTATE) && kb.isPressed() && !found) {
					SpectateHandler.openSpectateSelection();
				}

				if(kb.getKeyDescription().equals(KeybindRegistry.KEY_LIGHTING) && kb.isPressed() && !found) {
					ReplayMod.replaySettings.setLightingEnabled(!ReplayMod.replaySettings.isLightingEnabled());			
				}

			} catch(Exception e) {
				e.printStackTrace();
			}
			found = true;
		}
	}
}
