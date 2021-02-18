/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.protocol.protocol1_10to1_11.storage;

import nl.matsv.viabackwards.api.entities.storage.EntityStorage;

public class ChestedHorseStorage implements EntityStorage {
    private boolean chested;
    private int liamaStrength;
    private int liamaCarpetColor = -1;
    private int liamaVariant;

    public boolean isChested() {
        return chested;
    }

    public void setChested(boolean chested) {
        this.chested = chested;
    }

    public int getLiamaStrength() {
        return liamaStrength;
    }

    public void setLiamaStrength(int liamaStrength) {
        this.liamaStrength = liamaStrength;
    }

    public int getLiamaCarpetColor() {
        return liamaCarpetColor;
    }

    public void setLiamaCarpetColor(int liamaCarpetColor) {
        this.liamaCarpetColor = liamaCarpetColor;
    }

    public int getLiamaVariant() {
        return liamaVariant;
    }

    public void setLiamaVariant(int liamaVariant) {
        this.liamaVariant = liamaVariant;
    }

    @Override
    public String toString() {
        return "ChestedHorseStorage{" + "chested=" + chested + ", liamaStrength=" + liamaStrength + ", liamaCarpetColor=" + liamaCarpetColor + ", liamaVariant=" + liamaVariant + '}';
    }
}
