package data_processer;

import org.bson.*;

public class DocFreqPair {
    private Document doc;
    private int freq;

    public DocFreqPair(Document doc, int freq) {
        this.doc = doc;
        this.freq = freq;
    }

    public Document getDoc() {
        return doc;
    }

    public void setDoc(Document doc) {
        this.doc = doc;
    }

    public int getFreq() {
        return freq;
    }

    public void setFreq(int freq) {
        this.freq = freq;
    }



}
