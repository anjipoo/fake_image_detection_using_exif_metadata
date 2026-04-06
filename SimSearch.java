//search for similar hash in db 

import java.util.Map;

public class SimSearch {

    public static class Result {
        public String name;
        public double similarity;
        public Result(String n, double s) { name = n; similarity = s; }
    }

    public static Result findClosest(long queryHash, Map<String, Long> db) {
        String best = null;
        int bestDist = Integer.MAX_VALUE;
        for (Map.Entry<String, Long> e : db.entrySet()) {
            int d = ImageHash.hamming(queryHash, e.getValue());
            if (d < bestDist) { bestDist = d; best = e.getKey(); }
        }
        if (best == null) return new Result("none", 0);
        return new Result(best, ImageHash.similarity(queryHash, db.get(best)));
    }
}