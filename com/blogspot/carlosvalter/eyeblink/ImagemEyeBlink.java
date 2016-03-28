package com.blogspot.carlosvalter.eyeblink;

import com.googlecode.javacv.cpp.opencv_core;
import static com.googlecode.javacv.cpp.opencv_core.cvGet2D;
import static com.googlecode.javacv.cpp.opencv_core.cvSet2D;
import static com.googlecode.javacv.cpp.opencv_highgui.cvLoadImage;
import static com.googlecode.javacv.cpp.opencv_highgui.cvSaveImage;
import com.googlecode.javacv.cpp.opencv_imgproc;

/**
 *
 * @author Carlos Valter
 */
public class ImagemEyeBlink {

    private int iluminacao;
    private opencv_core.IplImage mascara;

    public opencv_core.IplImage getMascara() {
        return mascara;
    }

    public void setMascara(opencv_core.IplImage mascara) {
        this.mascara = mascara;
    }

    public int getIluminacao() {
        return iluminacao;
    }

    public void setIluminacao(int iluminacao) {
        this.iluminacao = iluminacao;
    }

    public opencv_core.IplImage transformarCinza(opencv_core.IplImage imagem) {
        opencv_core.IplImage imagemCinza = opencv_core.IplImage.create(imagem.width(), 
                imagem.height(), opencv_core.IPL_DEPTH_8U, 1);
        opencv_imgproc.cvCvtColor(imagem, imagemCinza, opencv_imgproc.CV_BGR2GRAY);

        //Libera memoria
        imagem.release();

        return imagemCinza;
    }

    public opencv_core.IplImage reduzir(opencv_core.IplImage imagem, int percentualScala) {
        double escala = percentualScala / 100; //Percentual de reducao
        int largura = (int) (imagem.width() * escala);
        int altura = (int) (imagem.height() * escala);

        opencv_core.IplImage imagemMenor = opencv_core.IplImage.create(largura, altura, imagem.depth(), imagem.nChannels());

        //As propriedades da imagemMenor tem q ser exatamente igual que imagem
        opencv_imgproc.cvResize(imagem, imagemMenor, opencv_imgproc.CV_INTER_LINEAR);

        //Libera memoria
        imagem.release();

        return imagemMenor;
    }

    public opencv_core.IplImage equalizarHistograma(opencv_core.IplImage imagem) {
        opencv_core.IplImage imagemEqualizada = opencv_core.IplImage.create(imagem.width(), 
                imagem.height(), opencv_core.IPL_DEPTH_8U, 1);
        opencv_imgproc.cvEqualizeHist(imagem, imagemEqualizada);

        //Libera memoria
        imagem.release();

        return imagemEqualizada;
    }

    /**
     *
     * @param imagem em tom de cinza
     * @return
     */
    public opencv_core.IplImage limiarizar(opencv_core.IplImage imagem, int iluminacao) {

        opencv_core.IplImage imagemLimiar = opencv_core.IplImage.create(imagem.width(), imagem.height(), 
                imagem.depth(), imagem.nChannels());
        opencv_imgproc.cvThreshold(imagem, imagemLimiar, iluminacao, 255, opencv_imgproc.CV_THRESH_BINARY);

        //Libera memoria
        imagem.release();

        return imagemLimiar;

    }

    public int percentPreto(opencv_core.IplImage imagem) {
        opencv_core.CvScalar pixel;

        int totalPixel = imagem.width() * imagem.height();
        double corPixel;
        int contPreto = 0;

        for (int y = 0; y < imagem.height(); y++) {
            for (int x = 0; x < imagem.width(); x++) {
                pixel = cvGet2D(imagem, y, x);
                corPixel = pixel.val(0);
                if (corPixel <= 5.0) {
                    ++contPreto;
                }
            }
        }

        return (contPreto * 100) / totalPixel;
    }

    /**
     *
     * @param mascaraPath - Caminho com nome do arquivo JPG da mascara
     * @param framePath - Caminho com nome do arquivo JPG dos frame
     */
    public void montarMascara(String mascaraPath, String framePath) {

        opencv_core.IplImage mascara = cvLoadImage(mascaraPath);
        opencv_core.IplImage frame = cvLoadImage(framePath);

        opencv_core.CvScalar pixelFrame;
        double corPixelFrame;

        for (int y = 0; y < mascara.height(); y++) {
            for (int x = 0; x < mascara.width(); x++) {
                pixelFrame = cvGet2D(frame, y, x);
                corPixelFrame = pixelFrame.val(0);
                if (corPixelFrame <= 5.0) { //Color black
                    cvSet2D(mascara, y, x, pixelFrame); //Copia o pixel preto para a mascara
                }
            }
        }

        mascara = transformarCinza(mascara); //Converte para cinza

        cvSaveImage(mascaraPath, mascara);

        //Libera memoria
        mascara.release();
        frame.release();

    }

    /**
     * Compara o percentual de igualdade entre o frame e mascara
     *
     * @param frame
     * @return percentual
     */
    public double piscar(opencv_core.IplImage frame) {

        int totalPixelPretoMascara = 0;
        int contPixelPretoAND = 0;
        int contPixelPretoMascara = 0;
        int contPixelPretoFrame = 0;

        double corPixelFrame, corPixelMascara;

        opencv_core.CvScalar pixelFrame, pixelMascara;

        for (int y = 0; y < mascara.height(); y++) {
            for (int x = 0; x < mascara.width(); x++) {
                pixelFrame = cvGet2D(frame, y, x);
                pixelMascara = cvGet2D(mascara, y, x);

                corPixelFrame = pixelFrame.val(0);
                corPixelMascara = pixelMascara.val(0);

                if (corPixelMascara <= 5.0) { //Se for preto
                    ++totalPixelPretoMascara;
                }

                //Faz um AND entre a imagem e a mascara
                if (corPixelFrame == corPixelMascara && corPixelMascara <= 5.0) { // Se ambos forem preto
                    ++contPixelPretoAND;
                }
            }
        }

        return (contPixelPretoAND * 100) / totalPixelPretoMascara; // calculo percentual
    }
}
