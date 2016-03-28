
package com.blogspot.carlosvalter.eyeblink;

/**
 *
 * @author Carlos Valter
 */
public class Main {
    public static void main(String[] args) {
        EyeBlink olho = new EyeBlink();
        olho.setLimiar(8);
        EventoEyeBlink eventoEye = new EventoEyeBlink(olho);
        olho.calibrar();
        olho.identificarPiscada();
    }
}
