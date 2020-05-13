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
import lombok.Value;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.PacketByteBuf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class KeyframeGenerator {

    private final SPTimeline timeline;
    private final ReplayHandler replayHandler;
    private final Logger logger = LogManager.getLogger();

    @Value
    private static class Span {
        int start;
        int end;
    }

    @Value
    private static class Event {
        int time;
        Type type;

        public enum Type {
            START, END
        }
    }

    public boolean generate() {
        UUID spectatedUuid = ReplayModReplay.instance.getReplayHandler().getSpectatedUUID();

        if (spectatedUuid == null) {
            infoPopup("Spectate a player or manually specify keyframes");
            return false;
        }

        int duration = replayHandler.getReplayDuration();

        List<Integer> startTimes = findStartTimes();
        List<Integer> endTimes = findEndTimes(duration);

        List<Event> events = new ArrayList<>();
        events.addAll(startTimes.stream().map(time -> new Event(time, Event.Type.START)).collect(Collectors.toList()));
        events.addAll(endTimes.stream().map(time -> new Event(time, Event.Type.END)).collect(Collectors.toList()));
        events.sort(Comparator.comparingInt(event -> event.time));

        List<Span> spans = new ArrayList<>();
        int lastStartTime = 0;
        for (Event event : events) {
            if (event.type == Event.Type.START) {
                lastStartTime = event.time;
            } else {
                Span span = new Span(lastStartTime, event.time);
                spans.add(span);
                lastStartTime = event.time;
            }
        }

        Span longestSpan = spans.stream()
                .max(Comparator.comparingInt(span -> span.end - span.start))
                // Shouldn't ever need, but its here if we do.
                .orElse(new Span(0, duration));

        int startTime = longestSpan.start;
        int endTime = longestSpan.end + 10000;

        if (endTime > duration) {
            endTime = duration;
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

    private List<Integer> findStartTimes() {
        List<Integer> result = new ArrayList<>();
        result.add(0);

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
                        result.add((int) data.getTime());
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    private List<Integer> findEndTimes(int duration) {
        List<Integer> result = new ArrayList<>();

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

                // Title packet has ID 0x50
                if (packet.getId() == 0x50) {
                    // Skip magic byte
                    packetBuf.readByte();

                    int action = packetBuf.readVarInt();

                    // Standard title
                    if (action == 0) {
                        Text message = packetBuf.readText();
                        if (message.asFormattedString().contains("Team Wins!")) {
                            result.add((int) data.getTime());
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (!result.contains(duration)) {
            result.add(duration);
        }

        return result;
    }

    private void infoPopup(String text) {
        GuiInfoPopup.open(replayHandler.getOverlay(), new GuiLabel().setText(text).setColor(ReadableColor.BLACK));
    }

    private void infoMessage(String text) {
        ReplayMod.instance.printInfoToChat(text);
    }
}
