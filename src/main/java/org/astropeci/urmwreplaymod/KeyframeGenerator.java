package org.astropeci.urmwreplaymod;

import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.simplepathing.SPTimeline;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.popup.GuiInfoPopup;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableColor;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class KeyframeGenerator {

    private final SPTimeline timeline;
    private final ReplayHandler replayHandler;

    public boolean generate() {
        UUID spectatedUuid = ReplayModReplay.instance.getReplayHandler().getSpectatedUUID();

        if (spectatedUuid == null) {
            infoPopup("Spectate a player or manually specify keyframes");
            return false;
        }

        infoPopup("Not implemented");
        return false;
    }

    private void infoPopup(String text) {
        GuiInfoPopup.open(replayHandler.getOverlay(), new GuiLabel().setText(text).setColor(ReadableColor.BLACK));
    }
}
