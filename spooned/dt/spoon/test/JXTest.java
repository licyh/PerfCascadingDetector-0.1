

package dt.spoon.test;


public class JXTest {
    public static void entrance() throws java.lang.Exception {
        long start_time = java.lang.System.nanoTime();
        int loop0 = 0;LogClass._DM_Log.log_LoopBegin("dt.spoon.test.JXTest.entrancevoid entrance()_loop0");
        for (int i = 0; i < 2; i++) {
            loop0++;LogClass._DM_Log.log_LoopCenter("dt.spoon.test.JXTest.entrancevoid entrance()_loop0");
            int loop1 = 0;LogClass._DM_Log.log_LoopBegin("dt.spoon.test.JXTest.entrancevoid entrance()_loop1");
            for (int j = 1; j < 5; j++) {
                loop1++;LogClass._DM_Log.log_LoopCenter("dt.spoon.test.JXTest.entrancevoid entrance()_loop1");
                int loop2 = 0;LogClass._DM_Log.log_LoopBegin("dt.spoon.test.JXTest.entrancevoid entrance()_loop2");
                for (int f = -1; f < 10; f++) {
                    loop2++;LogClass._DM_Log.log_LoopCenter("dt.spoon.test.JXTest.entrancevoid entrance()_loop2");
                    if (start_time > 1000) {
                        LogClass._DM_Log.log_LoopEnd("dt.spoon.test.JXTest.entrancevoid entrance()_loop0_"+loop0);
                        LogClass._DM_Log.log_LoopEnd("dt.spoon.test.JXTest.entrancevoid entrance()_loop1_"+loop1);
                        LogClass._DM_Log.log_LoopEnd("dt.spoon.test.JXTest.entrancevoid entrance()_loop2_"+loop2);
                        return ;
                    }
                    java.lang.String t = "xx";
                    try {
                    } catch (java.lang.Exception e) {
                        LogClass._DM_Log.log_LoopEnd("dt.spoon.test.JXTest.entrancevoid entrance()_loop0_"+loop0);
                        LogClass._DM_Log.log_LoopEnd("dt.spoon.test.JXTest.entrancevoid entrance()_loop1_"+loop1);
                        LogClass._DM_Log.log_LoopEnd("dt.spoon.test.JXTest.entrancevoid entrance()_loop2_"+loop2);
                        throw e;
                    }
                }
                LogClass._DM_Log.log_LoopEnd("dt.spoon.test.JXTest.entrancevoid entrance()_loop2_"+loop2);
            }
            LogClass._DM_Log.log_LoopEnd("dt.spoon.test.JXTest.entrancevoid entrance()_loop1_"+loop1);
        }
        LogClass._DM_Log.log_LoopEnd("dt.spoon.test.JXTest.entrancevoid entrance()_loop0_"+loop0);
        int loop3 = 0;LogClass._DM_Log.log_LoopBegin("dt.spoon.test.JXTest.entrancevoid entrance()_loop3");
        for (int w = 1; w < 10000000; w++) {
            loop3++;LogClass._DM_Log.log_LoopCenter("dt.spoon.test.JXTest.entrancevoid entrance()_loop3");
            int k = 1;
            k = 2;
        }
        LogClass._DM_Log.log_LoopEnd("dt.spoon.test.JXTest.entrancevoid entrance()_loop3_"+loop3);
        try {
            java.lang.System.out.println((("JX - Completion Time: " + ((((double) ((java.lang.System.nanoTime()) - start_time)) / 1000) / 1000)) + "ms"));
        } catch (java.lang.Exception e) {
            throw new java.lang.Exception(e);
        }
    }

    public static void main(java.lang.String[] args) throws java.lang.Exception {
        dt.spoon.test.JXTest.entrance();
        int x = 0;
        try {
        } catch (java.lang.Exception e) {
            int count = 5;
            int loop0 = 0;LogClass._DM_Log.log_LoopBegin("dt.spoon.test.JXTest.mainvoid main(java.lang.String[])_loop0");
            do {
                loop0++;LogClass._DM_Log.log_LoopCenter("dt.spoon.test.JXTest.mainvoid main(java.lang.String[])_loop0");
                int loop1 = 0;LogClass._DM_Log.log_LoopBegin("dt.spoon.test.JXTest.mainvoid main(java.lang.String[])_loop1");
                while (count < 10) {
                    loop1++;LogClass._DM_Log.log_LoopCenter("dt.spoon.test.JXTest.mainvoid main(java.lang.String[])_loop1");
                } 
                LogClass._DM_Log.log_LoopEnd("dt.spoon.test.JXTest.mainvoid main(java.lang.String[])_loop1_"+loop1);
                int loop2 = 0;LogClass._DM_Log.log_LoopBegin("dt.spoon.test.JXTest.mainvoid main(java.lang.String[])_loop2");
                for (int ff = 1; ff < 2; ff++) {
                    loop2++;LogClass._DM_Log.log_LoopCenter("dt.spoon.test.JXTest.mainvoid main(java.lang.String[])_loop2");
                }
                LogClass._DM_Log.log_LoopEnd("dt.spoon.test.JXTest.mainvoid main(java.lang.String[])_loop2_"+loop2);
            } while (false );
            LogClass._DM_Log.log_LoopEnd("dt.spoon.test.JXTest.mainvoid main(java.lang.String[])_loop0_"+loop0);
        }
    }

    class InnerCl {
        InnerCl() {
            int a = 0;
        }
    }
}

