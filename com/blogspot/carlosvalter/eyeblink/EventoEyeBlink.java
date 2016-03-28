package com.blogspot.carlosvalter.eyeblink;

import java.util.Observable;
import java.util.Observer;

/**
 *
 * @author Carlos Valter
 */
public class EventoEyeBlink implements Observer {

    EyeBlink eye;
    int contInvoluntario;
    int contNormal;
    int contLonga;

    public EventoEyeBlink(EyeBlink eye) {
        this.eye = eye;
        eye.addObserver(this); //Adiciona o objeto na lista de Observer para ser notificado
    }

    @Override
    public void update(Observable o, Object arg) {
        if (eye.getTempo() < 200) {
            contInvoluntario++;
            System.out.println(contInvoluntario + " - Piscou involuntario: " + eye.getTempo() + " ms.");
        } else if (eye.getTempo() > 200 && eye.getTempo() <= 600) {
            contNormal++;
            System.out.println(contNormal + " - Piscou normal: " + eye.getTempo() + " ms.");
        } else {
            contLonga++;
            System.out.println(contLonga + " - Piscou Longa: " + eye.getTempo() + " ms.");
        }
    }

}
