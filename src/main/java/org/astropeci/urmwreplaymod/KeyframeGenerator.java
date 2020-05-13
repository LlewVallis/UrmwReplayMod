package org.astropeci.urmwreplaymod;

import com.replaymod.core.ReplayMod;
import com.replaymod.pathing.properties.SpectatorProperty;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.io.ReplayInputStream;
import com.replaymod.replaystudio.pathing.path.Keyframe;
import com.replaymod.replaystudio.pathing.path.Path;
import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.PacketType;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.protocol.ProtocolVersion;
import com.replaymod.replaystudio.us.myles.ViaVersion.packets.State;
import com.replaymod.simplepathing.SPTimeline;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.popup.GuiInfoPopup;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableColor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.RequiredArgsConstructor;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.PacketByteBuf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.UUID;

@RequiredArgsConstructor
public class KeyframeGenerator {

    private final SPTimeline timeline;
    private final ReplayHandler replayHandler;
    private final Logger logger = LogManager.getLogger();

    public boolean generate() {
        UUID spectatedUuid = ReplayModReplay.instance.getReplayHandler().getSpectatedUUID();

        if (spectatedUuid == null) {
            infoPopup("Spectate a player or manually specify keyframes");
            return false;
        }

        int startTime = findStartTime();
        int endTime = replayHandler.getReplayDuration();

        if (startTime > endTime) {
            startTime = 0;
        }

        PlayerEntity spectated = replayHandler.getCameraEntity().getEntityWorld().getPlayerByUuid(spectatedUuid);

        Path positionPath = timeline.getPositionPath();
        for (Keyframe keyframe : positionPath.getKeyframes()) {
            positionPath.remove(keyframe, false);
        }

        Path timePath = timeline.getTimePath();
        for (Keyframe keyframe : timePath.getKeyframes()) {
            timePath.remove(keyframe, false);
        }

        Keyframe positionStart = positionPath.insert(0);
        Keyframe positionEnd = positionPath.insert(endTime - startTime);

        positionStart.setValue(SpectatorProperty.PROPERTY, spectated.getEntityId());
        positionEnd.setValue(SpectatorProperty.PROPERTY, spectated.getEntityId());

        timeline.addTimeKeyframe(0, startTime);
        timeline.addTimeKeyframe(endTime - startTime, endTime);

        positionPath.updateAll();

        infoMessage("Generated keyframes");
        return true;
    }

    private int findStartTime() {
        try {
            ReplayFile file = replayHandler.getReplayFile();
            PacketTypeRegistry packetRegistry = PacketTypeRegistry.get(ProtocolVersion.v1_15_2, State.PLAY);

            ReplayInputStream input = file.getPacketData(packetRegistry);

            PacketData data;
            while ((data = input.readPacket()) != null) {
                Packet packet = data.getPacket();

                int capacity = packet.getBuf().capacity();
                byte[] bytes = new byte[capacity];
                packet.getBuf().getBytes(0, bytes);
                ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
                PacketByteBuf packetBuf = new PacketByteBuf(byteBuf);

                if (packet.getType() == PacketType.Chat) {
                    // Skip magic byte
                    packetBuf.readByte();

                    Text message = packetBuf.readText();
                    if (message.asFormattedString().contains("§bGame has started!")) {
                        return (int) data.getTime();
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return 0;
    }

    private void infoPopup(String text) {
        GuiInfoPopup.open(replayHandler.getOverlay(), new GuiLabel().setText(text).setColor(ReadableColor.BLACK));
    }

    private void infoMessage(String text) {
        ReplayMod.instance.printInfoToChat(text);
    }
}
