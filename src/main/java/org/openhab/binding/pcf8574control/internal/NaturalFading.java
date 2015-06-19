package org.openhab.binding.pcf8574control.internal;

/**
 * Created by Michael on 03.06.2015.
 * NaturalFading means the logarithmic brightness view of the human eye.
 * A linear PWM up-fading looks like a fast rise in the beginning (by the lower PWM values) and a slow end,
 * where differences nearly aren't visible...
 */
public class NaturalFading {
//    public static final Integer[] STEPS_100 = new Integer[] {0,1,1,1,1,1,2,2,2,2,2,2,3,3,3,3,4,4,4,5,5,5,6,6,7,8,8,9,10,
//            11,11,12,14,15,16,17,19,21,22,24,26,29,31,34,37,40,44,48,52,56,61,67,73,79,86,93,102,110,120,131,142,155,
//            168,183,199,216,235,256,278,303,329,358,390,424,461,501,545,593,645,702,763,830,903,982,1068,1162,1263,1374,
//            1495,1625,1768,1923,2091,2275,2474,2691,2927,3183,3462,3766,4096};

    public static final Integer[] STEPS_100 = new Integer[] {0,1,1,1,1,1,1,2,2,2,2,2,2,3,3,3,3,3,4,4,4,5,5,5,6,6,7,7,8,
            9,9,10,11,12,13,14,15,16,17,18,20,22,23,25,27,29,32,34,37,40,43,46,50,54,59,63,68,74,80,86,93,100,108,117,
            126,136,147,159,171,185,200,216,233,252,272,293,317,342,369,399,431,465,502,542,586,632,683,737,796,860,928,
            1002,1082,1168,1262,1362,1471,1589,1715,1852,1999};
}
