package pt.unl.fct.di.syspriv.anonymity;

import java.util.Map;

public interface Anonymizable {
    /** Returns the QI values as a map (name → value). */
    Map<String, Float> getQuasiIdentifiers();

    /** Returns the sensitive attribute value. */
    Object getSensitiveValue();

    boolean haveSameQI (Anonymizable other);

    void generalize(Map<String, Float> min, Map<String, Float> max);
}
