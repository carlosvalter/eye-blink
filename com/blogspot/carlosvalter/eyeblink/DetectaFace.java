package com.blogspot.carlosvalter.eyeblink;

import com.googlecode.javacv.*;
import com.googlecode.javacv.cpp.*;
import static com.googlecode.javacv.cpp.avcodec.*;
import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_highgui.CvCapture;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacv.cpp.opencv_calib3d.*;
import static com.googlecode.javacv.cpp.opencv_objdetect.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;

/**
 * Padrao Singleton
 *
 * @author carlos
 */
public class DetectaFace {

    private static final DetectaFace instanciaUnica = new DetectaFace();

    private CvSeq facesDetectada;
    private int xInicio;
    private int yInicio;
    private int largura;
    private int altura;
    private IplImage frame;

    private DetectaFace() {

    }

    public static DetectaFace getInstancia() {
        return instanciaUnica;
    }

    public CvSeq getFacesDetectada() {
        return facesDetectada;
    }

    public void setFacesDetectada(CvSeq facesDetectada) {
        this.facesDetectada = facesDetectada;
    }

    public IplImage getFrame() {
        return frame;
    }

    private void setFrame(IplImage frame) {
        this.frame = frame;
    }

    public CvSeq detectar(IplImage frame) {

        if (this.facesDetectada == null) { //Detecta um unica vez
            // ACRESCENTAR TRATAMENTO CASO NAO ACHE O ARQUIVO EXCEPTION
            //Carrega o arquivo xml de deteccao
            CvHaarClassifierCascade cascade = new CvHaarClassifierCascade(cvLoad(DetectaFace.class.getResource("haarcascade_frontalface_alt2.xml").getPath()));

            //Objeto temporario para uso na deteccao da face
            CvMemStorage storage = CvMemStorage.create();

            //Detecta as faces
//        CvSeq faceDetectada = cvHaarDetectObjects(frame, cascade, storage, 1.1, 4, CV_HAAR_DO_CANNY_PRUNING);
            CvSeq faceDetectada = cvHaarDetectObjects(frame, cascade, storage, 1.2, 1, CV_HAAR_DO_ROUGH_SEARCH | CV_HAAR_FIND_BIGGEST_OBJECT, cvSize(80, 80), cvSize(0, 0));

            //Libera memoria
            if (faceDetectada.total() > 0) {
                System.out.println("FACE DETECTADA: " + faceDetectada.total());
                this.setFacesDetectada(faceDetectada);
                if (largura == 0 && altura == 0) { //Localiza somente a primeira vez
                    CvRect temp = new CvRect();
                    CvRect maiorFace = new CvRect();

                    //Descobre a maior face na imagem
                    for (int i = 0; i <= this.getFacesDetectada().total(); i++) {
                        maiorFace = new CvRect(cvGetSeqElem(this.getFacesDetectada(), i));
                        if (maiorFace.width() < temp.width() && maiorFace.height() < temp.height()) {
                            maiorFace = temp;
                        } else {
                            temp = maiorFace;
                        }
                    }
                    xInicio = maiorFace.x();
                    yInicio = maiorFace.y();
                    largura = maiorFace.width();
                    altura = maiorFace.height();
                    System.out.println("DEFININDO DIMENCOES");
                }

                System.out.printf("x:%d y:%d w:%d h:%d ", xInicio, yInicio, largura, altura);
                cvClearMemStorage(storage);
//            cvReleaseMemStorage(storage);
                storage.release();
            } else {
                System.out.println("TENTANDO DETECTAR FACE");
            }
        }

        //Liberar memoria
//        frame.release();
        return this.facesDetectada;
    }

    /**
     *
     * @param frame
     * @return IplImage
     * @
     */
    public IplImage faceROI(IplImage frame) {

//        facesDetectada = this.facesDetectada;
        CvRect temp = new CvRect();
        CvRect maiorFace = new CvRect();
        try {
            if (this.getFacesDetectada().total() > 0) {

                if (largura == 0 && altura == 0) { //Localiza somente a primeira vez
                    //Descobre a maior face na imagem
                    for (int i = 0; i <= this.getFacesDetectada().total(); i++) {
                        maiorFace = new CvRect(cvGetSeqElem(this.getFacesDetectada(), i));
                        if (maiorFace.width() < temp.width() && maiorFace.height() < temp.height()) {
                            maiorFace = temp;
                            xInicio = maiorFace.x();
                            yInicio = maiorFace.y();
                            largura = maiorFace.width();
                            altura = maiorFace.height();
                        }
                    }
                }
//                System.out.println("Maior face localizada: "+ maiorFace.width());
                //Desenha um retangulo no rosto (TESTE)
//                cvRectangle(frame, cvPoint(
//                        maiorFace.x(), maiorFace.y()),
//                        cvPoint(maiorFace.x() + maiorFace.width(), maiorFace.y() + maiorFace.height()),
//                        CvScalar.RED, 2, CV_AA, 0);
                //Cria uma imagem com as mesma dimensoes ROI
                IplImage face = cvCreateImage(cvSize(largura, altura), frame.depth(), frame.nChannels());
                cvSetImageROI(frame, cvRect(xInicio, yInicio, largura, altura));

                try {
                    cvCopy(frame, face);
                } finally {
                    cvResetImageROI(frame);
                }

                //Liberar memoria
                frame.release();
                return face;
            } else {
                System.out.println("Face não detectada para criação do ROI");
                return frame;
            }

        } catch (Exception e) {
            System.out.println("Face nula: " + e);
        }

        return frame;
    }

    /**
     *
     * @param faceROI
     * @return IplImage
     */
    public IplImage olhosROI(IplImage faceROI) {
        cvSetImageROI(faceROI, cvRect(
                0, //X topo
                (int) faceROI.height() / 5, //Y Um pouco abaixo do topo
                faceROI.width() / 2,
                (int) ((int) faceROI.height() / 2.5)) //25% da altura da imagem
        );

        IplImage olhos = null;

        try {
            //Cria uma imagem com as mesmas caracteristica da faceROI
            olhos = cvCreateImage(cvGetSize(faceROI), faceROI.depth(), faceROI.nChannels());

            //Copia o ROI da imagem para uma nova imagem
            cvCopy(faceROI, olhos);
        } finally {
            //Reseta o ROI da imagem
            cvResetImageROI(faceROI);
        }

        return olhos;
    }

    /**
     * Nao estou usando no projeto final
     * @param olhosROI
     * @return 
     */
    public CvSeq detectaOlhos(IplImage olhosROI) {
        // ACRESCENTAR TRATAMENTO CASO NAO ACHE O ARQUIVO EXCEPTION
        //Carrega o arquivo xml de deteccao
        CvHaarClassifierCascade cascade = new CvHaarClassifierCascade(cvLoad(DetectaFace.class.getResource("haarcascade_eye.xml").getPath()));

        //Objeto temporario para uso na deteccao da face
        CvMemStorage storage = CvMemStorage.create();

        //Detecta os olhos
        CvSeq olhosDetectado = cvHaarDetectObjects(olhosROI, cascade, storage, 1.1, 3, CV_HAAR_DO_CANNY_PRUNING);

        //Libera memoria
//        cvReleaseHaarClassifierCascade(cascade);
//        cvReleaseMemStorage(storage);
        storage.release();

        return olhosDetectado;

    }
}
