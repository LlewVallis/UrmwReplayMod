//#if MC>=10800
package com.replaymod.render.hooks;

import net.minecraft.client.render.WorldRenderer;

import java.lang.reflect.Field;

//#if MC>=11500
//#else
//$$ import com.replaymod.render.mixin.ChunkRenderDispatcherAccessor;
//$$ import com.replaymod.render.mixin.WorldRendererAccessor;
//$$ import com.replaymod.render.utils.JailingQueue;
//$$ import net.minecraft.client.render.chunk.BlockBufferBuilderStorage;
//$$ import net.minecraft.client.render.chunk.ChunkBuilder;
//$$ import net.minecraft.client.render.chunk.ChunkRenderTask;
//$$ import net.minecraft.client.render.chunk.ChunkRenderWorker;
//$$ import net.minecraft.client.render.chunk.ChunkRenderer;
//$$ import java.util.Iterator;
//$$ import static com.replaymod.core.versions.MCVer.*;
//#endif

//#if MC>=10904
import java.util.concurrent.PriorityBlockingQueue;
//#else
//$$ import java.util.concurrent.BlockingQueue;
//#endif

public class ChunkLoadingRenderGlobal {

    //#if MC>=11400
    private final WorldRenderer hooked;
    //#else
    //$$ private final RenderGlobal hooked;
    //#endif

    //#if MC>=11500
    //#else
    //$$ private ChunkBuilder renderDispatcher;
    //#if MC>=11400
    //$$ private JailingQueue<ChunkRenderTask> workerJailingQueue;
    //#else
    //$$ private JailingQueue<ChunkCompileTaskGenerator> workerJailingQueue;
    //#endif
    //$$ private ChunkRenderWorkerAccessor renderWorker;
    //$$ private int frame;
    //#endif

    public ChunkLoadingRenderGlobal(
            //#if MC>=11400
            WorldRenderer renderGlobal
            //#else
            //$$ RenderGlobal renderGlobal
            //#endif
    ) {
        this.hooked = renderGlobal;

        //#if MC>=11500
        install();
        //#else
        //$$ setup(((WorldRendererAccessor) renderGlobal).getRenderDispatcher());
        //$$ install();
        //#endif
    }

    private void install() {
        //#if FABRIC>=1
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("sodium")) {
            throw new UnsupportedOperationException(
                    "Rendering is not currently supported while Sodium is installed.\n" +
                            "See https://github.com/ReplayMod/ReplayMod/issues/150\n" +
                            "For now, you need to uninstall Sodium before rendering!"
            );
        }
        //#endif
        try {
            //#if MC>=11400
            Field hookField = WorldRenderer.class.getField("replayModRender_hook");
            //#else
            //$$ Field hookField = RenderGlobal.class.getField("replayModRender_hook");
            //#endif
            hookField.set(hooked, this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new Error(e);
        }
    }

    //#if MC>=11500
    //#else
    //$$ public void updateRenderDispatcher(ChunkBuilder renderDispatcher) {
    //$$     if (this.renderDispatcher != null) {
    //$$         workerJailingQueue.freeAll();
    //$$         this.renderDispatcher = null;
    //$$     }
    //$$     if (renderDispatcher != null) {
    //$$         setup(renderDispatcher);
    //$$     }
    //$$ }
    //$$
    //$$ private void setup(ChunkBuilder renderDispatcher) {
    //$$     this.renderDispatcher = renderDispatcher;
    //$$     this.renderWorker = (ChunkRenderWorkerAccessor) new ChunkRenderWorker(renderDispatcher, new BlockBufferBuilderStorage());
    //$$     ChunkRenderDispatcherAccessor renderDispatcherAcc = (ChunkRenderDispatcherAccessor) renderDispatcher;
    //$$
    //$$     int workerThreads = renderDispatcherAcc.getListThreadedWorkers().size();
        //#if MC>=10904
        //$$ PriorityBlockingQueue<ChunkRenderTask> queueChunkUpdates = renderDispatcherAcc.getQueueChunkUpdates();
        //#else
        //$$ BlockingQueue<ChunkCompileTaskGenerator> queueChunkUpdates = renderDispatcherAcc.getQueueChunkUpdates();
        //#endif
    //$$     workerJailingQueue = new JailingQueue<>(queueChunkUpdates);
    //$$     renderDispatcherAcc.setQueueChunkUpdates(workerJailingQueue);
        //#if MC>=10904
        //$$ ChunkRenderTask element = new ChunkRenderTask(
        //$$         null,
        //$$         null,
        //$$         0
                //#if MC>=11400
                //$$ , null
                //#endif
        //$$ );
        //#else
        //$$ ChunkCompileTaskGenerator element = new ChunkCompileTaskGenerator(null, null);
        //#endif
    //$$     element.cancel();
    //$$     for (int i = 0; i < workerThreads; i++) {
    //$$         queueChunkUpdates.add(element);
    //$$     }
    //$$
    //$$     // Temporary workaround for dead lock, will be replaced by a new (ShaderMod compatible) mechanism later
    //$$     //noinspection StatementWithEmptyBody
    //$$     while (renderDispatcher.runTasksSync(0)) {}
    //$$
    //$$     workerJailingQueue.jail(workerThreads);
    //$$     renderDispatcherAcc.setQueueChunkUpdates(queueChunkUpdates);
    //$$ }
    //$$
    //$$ public void updateChunks() {
    //$$     while (renderDispatcher.runTasksSync(0)) {
    //$$         ((WorldRendererAccessor) hooked).setDisplayListEntitiesDirty(true);
    //$$     }
    //$$
        //#if MC>=10904
        //$$ PriorityBlockingQueue<ChunkRenderTask> queueChunkUpdates;
        //#else
        //$$ BlockingQueue<ChunkCompileTaskGenerator> queueChunkUpdates;
        //#endif
    //$$     queueChunkUpdates = ((ChunkRenderDispatcherAccessor) renderDispatcher).getQueueChunkUpdates();
    //$$     while (!queueChunkUpdates.isEmpty()) {
    //$$         try {
    //$$             renderWorker.doRunTask(queueChunkUpdates.poll());
    //$$         } catch (InterruptedException ignored) { }
    //$$     }
    //$$
    //$$     Iterator<ChunkRenderer> iterator = ((WorldRendererAccessor) hooked).getChunksToUpdate().iterator();
    //$$     while (iterator.hasNext()) {
    //$$         ChunkRenderer renderchunk = iterator.next();
    //$$
    //$$         renderDispatcher.rebuildSync(renderchunk);
    //$$
            //#if MC>=10904
            //$$ renderchunk.unscheduleRebuild();
            //#else
            //$$ renderchunk.setNeedsUpdate(false);
            //#endif
    //$$         iterator.remove();
    //$$     }
    //$$ }
    //$$
    //$$ public int nextFrameId() {
    //$$     return frame++;
    //$$ }
    //#endif

    public void uninstall() {
        //#if MC<11500
        //$$ workerJailingQueue.freeAll();
        //#endif

        try {
            //#if MC>=11400
            Field hookField = WorldRenderer.class.getField("replayModRender_hook");
            //#else
            //$$ Field hookField = RenderGlobal.class.getField("replayModRender_hook");
            //#endif
            hookField.set(hooked, null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new Error(e);
        }
    }

    //#if MC>=11500
    public interface IBlockOnChunkRebuilds {
        boolean uploadEverythingBlocking();
    }
    //#endif
}
//#endif
