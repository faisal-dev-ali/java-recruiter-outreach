//package com.faisal.outreach;
//
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//import java.util.Scanner;
//
//@Component
//public class OutreachRunner implements CommandLineRunner {
//
//    private static final String LIVE_CONFIRMATION = "SEND";
//
//    private final OutreachService outreachService;
//    private final OutreachConfig config;
//
//    public OutreachRunner(
//            OutreachService outreachService,
//            OutreachConfig config) {
//
//        this.outreachService = outreachService;
//        this.config = config;
//    }
//
//    @Override
//    public void run(String... args) {
//
//        printHeader();
//
//        try (Scanner scanner = new Scanner(System.in)) {
//
//            if (!confirmLiveMode(scanner)) {
//                return;
//            }
//
//            processRecruiters(scanner);
//
//        } catch (Exception e) {
//
//            System.err.println(
//                    "Application error: " + e.getMessage()
//            );
//
//        } finally {
//
//            System.out.println();
//            System.out.println(
//                    "Finished. Check the outreach log for results."
//            );
//        }
//    }
//
//    private void printHeader() {
//
//        System.out.println();
//        System.out.println("=========================================");
//        System.out.println("     Java Recruiter Outreach Utility");
//        System.out.println("=========================================");
//
//        System.out.println(
//                "Mode: "
//                        + (config.isDryRun()
//                        ? "PREVIEW ONLY (no email will be sent)"
//                        : "LIVE EMAIL SENDING")
//        );
//
//        System.out.println(
//                "Resume: " + config.getResumePath()
//        );
//
//        System.out.println(
//                "Log: " + config.getLogPath()
//        );
//
//        System.out.println();
//    }
//
//    private boolean confirmLiveMode(Scanner scanner) {
//
//        if (config.isDryRun()) {
//            return true;
//        }
//
//        System.out.println(
//                "WARNING: LIVE MODE is enabled."
//        );
//
//        System.out.println(
//                "Emails will be sent to the addresses you enter."
//        );
//
//        System.out.print(
//                "Type SEND to continue: "
//        );
//
//        String confirmation =
//                scanner.nextLine().trim();
//
//        if (!LIVE_CONFIRMATION.equals(confirmation)) {
//
//            System.out.println(
//                    "Cancelled. No emails were sent."
//            );
//
//            return false;
//        }
//
//        System.out.println(
//                "Live mode confirmed."
//        );
//
//        System.out.println();
//
//        return true;
//    }
//
//    private void processRecruiters(Scanner scanner) {
//
//        while (true) {
//
//            RecruiterInput input =
//                    readRecruiter(scanner);
//
//            OutreachResult result =
//                    outreachService.process(
//                            input.recruiterName(),
//                            input.recruiterEmail(),
//                            input.companyName()
//                    );
//
//            printResult(result);
//
//            if (!shouldContinue(scanner)) {
//                break;
//            }
//
//            System.out.println();
//        }
//    }
//
//    private RecruiterInput readRecruiter(
//            Scanner scanner) {
//
//        System.out.println(
//                "---------- Recruiter Details ----------"
//        );
//
//        String name =
//                readRequired(
//                        scanner,
//                        "Recruiter name: "
//                );
//
//        String email =
//                readRequired(
//                        scanner,
//                        "Recruiter email: "
//                );
//
//        String company =
//                readRequired(
//                        scanner,
//                        "Company name: "
//                );
//
//        System.out.println();
//
//        return new RecruiterInput(
//                name,
//                email,
//                company
//        );
//    }
//
//    private boolean shouldContinue(
//            Scanner scanner) {
//
//        while (true) {
//
//            System.out.print(
//                    "Process another recruiter? (y/n): "
//            );
//
//            String input =
//                    scanner.nextLine().trim();
//
//            if ("y".equalsIgnoreCase(input)) {
//                return true;
//            }
//
//            if ("n".equalsIgnoreCase(input)) {
//                return false;
//            }
//
//            System.out.println(
//                    "Please enter 'y' or 'n'."
//            );
//        }
//    }
//
//    private String readRequired(
//            Scanner scanner,
//            String prompt) {
//
//        while (true) {
//
//            System.out.print(prompt);
//
//            String value =
//                    scanner.nextLine().trim();
//
//            if (!value.isBlank()) {
//                return value;
//            }
//
//            System.out.println(
//                    "This field is required. Please try again."
//            );
//        }
//    }
//
//    private void printResult(
//            OutreachResult result) {
//
//        System.out.println();
//        System.out.println("Result");
//        System.out.println("-----------------------------------------");
//        System.out.println(
//                "Recruiter : " + result.recruiterName()
//        );
//        System.out.println(
//                "Company   : " + result.companyName()
//        );
//        System.out.println(
//                "Email     : " + result.recruiterEmail()
//        );
//        System.out.println(
//                "Status    : " + result.status()
//        );
//        System.out.println(
//                "Details   : " + result.details()
//        );
//        System.out.println("-----------------------------------------");
//    }
//
//    private record RecruiterInput(
//            String recruiterName,
//            String recruiterEmail,
//            String companyName
//    ) {
//    }
//}