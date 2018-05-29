package kcp;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class Test {
    public static void main(String[] args) {

        System.out.println(ByteOrder.nativeOrder());



        log(1,2);

        ArrayList list = new ArrayList<Integer>();

        for (int i = 0; i < 10; i++)
            list.add(i);

        slice(list, 5, 8);

        System.out.println(list);
    }

    public static void log(Object... args){
        String str = String.format(" %d %d", args);
        System.out.println(str);
    }

    public static void slice(ArrayList list, int start, int stop) {
        int size = list.size();
        for (int i = 0; i < size; ++i) {
            if (i < stop - start) {
                list.set(i, list.get(i + start));
            } else {
                list.remove(stop - start);
            }
        }
    }
}
