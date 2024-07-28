package data_processer;

import org.bson.*;

public class DocFreqPair {
    private Document doc;
    private int Freq;

    public DocFreqPair(Document doc, int Freq) {
        this.doc = doc;
        this.Freq = Freq;
    }

    public Document getDoc() {
        return doc;
    }

    public void setDoc(Document doc) {
        this.doc = doc;
    }

    public int getFreq() {
        return Freq;
    }

    public void setFreq(int freq) {
        Freq = freq;
    }



}
