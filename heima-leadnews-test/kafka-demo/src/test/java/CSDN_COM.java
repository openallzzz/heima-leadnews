import java.util.*;

public class CSDN_COM {
    public static void main(String[] args) {
        int n = 13;
        int[] arr = new int[n];

        for(int i = 0; i < n; i ++) {
            arr[i] = i % 2;
        }

        arr[10] = 6;
        arr[11] = 6;
        arr[12] = 7;

        Set<Integer> set = new HashSet<>();
        List<Integer> list = new ArrayList<Integer>();

        for(int i = 0; i < n; i ++) {
            int cur = arr[i];
            if(set.contains(cur)) {

            } else {
                list.add(cur);
            }
            set.add(cur);
        }

        for(Integer x : list) {
            System.out.println(x);
        }
    }
}