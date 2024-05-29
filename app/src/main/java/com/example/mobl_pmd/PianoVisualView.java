package com.example.mobl_pmd;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PianoVisualView extends View {
    private static final Paint BACK_PAINT;
    private static final Paint WHITE_KEYS_PAINT;
    private static final Paint BLACK_KEYS_PAINT;
    private static final Paint SHADES_PAINT;
    private static final Paint PRESSED_PAINT;

    private static final Rect drawRect;
    private boolean[] keyIsPressed = new boolean[96]; // 12 * 8

    static {
        BACK_PAINT = new Paint();
        WHITE_KEYS_PAINT = new Paint();
        BLACK_KEYS_PAINT = new Paint();
        SHADES_PAINT = new Paint();
        PRESSED_PAINT = new Paint();

        BACK_PAINT.setColor(Color.rgb(0, 0, 0));
        WHITE_KEYS_PAINT.setColor(Color.rgb(0xcf, 0xcf, 0xba));
        BLACK_KEYS_PAINT.setColor(Color.rgb(0, 0, 0));
        SHADES_PAINT.setColor(Color.rgb(0x65, 0x65, 0x55));
        PRESSED_PAINT.setColor(Color.rgb(0x8a, 0xff, 0x45));

        drawRect = new Rect(0, 0, 0, 0);
    }
    public PianoVisualView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        int w = getWidth();
        int h = getHeight();

        drawRect.top = 0;
        drawRect.left = 0;
        drawRect.right = w;
        drawRect.bottom = h;
        canvas.drawRect(drawRect, BACK_PAINT);

        int startX = 0;
        int startKey = 0;
        int octaveWidth = w / 8;
        for (int i = 0; i < 8; i++) {
            drawOctave(canvas, startX, startKey, octaveWidth, h);
            startX += octaveWidth;
            startKey += 12;
        }

        super.onDraw(canvas);
    }
    private void drawOctave(Canvas canvas, int startX, int startKey, int w, int h) {
        // @TODO: поменьше вычислений в методе отрисовки
        final int[] semitonesWhite = {0, 2, 4, 5, 7, 9, 11};
        final int[] semitonesBlack = {1, 3, 6, 8, 10};

        // Рисуем попиксельно (прямоугольниками), но растягиваем картинку до размера холста.
        // Белые клавиши
        int keyWidth = w / 7;
        int wKeyBodyWidth = keyWidth * 4/5;
        int x = startX;
        int rectWidth;
        for (int i = 0; i < 7; i++) {
            // Основная часть
            drawRect.top = 0;
            drawRect.left = x;
            drawRect.right = x + wKeyBodyWidth;
            drawRect.bottom = h;
            if (keyIsPressed[startKey + semitonesWhite[i]])
                canvas.drawRect(drawRect, PRESSED_PAINT);
            else
                canvas.drawRect(drawRect, WHITE_KEYS_PAINT);

            // Тени
            rectWidth = wKeyBodyWidth/4;
            drawRect.top = h - rectWidth;
            drawRect.right = x + rectWidth;
            canvas.drawRect(drawRect, SHADES_PAINT);

            drawRect.right = x + wKeyBodyWidth;
            drawRect.left = drawRect.right - rectWidth;
            canvas.drawRect(drawRect, SHADES_PAINT);

            x += keyWidth;
        }

        // Чёрные клавиши
        int bKeyBodyWidth = keyWidth * 3/5;
        int bKeyBodyHeight = h / 2;
        x = startX + bKeyBodyWidth;
        int blackKeyIndex = 0;
        for (int i = 0; i < 6; i++) {
            if (i != 2) {
                // Основная часть
                drawRect.top = 0;
                drawRect.left = x;
                drawRect.right = x + bKeyBodyWidth;
                drawRect.bottom = bKeyBodyHeight;
                if (keyIsPressed[startKey + semitonesBlack[blackKeyIndex]])
                    canvas.drawRect(drawRect, PRESSED_PAINT);
                else
                    canvas.drawRect(drawRect, BLACK_KEYS_PAINT);

                // Тени
                // Верхняя
                rectWidth = bKeyBodyWidth/3;
                drawRect.top = bKeyBodyHeight - rectWidth*3;
                drawRect.bottom = bKeyBodyHeight - rectWidth*2;
                drawRect.right = x + rectWidth;
                canvas.drawRect(drawRect, SHADES_PAINT);

                // Нижняя
                drawRect.top += rectWidth;
                drawRect.bottom += rectWidth;
                canvas.drawRect(drawRect, SHADES_PAINT);

                // Правая
                drawRect.left += rectWidth;
                drawRect.right += rectWidth;
                canvas.drawRect(drawRect, SHADES_PAINT);

                blackKeyIndex++;
            }
            x += keyWidth;
        }
    }

    public void setKeyPressed(int midiKey, boolean isPressed) {
        if (midiKey >= 0 && midiKey < keyIsPressed.length)
            keyIsPressed[midiKey] = isPressed;
    }
    public void requestRedraw() {
        invalidate();
    }
}
