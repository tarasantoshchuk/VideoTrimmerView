package com.tarasantoshchuk.videotrimmerview;

public interface PlayerListener {
    void onPause();
    void onPositionChange(float currentPosition);
}
