package com.appathon.androzacs.c300cga;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

class MyRectInfo
{
    Rect m_rect;
    int m_Angle;
    int rotationCenterXCor;
    int rotationCenterYCor;

    String subsystem_name;

    MyRectInfo(Rect rect, int angle, int x, int y, String name)
    {
        m_rect = rect;
        m_Angle = angle;
        rotationCenterXCor = x;
        rotationCenterYCor = y;
        subsystem_name = name;
    }
}

public class Polygon extends View implements
        GestureDetector.OnGestureListener{

    private int sides = 2;
    private int strokeColor = 0xff000000;
    private int strokeWidth = 0;
    private int fillColor = 0xffffffff;
    private float ref_left_polygon_sides = -1;
    private int ref_right_polygon_sides = -1;
    //private int fillColor = 0xffffffff;
    private float startAngle = -90;
    private boolean showInscribedCircle = false;
    private float fillPercent = 1;
    private int fillBitmapResourceId = -1;

    private Paint fillPaint;
    private Paint strokePaint;
    private Paint inscribedCirclePaint;

    private Path polyPath;

    private float radius_for_pentagon = -1;
    private float side_distance_from_center = -1;
    private Point m_center_of_pentagon_innercircle = null;

    private ArrayList<MyRectInfo> listRectangles = new ArrayList<MyRectInfo>();

    private Context m_Context = null;

    private int m_toolID =-1;

    private Map<String, SubSystemInfo> m_subSystems;

    private Point m_TouchPoint;

    // private OnLongClickListener onLongClickListener =

    private static final String DEBUG_TAG = "Gestures";
    private GestureDetectorCompat mDetector;

    public Polygon(Context context) {
        super(context);
        mDetector = new GestureDetectorCompat(this.getContext(),this);
    }

    public Polygon(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);

        // Instantiate the gesture detector with the
        // application context and an implementation of
        // GestureDetector.OnGestureListener
        mDetector = new GestureDetectorCompat(this.getContext(),this);
    }

    public Polygon(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
        mDetector = new GestureDetectorCompat(this.getContext(),this);
    }

    public void setContext(Context ctx)
    {
        m_Context = ctx;
    }

    private void updateSubSystemInfo()
    {
        if(m_subSystems!=null)
        {
            ToolInfo ti = MainActivity.m_ToolMap.get(m_toolID);
            if(ti!=null)
            {
                m_subSystems = ti.GetSubSystemsMap();
            }
        }
    }

    public void SetToolID(int id)
    {
        m_toolID = id;
        updateSubSystemInfo();
    }

    private void init(AttributeSet attrs){

        m_subSystems = new LinkedHashMap<>();

        TypedArray polyAttributes =getContext().obtainStyledAttributes(
                attrs,
                R.styleable.Polygon);

        sides = polyAttributes.getInt(R.styleable.Polygon_sides, sides);
        strokeColor = polyAttributes.getColor(R.styleable.Polygon_stroke_color, strokeColor);
        strokeWidth = polyAttributes.getInt(R.styleable.Polygon_stroke_width, strokeWidth);
        fillColor = polyAttributes.getColor(R.styleable.Polygon_fill_color, fillColor);
        startAngle = polyAttributes.getFloat(R.styleable.Polygon_start_angle, startAngle);
        showInscribedCircle = polyAttributes.getBoolean(R.styleable.Polygon_inscribed_circle, showInscribedCircle);
        fillBitmapResourceId = polyAttributes.getResourceId(R.styleable.Polygon_fill_bitmap, fillBitmapResourceId);

        ref_left_polygon_sides = polyAttributes.getDimension(R.styleable.Polygon_ref_padding_left_polygon_side, ref_left_polygon_sides);
        ref_right_polygon_sides = polyAttributes.getInt(R.styleable.Polygon_ref_padding_right_polygon_side, ref_right_polygon_sides);

        float fillPct = polyAttributes.getFloat(R.styleable.Polygon_fill_percent, 100);

        polyAttributes.recycle();

        if(fillBitmapResourceId != -1){
            Bitmap fillBitmap = BitmapFactory.decodeResource(getResources(), fillBitmapResourceId);
            BitmapShader fillShader = new BitmapShader(fillBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
            fillPaint.setShader(fillShader);
        }

        if(strokeWidth > 0){
            strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            strokePaint.setColor(strokeColor);
            strokePaint.setStrokeWidth(strokeWidth);
            strokePaint.setStyle(Paint.Style.STROKE);
        }

        polyPath = new Path();
        polyPath.setFillType(Path.FillType.WINDING);

        if(fillPct < 100){
            fillPercent = fillPct / 100;
        }

        if (fillPercent < 0 || fillPercent > 100){
            fillPercent = 1;
        }

        this.setPadding((int)150,0,(int)150,0);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth = measureWidth(widthMeasureSpec);
        int measuredHeight = measureHeight(heightMeasureSpec);
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    private int measureWidth(int measureSpec){
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        int result;

        switch(specMode){
            case MeasureSpec.AT_MOST:
                result = specSize;
                break;
            case MeasureSpec.EXACTLY:
                result = specSize;
                break;

            default:
                // random size if nothing is specified
                result = 500;
                break;
        }
        return result;
    }

    private int measureHeight(int measureSpec){
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        int result;

        switch(specMode){
            case MeasureSpec.AT_MOST:
                result = specSize;
                break;
            case MeasureSpec.EXACTLY:
                result = specSize;
                break;

            default:
                // random size if nothing is specified
                result = 500;
                break;
        }
        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();
        int screenCenterXCor = (measuredWidth/2)  ;
        int screenCenterYCor = (measuredHeight/2) ;
        int ref_radius = Math.min(screenCenterXCor,screenCenterYCor);

        if(m_toolID < 1) return;

        if (sides < 3) return;

        listRectangles.clear();

        radius_for_pentagon = (float)(ref_radius * 0.60);

        side_distance_from_center = (float)(radius_for_pentagon * Math.cos(Math.PI/sides));

        m_center_of_pentagon_innercircle = new Point(screenCenterXCor, screenCenterYCor);

        drawPentagon(canvas, screenCenterXCor, screenCenterYCor);

        if(showInscribedCircle) {
            //canvas.drawCircle(screenCenterXCor,screenCenterYCor,ref_radius, inscribedCirclePaint);
            //inscribedCirclePaint.setColor(Color.BLUE);
            //canvas.drawCircle(screenCenterXCor,screenCenterYCor,(int)(radius_for_pentagon), inscribedCirclePaint);
        }

        drawTLRect(canvas, screenCenterXCor, screenCenterYCor, "CHB");
        drawTRRect(canvas, screenCenterXCor, screenCenterYCor, "CHC");
        drawMLRect(canvas, screenCenterXCor, screenCenterYCor, "CHA");
        drawMRRect(canvas, screenCenterXCor, screenCenterYCor, "CHD");
        drawBLRect(canvas, screenCenterXCor, screenCenterYCor, "LLA");
        drawBRRect(canvas, screenCenterXCor, screenCenterYCor, "LLB");

        super.onDraw(canvas);

    }

    private int getBaseSideLengthForRect()
    {
        return (int)(2 * (radius_for_pentagon - radius_for_pentagon * fillPercent) * Math.sin(Math.PI/sides));
    }

    final GestureDetector gestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
        public void onLongPress(MotionEvent event) {

        }
    });

    private boolean DoesContainPoint(MyRectInfo rect, int x, int y)
    {
        if(rect!=null)
        {
            RectF r = new RectF(rect.m_rect);

            float[] src = new float[] {rect.m_rect.left, rect.m_rect.top, rect.m_rect.right, rect.m_rect.bottom};
            float[] dest = new float[4];

            Matrix m = new Matrix();
            m.setRotate(rect.m_Angle, rect.rotationCenterXCor, rect.rotationCenterYCor);
            m.mapPoints(dest, src);

            if((Math.min(dest[0],dest[2]) <= x && Math.max(dest[0], dest[2]) >= x) && (Math.min(dest[1],dest[3]) <= y && Math.max(dest[1], dest[3]) >= y))
                return true;
        }
        return false;

    }

    private boolean IsInsidePentagon(int x, int y)
    {
        double distance = Math.sqrt(Math.pow((m_center_of_pentagon_innercircle.x - x),2) + Math.pow((m_center_of_pentagon_innercircle.y - y),2) );
        if(distance < radius_for_pentagon)
            return true;
        return false;
    }

    private void ShowPopup(String subsystem_key)
    {
        if(m_Context!=null) {
            AlertDialog.Builder popDialog = new AlertDialog.Builder(m_Context);

            final LayoutInflater inflater = (LayoutInflater) m_Context.getSystemService(m_Context.LAYOUT_INFLATER_SERVICE);
            final View Viewlayout = inflater.inflate(R.layout.activity_subsysteminfo_dialog,
                    (ViewGroup) findViewById(R.id.layout_subsysteminfo_dialog));

            popDialog.setIcon(R.drawable.chinfo);
            popDialog.setTitle(subsystem_key + " Info");
            popDialog.setView(Viewlayout);
            popDialog.setCancelable(true);
            popDialog.setPositiveButton("Close",null);

            //Fill the table

            TextView tv_txt_ch_state = (TextView)Viewlayout.findViewById(R.id.tbl_txt_ch_state);
            TextView tv_txt_ch_type = (TextView)Viewlayout.findViewById(R.id.tbl_txt_ch_type);
            TextView tv_txt_ch_pressure = (TextView)Viewlayout.findViewById(R.id.tbl_txt_ch_pressure);
            TextView tv_txt_ch_cur_recipe = (TextView)Viewlayout.findViewById(R.id.tbl_txt_ch_cur_recipe);
            TextView tv_txt_ch_wafer_present = (TextView)Viewlayout.findViewById(R.id.tbl_txt_ch_wafer_present);

            if(m_subSystems!=null)
            {
                SubSystemInfo si = m_subSystems.get(subsystem_key);
                if(si!=null)
                {
                    String state = si.GetSystemState().toString();
                    if(tv_txt_ch_state != null)
                        tv_txt_ch_state.setText(state);

                    String type = si.GetSubsystemType();
                    if(tv_txt_ch_type != null)
                        tv_txt_ch_type.setText(type);

                    String chPress = si.getSubSystemChPressure();
                    if(tv_txt_ch_pressure != null)
                        tv_txt_ch_pressure.setText(chPress);

                    String curRecipe = si.getSubSystemCurrentRecipe();
                    if(tv_txt_ch_cur_recipe != null)
                        tv_txt_ch_cur_recipe.setText(curRecipe);

                    String waferState = si.getSubSystemWaferState();
                    if(tv_txt_ch_wafer_present != null)
                        tv_txt_ch_wafer_present.setText(waferState);
                }
            }

            popDialog.create();
            popDialog.show();


//        builder.setTitle(message);
//
//        builder.setItems(new CharSequence[] {"Copy Text", "Delete", "Details"} ,
//                new DialogInterface.OnClickListener()
//                {
//                    public void onClick(DialogInterface dialog, int which) { /* which is an index */ }
//                });
//        builder.show();
        }
        else
        {
            //Toast.makeText(getApplicationContext(), "This is an Android Toast Message", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        this.mDetector.onTouchEvent(event);
        // Be sure to call the superclass implementation
        return super.onTouchEvent(event);
    }

    private void drawPentagon(Canvas canvas, int x, int y) {

        int workingRadius = (int)(radius_for_pentagon * 0.97);
        float angle_at_center = (float) (Math.PI * 2)/sides;

        polyPath.reset();
        polyPath.setFillType(Path.FillType.EVEN_ODD);

        workingRadius -= radius_for_pentagon * fillPercent;
        // The poly is created as a shape in a path.
        // If there is a hole in the poly, draw a 2nd shape inset from the first
        for (int j = 0; j < ((fillPercent < 1) ? 1 : 1); j++) {
            polyPath.moveTo(workingRadius, 0);
            for (int i = 1; i < sides; i++) {
                polyPath.lineTo((float) (workingRadius * Math.cos(angle_at_center * i)),
                        (float) (workingRadius * Math.sin(angle_at_center * i)));
            }
            polyPath.close();
        }

        canvas.save();
        canvas.translate(x, y);
        canvas.rotate(startAngle);

        //Draw Filled Polygon
        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setColor(Color.parseColor("#005883"));
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAntiAlias(true);
        canvas.drawPath(polyPath, fillPaint);

        //Draw border for polygon
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setColor(getSubSystemColor("Buffer"));
        strokePaint.setStrokeWidth(radius_for_pentagon * fillPercent);
        canvas.drawPath(polyPath, strokePaint);

        canvas.translate(-x, -y);

        canvas.save();
        canvas.rotate(90,x,y);
        int offset = (int)(radius_for_pentagon * 0.1);
        int radiiForCircle = (int)(radius_for_pentagon * 0.15);

        //Write Text
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        int pixel= (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                15, getResources().getDisplayMetrics());
        paint.setTextSize(pixel);
        canvas.drawText("BUFFER" + " " + getSubSystemInclusionCharacter("Buffer"), (int)(m_center_of_pentagon_innercircle.x - radiiForCircle - offset), (int)(m_center_of_pentagon_innercircle.y - offset - radiiForCircle),  paint);

        canvas.restore();

        boolean bHasWaferInSubSystem = hasWaferInSubSystem("Buffer");

        if(bHasWaferInSubSystem) {
            //Draw Filled Circle on left to the center
            fillPaint.setStyle(Paint.Style.FILL);
            fillPaint.setColor(Color.parseColor("#269FBD"));
            fillPaint.setAntiAlias(true);
            canvas.drawCircle(m_center_of_pentagon_innercircle.x, m_center_of_pentagon_innercircle.y - offset - radiiForCircle, radiiForCircle, fillPaint);
        }

        //Draw circle border on left to the center
        //Draw circle border (dotted)
        Paint paint1 = new Paint();
        paint1.setStyle(Paint.Style.STROKE);
        paint1.setColor(Color.LTGRAY);
        paint1.setStrokeWidth(radius_for_pentagon * fillPercent/4);
        if(!bHasWaferInSubSystem)
            paint1.setPathEffect(new DashPathEffect(new float[]{5, 5}, 0));
        canvas.drawCircle(m_center_of_pentagon_innercircle.x ,m_center_of_pentagon_innercircle.y - offset - radiiForCircle,radiiForCircle, paint1);

        if(bHasWaferInSubSystem) {
            //Draw Filled Circle on right to the center
            fillPaint.setStyle(Paint.Style.FILL);
            fillPaint.setColor(Color.parseColor("#269FBD"));
            fillPaint.setAntiAlias(true);
            canvas.drawCircle(m_center_of_pentagon_innercircle.x ,m_center_of_pentagon_innercircle.y + offset + radiiForCircle,radiiForCircle, fillPaint);
        }
        //Draw circle border on right to the center
        canvas.drawCircle(m_center_of_pentagon_innercircle.x ,m_center_of_pentagon_innercircle.y + offset + radiiForCircle,radiiForCircle, paint1);

        //Restore Canvas
        canvas.restore();
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        Log.d(DEBUG_TAG, "onShowPress: " + e.toString());
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        Log.d(DEBUG_TAG, "onLongPress: " + e.toString());
        int touchX = (int)e.getX();
        int touchY = (int)e.getY();
        if (IsInsidePentagon(touchX, touchY)) {
            ShowPopup("Buffer");
            System.out.println("*********Touched Buffer!. start activity.");
            return;
        }
        int i = 0;
        for (MyRectInfo rect : listRectangles) {
            i++;
            if (DoesContainPoint(rect, touchX, touchY)) {
                ShowPopup(rect.subsystem_name);
                System.out.println("*********Touched " + rect.subsystem_name + ", start activity.");
                return;
            }
        }
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    enum CHTextLocation { TOP, BOTTOM; }

    private void drawRectangle(Canvas canvas, int screenCenterX, int screenCenterY, Rect rect, int rotationAngle, String chName, CHTextLocation location )
    {
        listRectangles.add(new MyRectInfo(rect, rotationAngle, screenCenterX, screenCenterY, chName));
        canvas.save();
        canvas.rotate(rotationAngle,screenCenterX ,screenCenterY);

        //Write Text
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        int pixel= (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                15, getResources().getDisplayMetrics());
        paint.setTextSize(pixel);
        if(location == CHTextLocation.TOP)
            canvas.drawText(chName + " " + getSubSystemInclusionCharacter(chName), rect.left + (int)((rect.right - rect.left) * 0.3), rect.top - 20, paint);
        else
            canvas.drawText(chName + " " + getSubSystemInclusionCharacter(chName), rect.left + (int) ((rect.right - rect.left) * 0.25), rect.bottom + 60, paint);

        //Draw filled rectangle
        fillPaint.setColor(Color.parseColor("#005883"));
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAntiAlias(true);
        if (android.os.Build.VERSION.SDK_INT>= Build.VERSION_CODES.LOLLIPOP) {
            // call something for API Level 21+
            canvas.drawRoundRect(rect.left, rect.top, rect.right, rect.bottom, 6, 6, fillPaint);
        }
        else
        {
            canvas.drawRect(rect, fillPaint);
        }

        //Draw rectangle border
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setColor(getSubSystemColor(chName));
        strokePaint.setStrokeWidth(radius_for_pentagon * fillPercent);
        if (android.os.Build.VERSION.SDK_INT>= Build.VERSION_CODES.LOLLIPOP) {
            // call something for API Level 21+
            canvas.drawRoundRect(rect.left, rect.top, rect.right, rect.bottom, 6, 6, strokePaint);
        }
        else
        {
            canvas.drawRect(rect, strokePaint);
        }

        //Draw Slit
        fillPaint.setStyle(Paint.Style.FILL);
        // fillPaint.setColor(Color.parseColor("#005883"));
        fillPaint.setColor(Color.LTGRAY);
        fillPaint.setAntiAlias(true);
        fillPaint.setPathEffect(new DashPathEffect(new float[]{10, 20}, 0));
        int rect_height = rect.height();
        int rect_width = rect.width();

        if(location == CHTextLocation.TOP) {
            Rect slitRect = new Rect((int) (rect.left + rect_width * 0.2), (int) (rect.top + rect_height * 0.90), (int) (rect.right - rect_width * 0.2), (int) (rect.bottom + rect_height * 0.17));
            canvas.drawRect(slitRect, fillPaint);

            //Draw circle border
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setColor(Color.BLACK);
            strokePaint.setStrokeWidth(radius_for_pentagon * fillPercent/4);

            canvas.drawRect(slitRect, strokePaint);

        }
        else
        {
            Rect slitRect = new Rect((int) (rect.left + rect_width * 0.2), (int) (rect.top - rect_height * 0.17), (int) (rect.right - rect_width * 0.2), (int) (rect.top + rect_height * 0.1));
            canvas.drawRect(slitRect, fillPaint);

            //Draw circle border
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setColor(Color.BLACK);
            strokePaint.setStrokeWidth(radius_for_pentagon * fillPercent/4);
            canvas.drawRect(slitRect, strokePaint);
        }

        int radiiForCircle = (int)(radius_for_pentagon * 0.15);

        boolean bHasWaferInSubSystem = hasWaferInSubSystem(chName);

        if(bHasWaferInSubSystem) {
            //Draw Filled Circle
            fillPaint.setStyle(Paint.Style.FILL);
            fillPaint.setColor(Color.parseColor("#269FBD"));
            fillPaint.setAntiAlias(true);
            canvas.drawCircle(rect.left + (rect.right - rect.left) / 2, rect.top + (rect.bottom - rect.top) / 2, radiiForCircle, fillPaint);
        }

        //Draw circle border (dotted)
        Paint paint1 = new Paint();
        paint1.setStyle(Paint.Style.STROKE);
        paint1.setColor(Color.LTGRAY);
        paint1.setStrokeWidth(radius_for_pentagon * fillPercent/4);
        if(!bHasWaferInSubSystem)
            paint1.setPathEffect(new DashPathEffect(new float[]{5, 5}, 0));
        canvas.drawCircle(rect.left+(rect.right - rect.left)/2,rect.top+(rect.bottom - rect.top)/2,radiiForCircle,paint1);

        //Restore Canvas
        canvas.restore();
    }

    private void drawTRRect(Canvas canvas, int x, int y, String subSystemName)
    {
        SubSystemInfo si = m_subSystems.get(subSystemName);
        if(si!=null && si.IsConfigured()) {
            int side_length = getBaseSideLengthForRect();

            int widthRect = (int)(side_length * 0.8);
            int heightRect = (int) (side_length * 0.55);

            int topXCor = x - widthRect / 2;// - (int)side_distance_from_center - heightRect;
            int topYCor = y - (int) side_distance_from_center - heightRect;
            int bottomXCor = topXCor + widthRect;
            int bottomYCor = topYCor + heightRect;

            Rect rect = new Rect(topXCor, topYCor, bottomXCor, bottomYCor);


            drawRectangle(canvas, x, y, rect, 36, subSystemName, CHTextLocation.TOP);
        }
    }

    private void drawTLRect(Canvas canvas, int x, int y, String subSystemName)
    {

        SubSystemInfo si = m_subSystems.get(subSystemName);
        if(si!=null && si.IsConfigured()) {
            int side_length = getBaseSideLengthForRect();

            int widthRect = (int)(side_length * 0.8);
            int heightRect = (int) (side_length * 0.55);

            int topXCor = x - widthRect / 2;// - (int)side_distance_from_center - heightRect;
            int topYCor = y - (int) side_distance_from_center - heightRect;
            int bottomXCor = topXCor + widthRect;
            int bottomYCor = topYCor + heightRect;

            Rect rect = new Rect(topXCor, topYCor, bottomXCor, bottomYCor);
            drawRectangle(canvas, x, y, rect, -36, subSystemName, CHTextLocation.TOP);
        }
    }

    private void drawMLRect(Canvas canvas, int x, int y, String subSystemName)
    {
        SubSystemInfo si = m_subSystems.get(subSystemName);
        if(si!=null && si.IsConfigured()) {
            int side_length = getBaseSideLengthForRect();

            int widthRect = (int)(side_length * 0.8);
            int heightRect = (int) (side_length * 0.55);

            int topXCor = x - widthRect / 2;// - (int)side_distance_from_center - heightRect;
            int topYCor = y - (int) side_distance_from_center - heightRect;
            int bottomXCor = topXCor + widthRect;
            int bottomYCor = topYCor + heightRect;

            Rect rect = new Rect(topXCor, topYCor, bottomXCor, bottomYCor);
            drawRectangle(canvas, x, y, rect, -108, subSystemName, CHTextLocation.TOP);
        }
    }

    private void drawMRRect(Canvas canvas, int x, int y, String subSystemName)
    {
        SubSystemInfo si = m_subSystems.get(subSystemName);
        if(si!=null && si.IsConfigured()) {
            int side_length = getBaseSideLengthForRect();

            int widthRect = (int)(side_length * 0.8);
            int heightRect = (int) (side_length * 0.55);

            int topXCor = x - widthRect / 2;// - (int)side_distance_from_center - heightRect;
            int topYCor = y - (int) side_distance_from_center - heightRect;
            int bottomXCor = topXCor + widthRect;
            int bottomYCor = topYCor + heightRect;

            Rect rect = new Rect(topXCor, topYCor, bottomXCor, bottomYCor);
            drawRectangle(canvas, x, y, rect, 108, subSystemName, CHTextLocation.TOP);
        }
    }

    private void drawBLRect(Canvas canvas, int x, int y, String subSystemName)
    {
        SubSystemInfo si = m_subSystems.get(subSystemName);
        if(si!=null && si.IsConfigured()) {
            int side_length = getBaseSideLengthForRect();

            int heightRect = side_length / 2;
            int widthRect = (int) (side_length * 0.45);

            int topXCor = x - (int) (side_length * 0.5);// - (int)side_distance_from_center - heightRect;
            int topYCor = y + (int) side_distance_from_center;
            int bottomXCor = topXCor + widthRect;
            int bottomYCor = topYCor + heightRect;

            Rect rect = new Rect(topXCor, topYCor, bottomXCor, bottomYCor);
            drawRectangle(canvas, x, y, rect, 0, subSystemName, CHTextLocation.BOTTOM);
        }
    }

    private void drawBRRect(Canvas canvas, int x, int y, String subSystemName)
    {
        SubSystemInfo si = m_subSystems.get(subSystemName);
        if(si!=null && si.IsConfigured()) {
            int side_length = getBaseSideLengthForRect();

            int heightRect = side_length / 2;
            int widthRect = (int) (side_length * 0.45);

            int topXCor = x + (int) (side_length * 0.5) - widthRect;// - (int)side_distance_from_center - heightRect;
            int topYCor = y + (int) side_distance_from_center;
            int bottomXCor = topXCor + widthRect;
            int bottomYCor = topYCor + heightRect;

            Rect rect = new Rect(topXCor, topYCor, bottomXCor, bottomYCor);
            drawRectangle(canvas, x, y, rect, 0, subSystemName, CHTextLocation.BOTTOM);
        }
    }

    private int getSubSystemColor(String subSystemName)
    {
        if(m_subSystems!=null)
        {
            SubSystemInfo si = m_subSystems.get(subSystemName);
            if(si!=null)
            {
                ToolInfo.SystemState state = si.GetSystemState();
                if(state == ToolInfo.SystemState.IDLE)
                    return Color.BLUE;
                else if(state == ToolInfo.SystemState.FAULTED)
                    return Color.RED;
                else if(state == ToolInfo.SystemState.PROCESSING)
                    return Color.GREEN;
                else if(state == ToolInfo.SystemState.UNKNOWN)
                    return Color.GRAY;
            }
        }
        return Color.GRAY; //RK:Change it to appropriate one

    }

    private char getSubSystemInclusionCharacter(String subSystemName)
    {
        if(m_subSystems!=null)
        {
            SubSystemInfo si = m_subSystems.get(subSystemName);
            if(si!=null)
            {
                if(si.getIsPartOfToolSystemInfo())
                    return (char) 0x2714;         // Check Mark
            }
        }
        return (char) 0x2718; //Cross mark
    }

    private boolean hasWaferInSubSystem(String subSystemName)
    {
        if(m_subSystems!=null)
        {
            SubSystemInfo si = m_subSystems.get(subSystemName);
            if(si!=null)
            {
                return  si.hasWaferPresentInAnySlot();
            }
        }
        return false;
    }
}
