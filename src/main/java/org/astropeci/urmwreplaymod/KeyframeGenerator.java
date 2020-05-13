package org.astropeci.urmwreplaymod;

import com.replaymod.core.ReplayMod;
import com.replaymod.pathing.properties.SpectatorProperty;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replaystudio.pathing.path.Keyframe;
import com.replaymod.replaystudio.pathing.path.Path;
import com.replaymod.simplepathing.SPTimeline;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.popup.GuiInfoPopup;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableColor;
import lombok.RequiredArgsConstructor;
import net.minecraft.entity.player.PlayerEntity;

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

        PlayerEntity spectated = replayHandler.getCameraEntity().getEntityWorld().getPlayerByUuid(spectatedUuid);

        Path positionPath = timeline.getPositionPath();

        Keyframe positionStart = positionPath.insert(0);
        Keyframe positionEnd = positionPath.insert(replayHandler.getReplayDuration());

        positionStart.setValue(SpectatorProperty.PROPERTY, spectated.getEntityId());
        positionEnd.setValue(SpectatorProperty.PROPERTY, spectated.getEntityId());

        timeline.addTimeKeyframe(0, 0);
        timeline.addTimeKeyframe(replayHandler.getReplayDuration(), replayHandler.getReplayDuration());

        infoMessage("Generated keyframes");
        return true;
    }

    private void infoPopup(String text) {
        GuiInfoPopup.open(replayHandler.getOverlay(), new GuiLabel().setText(text).setColor(ReadableColor.BLACK));
    }

    private void infoMessage(String text) {
        ReplayMod.instance.printInfoToChat(text);
    }
}
