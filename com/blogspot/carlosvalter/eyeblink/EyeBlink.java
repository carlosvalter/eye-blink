package com.blogspot.carlosvalter.eyeblink;

import com.googlecode.javacv.cpp.opencv_core;
import static com.googlecode.javacv.cpp.opencv_core.cvReleaseImage;
import com.googlecode.javacv.cpp.opencv_highgui;
import static com.googlecode.javacv.cpp.opencv_highgui.CV_CAP_PROP_FRAME_HEIGHT;
import static com.googlecode.javacv.cpp.opencv_highgui.CV_CAP_PROP_FRAME_WIDTH;
import static com.googlecode.javacv.cpp.opencv_highgui.cvCreateCameraCapture;
import static com.googlecode.javacv.cpp.opencv_highgui.cvDestroyWindow;
import static com.googlecode.javacv.cpp.opencv_highgui.cvQueryFrame;
import static com.googlecode.javacv.cpp.opencv_highgui.cvReleaseCapture;
import static com.googlecode.javacv.cpp.opencv_highgui.cvSaveImage;
import static com.googlecode.javacv.cpp.opencv_highgui.cvSetCaptureProperty;
import static com.googlecode.javacv.cpp.opencv_highgui.cvShowImage;
import static com.googlecode.javacv.cpp.opencv_highgui.cvWaitKey;
import java.util.Date;
import java.util.Observable;

/**
 *
 * @author Carlos Valter
 */
public class EyeBlink extends Observable {

    private boolean piscou;
    private boolean fechado;
    private boolean aberto;

    private boolean iniciarCalibragem;
    private boolean calibrado;
    private long tempo;
    private int limiar;
    private int limiarClassificacao;
    private opencv_core.CvSeq faceDetectada;
    private opencv_highgui.CvCapture camera;
    private opencv_core.IplImage frameFinal;

    public EyeBlink() {
        //Capturar camera
        if (getCamera() == null) {
            setCamera(0);
        }

        //Configura as propriedade de captura
        cvSetCaptureProperty(camera, CV_CAP_PROP_FRAME_WIDTH, 640);
        cvSetCaptureProperty(camera, CV_CAP_PROP_FRAME_HEIGHT, 480);
        setLimiar(10);
        setLimiarClassificacao(10);
    }

    /**
     * Esse metodo notifica a classe Observable (Subject ou Observavel do
     * Designer Pattern Observer), e a mesma notifica os objetos observados
     * (Observer ou Observador do Designer Pattern Observer)
     *
     * @param boolean piscou
     */
    private void setPiscou(boolean piscou) {
        this.piscou = piscou;

        //Notifica os objetos Observador (Padrao de projeto Observer)
        setChanged();
        notifyObservers();
    }

    /**
     * Esse metodo server para iniciar a calibragem
     *
     * @param iniciarCalibragem
     */
    public void setIniciarCalibragem(boolean iniciarCalibragem) {
        this.iniciarCalibragem = iniciarCalibragem;
    }

    /**
     * Retorna o frame atual da câmera já tratado.
     *
     * @return opencv_core.IplImage
     */
    public opencv_core.IplImage getFrameFinal() {
        return frameFinal;
    }

    private void setFrameFinal(opencv_core.IplImage frameFinal) {
        this.frameFinal = frameFinal;
    }

    /**
     * Retorna quantos milisegundos o olho ficou fechado, isso pode ser usado
     * para definir piscadas involuntárias, curtas ou longas. Piscadas
     * involuntárias em geral são menores que 200ms
     *
     * @return long
     */
    public long getTempo() {
        return tempo;
    }

    /**
     * Retorna o percentual de diferença entre a calibração e o olho fechado,
     * para detectar que o olho fechou em x%, isso varia de pessoa, cor da pele
     * e iluminação.
     *
     * @return int
     */
    public int getLimiarClassificacao() {
        return limiarClassificacao;
    }

    /**
     * Seta o percentual de diferença entre a calibração e o olho fechado, para
     * detectar que o olho fechou em x%, isso varia de pessoa, cor da pele e
     * iluminação.
     *
     * @param limiarClassificacao Default (10)
     */
    public void setLimiarClassificacao(int limiarClassificacao) {
        this.limiarClassificacao = limiarClassificacao;
    }

    /**
     * Retorna o valor do limiar usado para criar a imagem binária (branco e
     * preto)
     *
     * @return int
     */
    public int getLimiar() {
        return limiar;
    }

    /**
     * Seta o valor do limiar para criar a imagem binária (branco e preto)
     *
     * @param limiar Default (10)
     */
    public void setLimiar(int limiar) {
        this.limiar = limiar;
    }

