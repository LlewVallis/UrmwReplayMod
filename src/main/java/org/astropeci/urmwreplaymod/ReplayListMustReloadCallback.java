package org.astropeci.urmwreplaymod;

import de.johni0702.minecraft.gui.utils.Event;

public interface ReplayListMustReloadCallback {
    Event<ReplayListMustReloadCallback> EVENT = Event.create((listeners) ->
            () -> {
                for (ReplayListMustReloadCallback listener : listeners) {
                    listener.reload();
                }
            });

    void reload();
}
