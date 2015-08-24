package org.kotemaru.android.taskkiller.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.List;


public class GraphView extends View {
    private static final String TAG = GraphView.class.getSimpleName();
    private float[] mData;
    private Paint mPaint = new Paint();
    private Path mPath = new Path();

    public GraphView(Context context) {
        super(context);
    }

    public GraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GraphView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //public GraphView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    //    super(context, attrs, defStyleAttr, defStyleRes);
    //}

    public void setData(float[] data) {
        mData = data;
        invalidate();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        float h = (float) canvas.getHeight();
        float w = (float) canvas.getWidth();
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(Color.LTGRAY);
        canvas.drawRect(0, 0, w, h, mPaint);
        if (mData == null) return;

        try {
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mPaint.setColor(Color.RED);
            int size = mData.length;
            mPath.reset();
            float x0 = 0;
            float y0 = h - 1.0F - mData[0] * 10F * h;
            mPath.moveTo(x0, y0);
            for (int i = 1; i < size; i++) {
                float x = i * w / (size - 1);
                float y = h - 1.0F - mData[i] * 10F * h;
                mPath.lineTo(x, y);
            }
            mPath.lineTo(w, h);
            mPath.lineTo(0, h);
            canvas.drawPath(mPath, mPaint);
        } catch (RuntimeException e) {
            Log.e(TAG, e.toString(), e);
        }
    }

}
