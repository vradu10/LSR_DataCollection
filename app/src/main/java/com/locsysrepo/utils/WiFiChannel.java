package com.locsysrepo.utils;

public class WiFiChannel {

    public static int getChannel(int freq) {

        if (freq == 2412) return 1;
        if (freq == 2437) return 6;
        if (freq == 2462) return 11;

        if (freq == 2417) return 2;
        if (freq == 2422) return 3;
        if (freq == 2427) return 4;
        if (freq == 2432) return 5;

        if (freq == 2442) return 7;
        if (freq == 2447) return 8;
        if (freq == 2452) return 9;
        if (freq == 2457) return 10;

        if (freq == 2467) return 12;
        if (freq == 2472) return 13;
        if (freq == 2484) return 14;
        if (freq == 2484) return 14;

        // .a

        if (freq == 4915) return 183;
        if (freq == 4920) return 184;
        if (freq == 4925) return 185;
        if (freq == 4935) return 187;
        if (freq == 4940) return 188;
        if (freq == 4945) return 189;
        if (freq == 4960) return 192;
        if (freq == 4980) return 196;
        if (freq == 5035) return -7;
        if (freq == 5040) return -8;
        if (freq == 5045) return -9;
        if (freq == 5055) return -11;
        if (freq == 5060) return -12;
        if (freq == 5080) return 16;
        if (freq == 5170) return 34;
        if (freq == 5180) return 36;
        if (freq == 5190) return 38;
        if (freq == 5200) return 40;
        if (freq == 5210) return 42;
        if (freq == 5220) return 44;
        if (freq == 5230) return 46;
        if (freq == 5240) return 48;
        if (freq == 5260) return 52;
        if (freq == 5280) return 56;
        if (freq == 5300) return 60;
        if (freq == 5320) return 64;
        if (freq == 5500) return 100;
        if (freq == 5520) return 104;
        if (freq == 5540) return 108;
        if (freq == 5560) return 112;
        if (freq == 5580) return 116;
        if (freq == 5600) return 120;
        if (freq == 5620) return 124;
        if (freq == 5640) return 128;
        if (freq == 5660) return 132;
        if (freq == 5680) return 136;
        if (freq == 5700) return 140;
        if (freq == 5745) return 149;
        if (freq == 5765) return 153;
        if (freq == 5785) return 157;
        if (freq == 5805) return 161;
        if (freq == 5825) return 165;
        return -99;
    }
}