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
        for (T record : records) {
            int n = sizeEquivSet(record, records);
            if (n < k) return false;
        }
        return true;
    }

    public boolean isTClose(List<T> P, List<T> Q){
        if (P.isEmpty() || Q.isEmpty())
            return false;

        List<Float> pList = new ArrayList<>(); // p list with only sensitive values
        List<Float> qList = new ArrayList<>(); // q list with only sensitive values
        for (T rec : P)
            pList.add(((Number) rec.getSensitiveValue()).floatValue());
        for (T rec : Q)
            qList.add(((Number) rec.getSensitiveValue()).floatValue());

        TreeSet<Float> qSet = new TreeSet<>(qList);
        qSet.addAll(pList);
        float sum = 0;
        float running = 0;

        for (Float q : qSet){
            float shareQ = sizeSensitiveSet(q, qList) / (float) Q.size();
            float shareP = sizeSensitiveSet(q, pList) / (float) P.size();
            running += shareP - shareQ;
            sum += Math.abs(running);
        }

        return sum / (qSet.size() - 1) <= t;
    }

    public Map<Map<String, Object>, Set<Object>> sensitiveValuesPerGroup(List<T> records) {
        Map<Map<String, Object>, Set<Object>> groups = new HashMap<>();

        for (T rec : records) {
            Map<String, Object> qi = new HashMap<>(rec.getQuasiIdentifiers());

            if (!groups.containsKey(qi)) {
                groups.put(qi, new HashSet<>());
            }

            groups.get(qi).add(rec.getSensitiveValue());
        }

        return groups;
    }

    public List<T> runMondrain(List<T> records) {
        if (records.isEmpty())
            return records;

        for(String dim : records.get(0).getQuasiIdentifiers().keySet()){
            List<T> leftEquivSet = new ArrayList<>();
            List<T> rightEquivSet = new ArrayList<>();

            float median = getMedian(records, dim);

            for(T rec : records){
                float value = rec.getQuasiIdentifiers().get(dim);

                if (value < median){
                    leftEquivSet.add(rec);
                }
                else{
                    rightEquivSet.add(rec);
                }
            }

            if(leftEquivSet.size() >= k && rightEquivSet.size() >= k){ // split again
                List<T> newLeftEquivSet = new ArrayList<>(runMondrain(leftEquivSet));
                List<T> newRightEquivSet = new ArrayList<>(runMondrain(rightEquivSet));
                newLeftEquivSet.addAll(newRightEquivSet);
                return newLeftEquivSet;
            }

            // try different dim
        }

        return generalizePartition(records);
    }

    public List<T> runTCloseMondrain(List<T> records) {
        return runTCloseMondrain(records, records);
    }

    public List<T> runTCloseMondrain(List<T> records, List<T> originalRecords) {
        if (records.isEmpty())
            return records;

        for(String dim : records.get(0).getQuasiIdentifiers().keySet()){
            List<T> leftEquivSet = new ArrayList<>();
            List<T> rightEquivSet = new ArrayList<>();

            float median = getMedian(records, dim);

            for(T rec : records){
                float value = rec.getQuasiIdentifiers().get(dim);

                if (value < median){
                    leftEquivSet.add(rec);
                }
                else{
                    rightEquivSet.add(rec);
                }
            }

            if(leftEquivSet.size() >= k && rightEquivSet.size() >= k
                && isTClose(leftEquivSet, originalRecords) && isTClose(rightEquivSet, originalRecords)){ // split again
                List<T> newLeftEquivSet = new ArrayList<>(runTCloseMondrain(leftEquivSet, originalRecords));
                List<T> newRightEquivSet = new ArrayList<>(runTCloseMondrain(rightEquivSet, originalRecords));
                newLeftEquivSet.addAll(newRightEquivSet);
                return newLeftEquivSet;
            }

            // try different dim
        }

        return generalizePartition(records);
    }

    private int sizeEquivSet(T rec, List<T> records){
        int n = 0;

        for(Anonymizable record : records){
            if(rec.haveSameQI(record))
                n++;
        }
        return n;
    }

    private int sizeSensitiveSet(float value, List<Float> values){
        int n = 0;

        for(float v : values){
            if(v == value)
                n++;
        }
        return n;
    }

    private float getMedian(List<T> records, String dim){
        List<Float> values = new ArrayList<>();
        for (T r : records) {
            values.add(r.getQuasiIdentifiers().get(dim));
        }
        Collections.sort(values);

        int n = values.size();
        if (n % 2 == 1)
            return values.get(n / 2);

        return ((values.get(n / 2 - 1) + values.get(n / 2)) / 2);
    }

    private List<T> generalizePartition(List<T> records) {
        Map<String, Float> min = new HashMap<>();
        Map<String, Float> max = new HashMap<>();

        for (String dim : records.get(0).getQuasiIdentifiers().keySet()) {
            float currentMin = Float.MAX_VALUE;
            float currentMax = -Float.MAX_VALUE;

            for (T rec : records) {
                float value = rec.getQuasiIdentifiers().get(dim);

                currentMin = Math.min(currentMin, value);
                currentMax = Math.max(currentMax, value);
            }

            min.put(dim, currentMin);
            max.put(dim, currentMax);
        }

        for (T rec : records) {
            rec.generalize(min, max);
        }

        return records;
    }
}
