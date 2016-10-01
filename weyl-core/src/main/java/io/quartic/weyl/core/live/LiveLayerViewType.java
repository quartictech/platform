package io.quartic.weyl.core.live;

public enum LiveLayerViewType {
    LOCATION_AND_TRACK;

    LiveLayerView getLiveLayerView() {
        switch (this) {
            case LOCATION_AND_TRACK:
                return new LastKnownLocationAndTrackView();
            default:
                throw new IllegalArgumentException("unconfigured live layer view type " + toString());
        }
    }
}
