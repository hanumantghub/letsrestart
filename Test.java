import java.util.Scanner;

public class Test {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("Enter 5 numbers:");
        
        int smallest = Integer.MAX_VALUE;
        int biggest = Integer.MIN_VALUE;
        
        for (int i = 0; i < 5; i++) {
            System.out.print("Enter number " + (i + 1) + ": ");
            int number = scanner.nextInt();
            
            if (number < smallest) {
                smallest = number;
            }
            if (number > biggest) {
                biggest = number;
            }
        }
        
        scanner.close();
        
        System.out.println("\nSmallest number: " + smallest);
        System.out.println("Biggest number: " + biggest);
    }
}