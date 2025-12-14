package selahattin.dev.ecom.dev;

import org.springframework.boot.CommandLineRunner;

import java.util.Random;

public class CyberPsychosisBean implements CommandLineRunner {

    // Renk Paleti (ANSI Kaçış Kodları)
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String PURPLE = "\u001B[35m"; // .NET Hatası için
    private static final String CYAN = "\u001B[36m";
    private static final String WHITE_BOLD = "\033[1;37m";
    private static final String RED_BACKGROUND = "\u001B[41m";

    @Override
    public void run(String... args) throws Exception {
        Random random = new Random();

        System.out.println(GREEN + ">>> SYSTEM INTEGRITY CHECK..." + RESET);
        Thread.sleep(1000);
        System.out.println(GREEN + ">>> CPU: OK" + RESET);
        System.out.println(GREEN + ">>> RAM: OK" + RESET);
        System.out.println(YELLOW + ">>> WARNING: UNKNOWN FLUX DETECTED..." + RESET);
        Thread.sleep(1500);

        // KAOS DÖNGÜSÜ
        for (int i = 0; i < 200; i++) {
            int scenario = random.nextInt(6); // 6 farklı felaket senaryosu

            switch (scenario) {
                case 0: // CRITICAL KERNEL (Kırmızı)
                    System.err.println(RED + "[KERNEL_PANIC] PID: " + random.nextInt(9999) +
                            " | CPU#0 STUCK FOR " + random.nextInt(50) + "s! | VFS: Unable to mount root fs" + RESET);
                    break;

                case 1: // MEMORY LEAK (Sarı - Uyarı)
                    System.out.println(YELLOW + "[MEM_LEAK_DETECTOR] Heap fragmentation at 98%. " +
                            "Garbage Collector is DEAD. Allocation failed at 0x"
                            + Integer.toHexString(random.nextInt()).toUpperCase() + RESET);
                    break;

                case 2: // DATA CORRUPTION (Cyan - Sistem Mesajı)
                    System.out.println(CYAN + "[FS_CORRUPTION] Inode table mismatch. " +
                            "Writing random bytes to /dev/sda1... (Sector " + random.nextInt(1000000) + " lost)"
                            + RESET);
                    break;

                case 3: // NETWORK SECURITY (Mavi)
                    System.out.println(
                            BLUE + "[NET_SEC] UNCLOAKED INTRUSION DETECTED from IP 192.168.1." + random.nextInt(255) +
                                    " | FIREWALL BYPASSED via Port 8080" + RESET);
                    break;

                case 4: // .NET HATASI (Mor - Absürd)
                    System.out.println(PURPLE
                            + "Unhandled Exception: System.NullReferenceException: Object reference not set to an instance of an object.\n"
                            +
                            "   at Microsoft.NET.Framework.Core.Clr.Panic() in C:\\Windows\\Microsoft.NET\\Framework64\\v4.0.30319\\clr.dll\n"
                            +
                            "   >>> WAIT, WHY IS CLR RUNNING IN JVM?! <<<" + RESET);
                    Thread.sleep(300); // Okumaları için biraz beklet
                    break;

                case 5: // TOTAL CRASH (Beyaz Bold üzerine Kırmızı Arka Plan)
                    if (random.nextInt(10) > 8) { // Nadiren çıksın
                        System.out.println(RED_BACKGROUND + WHITE_BOLD +
                                " !!! FATAL EXCEPTION !!! SYSTEM HALTED !!! " + RESET);
                    }
                    break;
            }

            // Hızlanıp yavaşlayan "Matrix" akışı efekti
            Thread.sleep(random.nextInt(50) + 10);
        }

        System.out.println(RED + "\n\n>>> SYSTEM SHUTDOWN INITIATED DUE TO CRITICAL FAILURE <<<" + RESET);
        System.out.println(RED + ">>> GOODBYE CRUEL WORLD." + RESET);
    }
}