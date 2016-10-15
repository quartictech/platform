package io.quartic.weyl.core.live;

public enum LiveLayerViewType {
    LOCATION_AND_TRACK(new LastKnownLocationAndTrackView()),
    MOST_RECENT(new MostRecentGeometryView());

    private final LiveLayerView view;

    LiveLayerViewType(LiveLayerView view) {
        this.view = view;
    }

    public LiveLayerView getLiveLayerView() {
        return view;
    }
}
