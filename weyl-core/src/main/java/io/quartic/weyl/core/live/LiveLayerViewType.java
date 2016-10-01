package io.quartic.weyl.core.live;

public enum LiveLayerViewType {
    LOCATION_AND_TRACK(new LastKnownLocationAndTrackView());

    private final LiveLayerView view;

    LiveLayerViewType(LiveLayerView view) {
        this.view = view;
    }

    LiveLayerView getLiveLayerView() {
        return view;
    }
}
