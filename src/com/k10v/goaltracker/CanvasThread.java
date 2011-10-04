package com.k10v.goaltracker;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

public class CanvasThread extends Thread {
    private SurfaceHolder mSurfaceHolder;
    private Panel mPanel;
    private boolean mRun = false;

    public CanvasThread(SurfaceHolder surfaceHolder, Panel panel) {
        mSurfaceHolder = surfaceHolder;
        mPanel = panel;
    }

    public void setRunning(boolean run) {
        mRun = run;
    }

    @Override
    public void run() {
        Canvas c;
        while (mRun) {
            c = null;
            if (!mPanel.needsUpdate()) {
                continue;
            }
            try {
                c = mSurfaceHolder.lockCanvas();
                synchronized (mSurfaceHolder) {
                    mPanel.onDraw(c);
                }
            } finally {
                // do this in a finally so that if an exception is thrown during
                // the above, we don't leave the Surface in an inconsistent
                // state
                if (c != null) {
                    mSurfaceHolder.unlockCanvasAndPost(c);
                }
            }
        }
    }
}
