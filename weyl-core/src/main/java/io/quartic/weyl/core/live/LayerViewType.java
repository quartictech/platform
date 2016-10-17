package io.quartic.weyl.core.live;

public enum LayerViewType {
    LOCATION_AND_TRACK(new LastKnownLocationAndTrackView()),
    MOST_RECENT(new MostRecentGeometryView());

    private final LayerView view;

    LayerViewType(LayerView view) {
        this.view = view;
    }

    public LayerView getLayerView() {
        return view;
    }
}