    /**
     * Retorna o número que corresponde a camera usada atualmente
     *
     * @return int
     */
    public opencv_highgui.CvCapture getCamera() {
        return camera;
    }

    /**
     * O dipositivo camera é representado por um numero inteiro pelo sistema,
     * sendo 0 a primeira camera encontrada no sistema
     *
     * @param camera int Default (0)
     */
    public void setCamera(int camera) {
        this.camera = cvCreateCameraCapture(camera);
    }

    /**
     * Retorna a quantidade de faces detectada pela câmera, o ideal é encontrar
     * somente uma face
     *
     * @return int
     */
    public opencv_core.CvSeq getFaceDetectada() {
        return faceDetectada;
    }

    private void setFaceDetectada(opencv_core.CvSeq faceDetectada) {
        this.faceDetectada = faceDetectada;
    }

    /**
     * Retorna se a biblioteca já esta calibrado para uso. Calibrar é o ato de
     * capturar o olho aberto para usar como referência quando for definir que o
     * olho fechou.
     *
     * @return boolean
     */
    public boolean isCalibrado() {
        return calibrado;
    }

    private void setCalibrado(boolean calibrado) {
        this.calibrado = calibrado;
    }

    /**
     * Retorna se o olho deu uma piscada completa, ou seja, fechou e abriu
     *
     * @return boolean
     */
    public boolean isPiscou() {
        return piscou;
    }

    /**
     * Executa a calibragem do sistema, criando uma referência para o sistema
     * definir se o olho piscou. Durante a calibragem o usuário deverá olhar no
     * centro da parte superior da tela do computador e mater os olhos ABERTO.
     * Durante a calibragem e uso geral da biblioteca o usuário deverá
     * permanecer imóvel, Caso se mova deverá recalibrar o sistema
     */
    public void calibrar() {
        //Cria o frame
        opencv_core.IplImage frame;

        //Cria objeto para detectar faces
        DetectaFace faces = DetectaFace.getInstancia();

        //Imagem
        ImagemEyeBlink imagem = new ImagemEyeBlink();
        imagem.setIluminacao(getLimiar());

        boolean flagMascara = false; // Flag para criar a mascara de calibragem
        int contMascara = 0;         // Contador para salvar 10 imagem para criacao da mascara
        String mascaraPath = "mascara.jpg";

        for (;;) {
            //Captura o frame atual
            frame = cvQueryFrame(camera);

            //Cinza
            frame = imagem.transformarCinza(frame);
            //Histograma
            frame = imagem.equalizarHistograma(frame);
            //Exibi o frame na tela

            faceDetectada = faces.detectar(frame);

            try {
                if (faceDetectada.total() > 0) {
                    //Recortar a area do rosto
                    frame = faces.faceROI(frame);

                    //Recorta a area dos olhos
                    frame = faces.olhosROI(frame);

                    frame = imagem.limiarizar(frame, imagem.getIluminacao());

                } else {
                    faces.setFacesDetectada(null); //Null para fazer uma nova deteccao de face
                }
            } catch (NullPointerException e) {
                System.out.println("Face não detectada, ajuste a posição da camera ou iluminação");
            }

            //Criar saida de um frame IplImage
            //Descomente as proximas 2 linha para fazer teste na Eye Blink
            opencv_highgui.cvMoveWindow("Camera", 600, 10); //Posicao da tela
            cvShowImage("Camera", frame);
            setFrameFinal(frame); // Seta o frame já tratado para o atributo frameFinal, onde o programador poderá usar

            //Ler teclado
            char tecla = (char) cvWaitKey(15);
            if (tecla == 27) {
                break;
            }

            //Somente para uso em testes da Eye Blink
            if (tecla == 115) { //letra s
                if (frame != null) { //Salvar imagem
                    System.out.println("Salvando imagem: frame_Eye_Blink.jpg");
                    cvSaveImage("frame_Eye_Blink.jpg", frame);
                }
            }

            //Somente para uso em testes da Eye Blink
            if (tecla == 73) { //letra I: controlar iluminacao
                imagem.setIluminacao(imagem.getIluminacao() - 1);
                setLimiar(imagem.getIluminacao());
                System.out.printf("Limiar: %d\n", imagem.getIluminacao());
            }

            //Somente para uso em testes da Eye Blink
            if (tecla == 105) { //letra i: controlar iluminacao
                imagem.setIluminacao(imagem.getIluminacao() + 1);
                setLimiar(imagem.getIluminacao());
                System.out.printf("Limiar: %d\n", imagem.getIluminacao());
            }

            if (iniciarCalibragem || flagMascara || tecla == 99) { //letra c

                if (faceDetectada.total() > 0) {
                    if (contMascara < 10) { //Salva 10 frames para criar a mascara
                        flagMascara = true;
                        ++contMascara;
                        System.out.println("Criando Mascara: mascara_" + contMascara + ".jpg");
                        cvSaveImage("mascara_" + contMascara + ".jpg", frame);
                    } else {
                        flagMascara = false;
                        //Criar a mascara
                        cvSaveImage(mascaraPath, frame);
                        String framePath;
                        //Monta a mascara baseado nos 10 frames capturados
                        for (contMascara = 1; contMascara <= 10; contMascara++) {
                            framePath = "mascara_" + contMascara + ".jpg";

                            imagem.montarMascara(mascaraPath, framePath);
                        }

                        setCalibrado(true); // camera ajustada com o olho
                        break;
                    }
                } else {
                    System.out.println("ATENCAO: Rosto não detectado!");
                }
            }

            //Libera memoria
            frame.release(); //Vai aumentando o consumo de memoria
        }

        //Libera memoria
        frame.release();
        cvDestroyWindow("Camera");
    }

