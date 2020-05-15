package org.astropeci.urmwreplaymod;

import com.replaymod.core.utils.Utils;
import com.replaymod.core.versions.MCVer;
import com.replaymod.replay.FullReplaySender;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replaystudio.replay.ZipReplayFile;
import com.replaymod.simplepathing.SPTimeline;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.popup.GuiInfoPopup;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableColor;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class Autonamer {

    private final ReplayHandler replayHandler;

    public Runnable createAction() {
        MinecraftClient mc = MCVer.getMinecraft();

        KeyframeGenerator.Span span = new KeyframeGenerator(replayHandler).getMatchSpan();
        // Add 10 seconds to the jump. This seems to fix an issue where the jump doesn't load instantly.
        replayHandler.doJump(span.getStart() + 10000, false);

        //mc.tick();

        List<AbstractClientPlayerEntity> players = mc.world.getPlayers();

        List<PlayerEntity> greenTeam = players.stream()
                .filter(player -> player.getTeamColorValue() == 5635925)
                .collect(Collectors.toList());

        List<PlayerEntity> redTeam = players.stream()
                .filter(player -> player.getTeamColorValue() == 16733525)
                .collect(Collectors.toList());

        String greenString = teamString(greenTeam);
        String redString = teamString(redTeam);

        String name = greenString + " vs " + redString;

        ZipReplayFile zip = (ZipReplayFile) replayHandler.getReplayFile();

        Field fileField;
        File sourceFile;
        try {
            fileField = ZipReplayFile.class.getDeclaredField("input");
            fileField.setAccessible(true);
            sourceFile = (File) fileField.get(zip);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

        return () -> {
            File targetFile = new File(sourceFile.getParentFile(), Utils.replayNameToFileName(name));

            try {
                if (sourceFile.getCanonicalFile().equals(targetFile.getCanonicalFile())) {
                    return;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            while (targetFile.exists()) {
                targetFile = new File(sourceFile.getParentFile(), Utils.replayNameToFileName(name));
            }

            try {
                FileUtils.moveFile(sourceFile, targetFile);

                String cacheSourceName = sourceFile.getName() + ".cache";
                String cacheTargetName = targetFile.getName() + ".cache";

                File cacheSourceFile = new File(sourceFile.getParent(), cacheSourceName);
                File cacheTargetFile = new File(targetFile.getParent(), cacheTargetName);

                FileUtils.moveDirectory(cacheSourceFile, cacheTargetFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private String teamString(List<PlayerEntity> players) {
        if (players.size() == 0) {
            return "___";
        }

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < players.size(); i++) {
            if (i != 0) {
                builder.append(", ");
            }

            builder.append(players.get(i).getDisplayName().getString());
        }

        return builder.toString();
    }

    public void infoPopup(String text) {
        GuiInfoPopup.open(ReplayModReplay.instance.getReplayHandler().getOverlay(), new GuiLabel().setText(text).setColor(ReadableColor.BLACK));
    }
}
