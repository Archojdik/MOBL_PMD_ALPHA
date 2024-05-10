package com.example.mobl_pmd;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;

public class PianoVisualView extends View {
    private static final Paint backPaint;

    static {
        backPaint = new Paint();
        backPaint.setColor(Color.blue(0xff));
    }
    public PianoVisualView(Context context) {
        super(context);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        //super.onDraw(canvas);
        int w = canvas.getWidth();
        int h = canvas.getHeight();

        Rect r = new Rect(0, 0, w, h);
        canvas.drawRect(r, backPaint);
    }
}
