package org.bytedeco.javacv.facepreview;

import static org.bytedeco.javacpp.helper.opencv_objdetect.cvHaarDetectObjects;
import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_8U;
import static org.bytedeco.javacpp.opencv_core.cvClearMemStorage;
import static org.bytedeco.javacpp.opencv_core.cvGetSeqElem;
import static org.bytedeco.javacpp.opencv_core.cvLoad;
import static org.bytedeco.javacpp.opencv_objdetect.CV_HAAR_DO_CANNY_PRUNING;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_core.CvMemStorage;
import org.bytedeco.javacpp.opencv_core.CvRect;
import org.bytedeco.javacpp.opencv_core.CvSeq;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_objdetect.*;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.util.Log;
import android.view.View;

@SuppressLint("DrawAllocation")
public class FaceView extends View implements Camera.PreviewCallback {
    public static final int SUBSAMPLING_FACTOR = 4;

    private IplImage grayImage;
    private final CvHaarClassifierCascade classifierA;
    private final CvHaarClassifierCascade classifierB;
    private final CvMemStorage storage;
    private CvSeq facesA=null;
    private CvSeq facesB=null;

    public FaceView(Context context) throws IOException {
            super(context);

            // Load the classifier file from Java resources.
            File classifierFileA = Loader
                            .extractResource(
                                            getClass(),
                                            "/org/bytedeco/javacv/facepreview/A.xml",
                                            context.getCacheDir(), "classifier", ".xml");
            File classifierFileB = Loader
                    .extractResource(
                                    getClass(),
                                    "/org/bytedeco/javacv/facepreview/B.xml",
                                    context.getCacheDir(), "classifier", ".xml");
            if (classifierFileA == null || classifierFileA.length() <= 0) {
                    throw new IOException(
                                    "Could not extract the classifier file from Java resource.");
            }

            // Preload the opencv_objdetect module to work around a known bug.
            Loader.load(org.bytedeco.javacpp.opencv_objdetect.CvHaarClassifierCascade.class);
            classifierA = new CvHaarClassifierCascade(
                            cvLoad(classifierFileA.getAbsolutePath()));
            classifierB = new CvHaarClassifierCascade(
                    cvLoad(classifierFileB.getAbsolutePath()));
            classifierFileA.delete();
            classifierFileB.delete();
            if (classifierA.isNull()) {
                    throw new IOException("Could not load the classifier file.");
            }
            storage = CvMemStorage.create();
    }

    @Override
    public void onPreviewFrame(final byte[] data, final Camera camera) {
            try {
                    Camera.Size size = camera.getParameters().getPreviewSize();
                    processImage(data, size.width, size.height);
                    camera.addCallbackBuffer(data);
            } catch (RuntimeException e) {
                    // The camera has probably just been released, ignore.
            }
    }

    protected void processImage(byte[] data, int width, int height) {
            // First, downsample our image and convert it into a grayscale IplImage
            int f = SUBSAMPLING_FACTOR;
            if (grayImage == null || grayImage.width() != width / f
                            || grayImage.height() != height / f) {
                    grayImage = IplImage.create(width / f, height / f, IPL_DEPTH_8U, 1);
            }
            int imageWidth = grayImage.width();
            int imageHeight = grayImage.height();
            int dataStride = f * width;
            int imageStride = grayImage.widthStep();
            ByteBuffer imageBuffer = grayImage.getByteBuffer();
            for (int y = 0; y < imageHeight; y++) {
                    int dataLine = y * dataStride;
                    int imageLine = y * imageStride;
                    for (int x = 0; x < imageWidth; x++) {
                            imageBuffer.put(imageLine + x, data[dataLine + f * x]);
                    }
            }

            facesA = cvHaarDetectObjects(grayImage, classifierA, storage, 1.1, 3,
                            CV_HAAR_DO_CANNY_PRUNING);
            facesB = cvHaarDetectObjects(grayImage, classifierB, storage, 1.1, 3,
                    CV_HAAR_DO_CANNY_PRUNING);
            postInvalidate();
            cvClearMemStorage(storage);
    }

    @Override
    protected void onDraw(Canvas canvas) {
            Paint paint = new Paint();
            paint.setColor(Color.BLUE);
            paint.setTextSize(20);

            String s = "HandDetection - Bagian Atas.";
            float textWidth = paint.measureText(s);
            canvas.drawText(s, (getWidth() - textWidth) / 2, 20, paint);
            Paint paintResult= new Paint();

            if (facesA != null && facesB !=null ) {
//            		Paint paintResult= new Paint();
            		
                    int totalA = facesA.total();
                    int b=facesB.total();
                    Log.e("TOtal Semua", "A: "+totalA+" B: "+b);
                    if(totalA==1)
                    {
                    	Log.i("total A", "Total A: "+totalA);
                    	paintResult.setColor(Color.RED);
                		paintResult.setTextSize(50);
                		String hasil="A";
                		float hasilWidth=paintResult.measureText(hasil);
                		canvas.drawText(hasil, (getWidth() - hasilWidth)/2, 30, paintResult);
                		Log.d("Lokasi", "Terdeteksi");
                        paint.setStrokeWidth(2);
                        paint.setStyle(Paint.Style.STROKE);
                        float scaleX = (float) getWidth() / grayImage.width();
                        float scaleY = (float) getHeight() / grayImage.height();
                        for (int i = 0; i < totalA; i++) {
                            CvRect r = new CvRect(cvGetSeqElem(facesA, i));
                            int x = r.x(), y = r.y(), w = r.width(), h = r.height();
                            canvas.drawRect(x * scaleX, y * scaleY, (x + w) * scaleX,
                                            (y + h) * scaleY, paint);
                        }
                    }else
                    {
	                    if (facesB != null ) 
	                    {
	//                		Paint paintResult= new Paint();
	                		
	                        
	                        int totalB = facesB.total();
	                        if(totalB==1)
	                        {
	                        	Log.i("total B", "Total B: "+totalB);
	                        	paintResult.setColor(Color.RED);
	                    		paintResult.setTextSize(50);
	                    		String hasil="B";
	                    		float hasilWidth=paintResult.measureText(hasil);
	                    		canvas.drawText(hasil, (getWidth() - hasilWidth)/2, 30, paintResult);
	                    		Log.d("Lokasi", "Terdeteksi");
	                            paint.setStrokeWidth(2);
	                            paint.setStyle(Paint.Style.STROKE);
	                            float scaleX = (float) getWidth() / grayImage.width();
	                            float scaleY = (float) getHeight() / grayImage.height();
	                            for (int i = 0; i < totalB; i++) {
	                                CvRect r = new CvRect(cvGetSeqElem(facesB, i));
	                                int x = r.x(), y = r.y(), w = r.width(), h = r.height();
	                                canvas.drawRect(x * scaleX, y * scaleY, (x + w) * scaleX,
	                                                (y + h) * scaleY, paint);
	                            }
	                        }
	                   
	                    }
                    }
            
            }
    }
}

