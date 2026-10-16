package pt.unl.fct.di.syspriv.anonymity;

import java.util.*;

public class KAnonymityTool<T extends Anonymizable> {

    private final int k;
    private final float t;

    public KAnonymityTool(int k, float t) {
        this.k = k;
        this.t = t;
    }

    public boolean isKAnonymous(List<T> records) {
        for (T rec: records){
            int n = sizeEquivSet(rec, records);

            if (n < k)
                return false;
        }
        return true;
    }

    public boolean isTClose(List<T> P, List<T> Q){
        // TODO
        return false;
    }

    public Map<Map<String, Object>, Set<Object>> sensitiveValuesPerGroup(List<T> records) {
        // TODO
        return null;
    }

    public List<T> runMondrain(List<T> records) {

        List<T> leftEquivSet = new ArrayList<>();
        List<T> rightEquivSet = new ArrayList<>();

        for(String dim : records.get(0).getQuasiIdentifiers().keySet()){
            float median = getMedian(records, dim);
            for( T rec : records){
                if (rec.getQuasiIdentifiers().get(dim) < median)
                    leftEquivSet.add(rec);
                else
                    rightEquivSet.add(rec);
            }

        }


        return List.of(runMondrain(leftEquivSet), runMondrain(rightEquivSet));
    }

    private int sizeEquivSet(T rec, List<T> records){
        int n = 0;

        for(Anonymizable record : records){
            if(rec.haveSameQI(record))
                n++;
        }
        return n;
    }

    private float getMedian(List<T> records, String dim){
        List<Float> values = new ArrayList<>();
        for (T r : records) {
            values.add(((Number) r.getQuasiIdentifiers().get(dim)).floatValue());
        }
        Collections.sort(values);

        int n = values.size();
        if (n % 2 == 1)
            return values.get(n / 2);

        return ((values.get(n / 2 - 1) + values.get(n / 2)) / 2);
    }

}