    /**
     * Para realizar a captura o sistema deverá já esta calibrado. Durante a
     * calibragem e uso geral da biblioteca o usuário deverá permanecer imóvel,
     * Caso se mova deverá recalibrar o sistema
     *
     */
    public void identificarPiscada() {

        long tempoInicial = new Date().getTime();
        //Criar frame
        opencv_core.IplImage frame;

        //Cria objeto para detectar faces
        DetectaFace faces = DetectaFace.getInstancia();

        //Imagem
        ImagemEyeBlink imagem = new ImagemEyeBlink();

        imagem.setIluminacao(getLimiar());
        String mascaraPath = "mascara.jpg";
        double percentualPiscar = 0.0;

        imagem.setMascara(opencv_highgui.cvLoadImage(mascaraPath));

        int cont = 0;
        for (;;) {
            //Captura o frame atual
            try {
                frame = cvQueryFrame(camera);

                //Cinza
                frame = imagem.transformarCinza(frame);
                //Histograma
                frame = imagem.equalizarHistograma(frame);
                //Exibi o frame na tela

                faceDetectada = faces.detectar(frame);
                try {
                    if (faceDetectada.total() > 0) {
                        //Recortar a area do rosto
                        frame = faces.faceROI(frame);
                        //Recorta a area dos olhos
                        frame = faces.olhosROI(frame);

                        frame = imagem.limiarizar(frame, imagem.getIluminacao());

                        if (isCalibrado()) {
                            //Compara o percentual de igualdade entre o frame e mascara
                            percentualPiscar = imagem.piscar(frame);
                            if (percentualPiscar <= getLimiarClassificacao()) {
                                if (aberto) {
                                    tempoInicial = new Date().getTime(); // Pega o tempo atual em milisegundos
                                } else {
                                    // Calcula quantos milisegundos o olho esta fechado
                                    tempo = new Date().getTime() - tempoInicial;
                                }
                                fechado = true;
                                aberto = false;
                            } else {
                                if (fechado) {
                                    setPiscou(true);
                                    tempo = 0;
                                }
                                aberto = true;
                                fechado = false;
                            }
                        } else {
                            System.out.println("Equipamento não calibrado!");
                        }
                    } else {
                        faces.setFacesDetectada(null); //Null para fazer uma nova deteccao de face
                    }
                } catch (NullPointerException e) {
                    System.out.println("Face não detectada, ajuste a posição da camera ou iluminação");
                }

                //Descomente as proximas 2 linha para fazer teste na Eye Blink
                opencv_highgui.cvMoveWindow("Camera", 600, 10); //Posicao da tela
                cvShowImage("Camera", frame);
                setFrameFinal(frame); // Seta o frame já tratado para o atributo frameFinal, onde o programador poderá usar

                //Ler teclado
                char tecla = (char) cvWaitKey(15);
                if (tecla == 27) {
                    break;
                }

                if (tecla == 115) { //letra s
                    if (frame != null) { //Salvar imagem
                        ++cont;
                        System.out.println("Salvando imagem: olhos_" + cont + ".jpg");
                        cvSaveImage("olhos_" + cont + ".jpg", frame);
                    }
                }

                //Libera memoria
                frame.release(); //Se nao usar Vai aumentando o consumo de memoria
            } catch (NullPointerException e) {
                System.out.println("Frame vazio, não foi possivel capturar da camera.");
            }
        }
        //Libera memoria
        cvReleaseImage(frame);
        cvReleaseCapture(camera);
        cvDestroyWindow("Camera");
    }
}
