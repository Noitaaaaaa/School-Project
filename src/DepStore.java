import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

// This runs the whole department store application or system and is basically the starting point
public class DepStore {
    private static final String CURRENCY = "P";
    private static final String[] CLOTHING_SIZES = {"XS", "S", "M", "L", "XL"};
    private static final String[] SOCK_SIZES = {"S", "M", "L", "XL", "XXL"};
    private static final String[] SHOE_SIZES = {"6", "7", "8", "9", "10"};
    private static final String[] JEAN_SIZES = {"28", "30", "32", "34", "36"};
    private static final String[] ZARA_JEAN_SIZES = {"26", "28", "30", "32", "34"};
    private static final String[] EURO_SHOE_SIZES = {"36", "37", "38", "39", "40"};
    private static final String[] SPF_CHOICES = {"30", "50", "60", "75", "100"};
    private static final String[] WEIGHT_CHOICES = {"2kg", "5kg", "10kg", "15kg", "20kg"};
    private static final String[] TUMBLER_SIZES = {"16 oz", "20 oz", "24 oz", "32 oz", "40 oz"};

    public static void main(String[] args) {    
        Scanner sc = new Scanner(System.in);
        Store store = new Store();
        List<CartItem> cart = new ArrayList<>();
        List<Receipt> receiptHistory = new ArrayList<>();

        System.out.println("================================");
        System.out.println(" Welcome to the Augustore!");
        System.out.println("================================");
        System.out.println("Choose by number or by words, like Clothing, H&M, shirts, medium, cart, checkout, card, or cash.");

        // When running is true, the whole store app keeps accepting another shopping session.
        boolean running = true;
        while (running) {
            // When shopping is true dyan magsisimula nag shopping loop and hindi matatapos until maging false yung shopping
            // Shopping will be false once na nag checkout ang user
            boolean shopping = true;
            while (shopping) {
            MenuChoice choice = chooseMenuOption(sc, store.getCategories());

            // Once na napili ang Cart mago-open yung cart para makita ng user ang laman ng cart and pwede i manage
            if (choice.isCart()) {    
                running = openCart(sc, cart, receiptHistory);
                if (!running) {
                    shopping = false;
                }
                continue;
            }

            // Once na napili ang Checkout mag a-ask muna sya if yes/no just incase na wrong input
            if (choice.isCheckout()) { 
                if (cart.isEmpty()) {
                    System.out.println("\nThere is nothing in your cart yet.");
                    if (askYesNo(sc, "Are you done shopping? (yes/no): ")) {
                        running = false;
                        shopping = false;
                    }
                    continue;
                }
                shopping = !askYesNo(sc, "Are you sure you want to checkout? (yes/no): ");
                continue;
            }
             // Once na nakapili ng category pupunta na sa store menu para pumili ang user
            Category category = store.getCategories().get(choice.getIndex());
            boolean choosingStore = true;
            // When choosingStore is true magsisimula na ang store menu loop
            while (choosingStore) {     
                StoreBrand storeBrand = chooseStoreBrand(sc, category);
                if (storeBrand == null) {
                    choosingStore = false;
                    continue;
                }
                
                boolean choosingFromStore = true;
                // Once na naging true ang choosingFromStore dito na magsisimula ang loop ng pagpili ng product
                while (choosingFromStore) { 
                    Product product = chooseProduct(sc, category, storeBrand);
                    if (product == null) {
                        choosingFromStore = false;
                        continue;
                    }

                    if (!product.hasOptions() && product.getStock() == 0) {
                        printStockMessage(product); // If product is out of stock mag pri-print sya na wala ng stock 
                        continue;
                    }

                    ProductOption option = null;
                    // If may stock naman dito magsisimula yung chooseProductOption para pumili yung user ng options 
                     // Options na katulad ng mga sizes
                    if (product.hasOptions()) {                         
                        option = chooseProductOption(sc, product); 
                        if (option == null) {
                            continue;
                        }
                    }

                    Integer quantity = readQuantity(sc, "\nHow many " + product.getDisplayName(option) + " will you buy? (or Back): ");
                    if (quantity == null) {
                        continue;
                    }

                    // After addToCart mag pri-print ng add to cart and babalik sa Category Menu
                    if (addToCart(cart, product, option, quantity)) { 
                        System.out.println("\n" + quantity + " x " + product.getDisplayName(option) + " added to your cart.");
                        choosingFromStore = false;
                        choosingStore = false;
                    }
                }
            }
        }
            if (!running) {
                continue;
            }

            // Once na nag accept na sa Checkout mag tatanong yung program if Cash or Card
            Payment payment = collectPayment(sc, cart);
            if (payment.isCompleted()) {
                receiptHistory.add(new Receipt(receiptHistory.size() + 1, createReceiptItems(cart), payment));
                running = handlePostPaymentOptions(sc, receiptHistory);
                cart.clear();
            }
        }
 
        sc.close();
    }

    // =========================
    // Menu Methods
    // =========================

    private static MenuChoice chooseMenuOption(Scanner sc, List<Category> categories) {
        // This list stores all valid choices para matanggap ang number or word input ng user
        List<ChoiceOption> options = new ArrayList<>();

        System.out.println("\n========== Categories ==========");
            // A for loop is used to print each category with its number and to add it as a valid choice with its name and aliases
        for (int i = 0; i < categories.size(); i++) {
            Category category = categories.get(i);
            System.out.printf("%2d. %s%n", i + 1, category.getName());
            // Every category is added as a choice para pwede piliin by number or by name or by alias
            options.add(new ChoiceOption(i + 1, category.getName(), category.getAliases()));
        }

        System.out.println("\n========== Store Options ==========");
        // Cart and Checkout options are added after the categories, so their numbers depend on how many categories there are. 
        // They also have word choices like "cart" or "checkout" for user input.
        System.out.printf("%2d. Cart%n", categories.size() + 1);
        System.out.printf("%2d. Checkout%n", categories.size() + 2);
        // Adding Cart and Checkout as valid choices with their respective numbers and word aliases.
        options.add(new ChoiceOption(categories.size() + 1, "Cart", "cart", "carts", "basket", "baskets"));
        options.add(new ChoiceOption(categories.size() + 2, "Checkout", "checkout", "checkouts", "pay", "payment", "payments"));

        // Dito binabasa yung final menu choice ng user
        // User can choose a category by number or name, or choose cart or checkout by number or name
        ChoiceOption choice = readChoice(sc, "\nChoose a category, cart, or checkout: ", options); 

        // If the user chose categories.size() + 1 the cart will open the cart menu,
        if (choice.getNumber() == categories.size() + 1) {
            return MenuChoice.cart();
        }

        // If the user chose categories.size() + 2 the checkout will open the checkout flow,
        if (choice.getNumber() == categories.size() + 2) {
            return MenuChoice.checkout();
        }

        // If the user chose a category number, the corresponding index is returned to show the stores under that category
        return MenuChoice.category(choice.getNumber() - 1);
    }

    private static StoreBrand chooseStoreBrand(Scanner sc, Category category) {
        // Dito makikita yung stores/brands under the selected category, like H&M and Zara under Clothing
        List<StoreBrand> storeBrands = category.getStoreBrands();
        // Options dito ay stores/brands under the selected category
        List<ChoiceOption> options = new ArrayList<>();

        System.out.println("\n========== " + category.getName() + " Stores ==========");
        // Same as categories it will print out all the store brands
        for (int i = 0; i < storeBrands.size(); i++) {
            StoreBrand storeBrand = storeBrands.get(i);
            System.out.printf("%2d. %s%n", i + 1, storeBrand.getName());
            // Store aliases allow inputs like "hm" or "h and m" kahit iba yung displayed name
            options.add(new ChoiceOption(i + 1, storeBrand.getName(), storeBrand.getAliases()));
        }

        // A back to category choice to go back to the chooseMenuOption part
        System.out.printf("%2d. Back to categories%n", storeBrands.size() + 1);
        options.add(new ChoiceOption(storeBrands.size() + 1, "Back", "b", "back", "backs", "category", "categories", "return"));

        ChoiceOption choice = readChoice(sc, "\nChoose a store or brand: ", options);
        if (choice.getNumber() == storeBrands.size() + 1) {
            // Null means back, so the main loop knows babalik sa category menu
            return null;
        }

        return storeBrands.get(choice.getNumber() - 1);
    }

    private static Product chooseProduct(Scanner sc, Category category, StoreBrand storeBrand) {
        List<Product> products = storeBrand.getProducts();
        List<ChoiceOption> options = new ArrayList<>();
        // If at least one product has sizes/options, the table shows an extra Available Choices column
        boolean showAvailableChoices = hasProductsWithOptions(products);

        System.out.println("\n========== " + category.getName() + " > " + storeBrand.getName() + " Products ==========");
        if (showAvailableChoices) {
            System.out.printf("%-5s %-28s %-14s %-36s %10s %10s%n", "No.", "Product", "Choice", "Available Choices", "Price", "Stock");
            System.out.println("-------------------------------------------------------------------------------------------------------------");
        } else {
            System.out.printf("%-5s %-28s %-14s %10s %10s%n", "No.", "Product", "Choice", "Price", "Stock");
            System.out.println("--------------------------------------------------------------------------");
        }

        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);

            // If showAvailableChoices is true, it means at least one product has options,
            //  so an extra column is printed to show the available options for each product. 
            // Otherwise, the table is simpler without that column.
            // This is mostly seen in the Clothes product list with the size options
            if (showAvailableChoices) {
                System.out.printf(
                        "%-5d %-28s %-14s %-36s %10s %10s%n",
                        i + 1,
                        product.getName(),
                        product.getOptionLabelDisplay(),
                        product.getShortOptionsDisplay(),
                        formatPrice(product.getPrice()),
                        product.getStockDisplay());

            // If showAvailableChoices is false, it means none of the products have options, 
            // so the table doesn't include the available choices column and just shows the basic product info.
            } else {
                System.out.printf(
                        "%-5d %-28s %-14s %10s %10s%n",
                        i + 1,
                        product.getName(),
                        product.getOptionLabelDisplay(),
                        formatPrice(product.getPrice()),
                        product.getStockDisplay());
            }

            // Product is added to choices para pwede siya piliin by number, product name, or alias
            options.add(new ChoiceOption(i + 1, product.getName(), product.getAliases()));
        }

        // if showAvailableChoices is true, the separator line is longer to match the extra column, otherwise it's shorter
        if (showAvailableChoices) {
            System.out.println("-------------------------------------------------------------------------------------------------------------");
        } else {
            System.out.println("--------------------------------------------------------------------------");
        }
        // back to store choice
        System.out.printf("%-5d %s%n", products.size() + 1, "Back to stores");
        options.add(new ChoiceOption(products.size() + 1, "Back", "b", "back", "backs", "stores", "store", "brands", "brand", "return"));

        // Dito binabasa yung final product choice ng user, pwede by number, name, or alias
        ChoiceOption choice = readChoice(sc, "\nChoose a product: ", options);
        if (choice.getNumber() == products.size() + 1) {
            // Null means back, so the store loop will show the stores again
            return null;
        }

        return products.get(choice.getNumber() - 1);
    }

    private static boolean hasProductsWithOptions(List<Product> products) {
        // Checks kung kailangan ipakita yung choices column like Size, SPF, or Weight
        for (Product product : products) {
            if (product.hasOptions()) {
                return true;
            }
        }

        return false;
    }

    private static ProductOption chooseProductOption(Scanner sc, Product product) {
        List<ProductOption> options = product.getOptions();
        // Separate ChoiceOption list para yung choices like S/M/L or 30/50 SPF can be typed by words
        List<ChoiceOption> choiceOptions = new ArrayList<>();

        // This part is similar to the previous menus but focused on the product options like sizes or SPF choices
        System.out.println("\n========== " + product.getDisplayName() + " " + product.getOptionLabel() + " ==========");
        System.out.printf("%-5s %-12s %10s%n", "No.", product.getOptionLabel(), "Stock");
        System.out.println("-------------------------------");
        for (int i = 0; i < options.size(); i++) {
            ProductOption option = options.get(i);
            System.out.printf("%-5d %-12s %10s%n", i + 1, option.getName(), formatStock(option.getStock()));
            choiceOptions.add(new ChoiceOption(i + 1, option.getName(), option.getAliases()));
        }
        System.out.println("-------------------------------");

        // back option
        System.out.printf("%2d. Back to products%n", options.size() + 1);
        choiceOptions.add(new ChoiceOption(options.size() + 1, "Back", "b", "back", "backs", "product", "products", "return"));

        ChoiceOption choice = readChoice(sc, "\nChoose " + product.getOptionLabel().toLowerCase() + ": ", choiceOptions);
        if (choice.getNumber() == options.size() + 1) {
            // Null means the user chose Back to products
            return null;
        }

        return options.get(choice.getNumber() - 1);
    }

    // =========================
    // Input Methods
    // =========================

    private static ChoiceOption readChoice(Scanner sc, String prompt, List<ChoiceOption> options) {
        // This repeats until the user enters a valid number or valid word choice
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            String normalizedInput = normalizeChoice(input);

            // First checks word inputs like "cart", "checkout", "shirt", or aliases
            for (ChoiceOption option : options) {
                if (option.matches(normalizedInput)) {
                    return option;
                }
            }

            // If the word did not match, try reading it as a number like 1 or "one"
            Integer number = parseNumber(input);
            if (number != null) {
                for (ChoiceOption option : options) {
                    if (option.getNumber() == number) {
                        return option;
                    }
                }
            }

            System.out.println("Please enter a listed number or word choice.");
        }
    }

    private static int readInt(Scanner sc, String prompt, int min, int max) {
        // Used for numbers with allowed range, like changing cart quantity from -100 to 100
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            Integer number = parseNumber(input);

            if (number != null && number >= min && number <= max) {
                return number;
            }

            System.out.println("Please enter a number from " + min + " to " + max + ".");
        }
    }

    private static Integer readIntOrBack(Scanner sc, String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            if (isBackInput(input)) {
                return null;
            }
            Integer number = parseNumber(input);

            if (number != null && number >= min && number <= max) {
                return number;
            }

            System.out.println("Please enter a number from " + min + " to " + max + ", or type Back.");
        }
    }

    private static Integer readQuantity(Scanner sc, String prompt) {
        // Used for buying/payment amounts, kaya dapat positive number lang so it wont accept anything negative
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            if (isBackInput(input)) {
                return null;
            }
            Integer number = parseNumber(input);

            if (number != null && number > 0) {
                return number;
            }

            System.out.println("Please enter at least 1, or type Back.");
        }
    }

    private static Integer readPaymentAmount(Scanner sc, String prompt) {
        // Payment accepts plain numbers and common currency formats like P1000, PHP 1000, and 1,000.
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            if (isBackInput(input)) {
                return null;
            }
            Integer amount = parseMoneyAmount(input);

            if (amount != null && amount > 0) {
                return amount;
            }

            System.out.println("Please enter a valid payment amount, or type Back.");
        }
    }

    private static boolean isBackInput(String input) {
        String text = normalizeChoice(input);
        return text.equals("b") || text.equals("back") || text.equals("go back") || text.equals("cancel") || text.equals("return");
    }

    private static Integer parseMoneyAmount(String input) {
        if (input == null) {
            return null;
        }

        String text = input.toLowerCase()
                .replace(",", "")
                .replace("₱", "p")
                .trim();

        text = text.replaceAll("^(php|pesos?|p)\\s*", "");
        text = text.replaceAll("\\s*(php|pesos?|p)$", "");

        if (text.matches("\\d+\\.0{1,2}")) {
            text = text.substring(0, text.indexOf('.'));
        } else if (text.matches("\\d+\\.\\d+")) {
            return null;
        }

        return parseNumber(text);
    }

    private static Integer parseNumber(String input) {
        if (input == null) {
            return null;
        }

        // Cleans the input so "twenty-one" and "1,000" can still be converted
        String text = input.toLowerCase().replace(",", "").trim();
        if (text.isEmpty()) {
            return null;
        }

        // Allows negative inputs like "-2", "minus two", or "negative two"
        boolean negative = false;
        if (text.startsWith("negative ")) {
            negative = true;
            text = text.substring(9).trim();
        } else if (text.startsWith("minus ")) {
            negative = true;
            text = text.substring(6).trim();
        } else if (text.startsWith("-")) {
            negative = true;
            text = text.substring(1).trim();
        }

        Integer value = parsePositiveNumber(text); // Converts the remaining text into a positive number
        if (value == null) {
            return null;
        }

        return negative ? -value : value;
    }

    private static Integer parsePositiveNumber(String text) {
        if (text.matches("\\d+")) {
            // If digits na siya, diretso convert to integer
            return Integer.parseInt(text);
        }

        // If words yung input, each word is converted and added together
        String[] words = text.split("\\s+");
        int total = 0;
        int current = 0;
        boolean foundNumber = false;

        for (String word : words) {
            int value;

            if (word.matches("\\d+")) {
                value = Integer.parseInt(word);
            } else if (word.equals("a")) {
                value = 1;
            } else if (word.equals("hundred")) {
                // Handles inputs like "one hundred" or "two hundred"
                current = current == 0 ? 100 : current * 100;
                foundNumber = true;
                continue;
            } else {
                value = singleWordNumber(word);
                if (value == -1) {
                    return null;
                }
            }

            current += value;
            foundNumber = true;
        }

        total += current;
        return foundNumber ? total : null;
    }

    private static int singleWordNumber(String word) {
        // Converts single number words to their integer value
        switch (word) {
            case "zero":
                return 0;
            case "one":
                return 1;
            case "two":
                return 2;
            case "three":
                return 3;
            case "four":
                return 4;
            case "five":
                return 5;
            case "six":
                return 6;
            case "seven":
                return 7;
            case "eight":
                return 8;
            case "nine":
                return 9;
            case "ten":
                return 10;
            case "eleven":
                return 11;
            case "twelve":
                return 12;
            case "thirteen":
                return 13;
            case "fourteen":
                return 14;
            case "fifteen":
                return 15;
            case "sixteen":
                return 16;
            case "seventeen":
                return 17;
            case "eighteen":
                return 18;
            case "nineteen":
                return 19;
            case "twenty":
                return 20;
            case "thirty":
                return 30;
            case "forty":
                return 40;
            case "fifty":
                return 50;
            case "sixty":
                return 60;
            case "seventy":
                return 70;
            case "eighty":
                return 80;
            case "ninety":
                return 90;
            case "extra small":
                return -1;
            default:
                return -1;
        }
    }

    // This method keeps asking a yes or no question until the user answers with a valid yes/y or no/n response
    private static boolean askYesNo(Scanner sc, String prompt) {
        // Keeps asking until yes/y or no/n ang sagot ng user
        while (true) {
            System.out.print(prompt);
            String answer = sc.nextLine().trim().toLowerCase();

            if (answer.equals("yes") || answer.equals("y")) {
                return true;
            }

            if (answer.equals("no") || answer.equals("n")) {
                return false;
            }

            System.out.println("Please answer yes or no.");
        }
    }

    // =========================
    // Cart Methods
    // =========================

    // This method tries to add the specified quantity of the product with the chosen option to the cart.
    private static boolean addToCart(List<CartItem> cart, Product product, ProductOption option, int quantity) {
        // Bawas muna sa stock para hindi ma-add sa cart kapag kulang ang available items
        if (!product.removeStock(option, quantity)) {
            printStockMessage(product, option);
            return false;
        }

        // If same product and same option already exists, quantity na lang ang dadagdagan
        for (CartItem item : cart) {
            if (item.matches(product, option)) {
                item.addQuantity(quantity);
                return true;
            }
        }

        // If new item siya, gagawa ng bagong CartItem sa cart
        cart.add(new CartItem(product, option, quantity));
        return true;
    }

    // This method opens the cart menu where the user can see their cart items and choose to add/remove quantity, 
    // remove items, or go back to categories
    private static boolean openCart(Scanner sc, List<CartItem> cart, List<Receipt> receiptHistory) {
        boolean viewingCart = true;

        // This loop keeps the cart open until the user chooses back
        while (viewingCart) {
            printCart(cart);

            if (cart.isEmpty()) {
                // If empty ang cart, back lang ang valid action
                System.out.println("1. Back to categories");
                List<ChoiceOption> emptyOptions = Arrays.asList(
                        new ChoiceOption(1, "Back", "b", "back", "backs", "category", "categories", "return"));
                readChoice(sc, "Choose back to return: ", emptyOptions);
                return true;
            }

            System.out.println("------------------------------------------------------------------------------------------------");
            System.out.println("1. Add or Remove quantity");
            System.out.println("2. Remove item");
            System.out.println("3. Back to categories");
            System.out.println("4. Checkout");

            // This list contains the available options for the cart menu
            List<ChoiceOption> options = Arrays.asList(
                    new ChoiceOption(1, "Add or Remove quantity", "add", "adds", "increase", "remove quantity", "decrease", "quantity", "quantities", "change", "changes", "update", "updates"),
                    new ChoiceOption(2, "Remove item", "remove", "removes", "delete", "deletes", "item", "items"),
                    new ChoiceOption(3, "Back", "b", "back", "backs", "category", "categories", "return"),
                    new ChoiceOption(4, "Checkout", "Done", "Payment", "Pay", "Receipt", "Claim"));
            ChoiceOption choice = readChoice(sc, "Choose an option: ", options);

            if (choice.getNumber() == 1) {
                // Opens quantity update flow for an existing cart item
                addCartQuantity(sc, cart);
            } else if (choice.getNumber() == 2) {
                // Removes one whole item line from the cart
                removeCartItem(sc, cart);
            } else if (choice.getNumber() == 3) {
                viewingCart = false;
            } else {
                Payment payment = collectPayment(sc, cart);
                if (payment.isCompleted()) {
                    receiptHistory.add(new Receipt(receiptHistory.size() + 1, createReceiptItems(cart), payment));
                    boolean keepRunning = handlePostPaymentOptions(sc, receiptHistory);
                    cart.clear();
                    return keepRunning;
                }
            }
        }

        return true;
    }

    // This method allows the user to add or remove quantity for an existing cart item, with stock checks and updates
    private static void addCartQuantity(Scanner sc, List<CartItem> cart) {
        CartItem item = chooseCartItem(sc, cart);
        if (item == null) {
            return;
        }

        Integer quantityInput = readIntOrBack(sc, "How many more will you add/remove? Use a negative number to remove, or Back: ", -100, 100);
        if (quantityInput == null) {
            return;
        }
        int quantity = quantityInput;

        if (quantity < 0 && item.getQuantity() + quantity < 0) {
            // Prevents removing more items than what the cart currently has
            System.out.println("You cannot remove more than " + item.getQuantity() + " " + item.getDisplayName() + "(s).");
            return;
        }

        if (quantity > 0 && !item.getProduct().removeStock(item.getOption(), quantity)) {
            // If user adds more, stock must be checked again first
            printStockMessage(item.getProduct(), item.getOption());
            return;
        }

        if (quantity < 0) {
            // Removed quantity goes back to product stock
            item.getProduct().addStock(item.getOption(), -quantity);
        }

        if (item.getQuantity() + quantity <= 0) {
            // If quantity becomes zero, remove the whole cart item
            cart.remove(item);
            System.out.println(item.getDisplayName() + " removed from your cart.");
            return;
        }

        item.addQuantity(quantity);
        System.out.println("Cart updated for " + item.getDisplayName() + ".");
    }

    // This method allows the user to remove an entire cart item, with stock updates to return 
    // the removed quantity back to the product stock
    private static void removeCartItem(Scanner sc, List<CartItem> cart) {
        CartItem removedItem = chooseCartItem(sc, cart);
        if (removedItem == null) {
            return;
        }
        cart.remove(removedItem);
        // Since item is removed from cart, ibabalik sa stock yung quantity niya
        removedItem.getProduct().addStock(removedItem.getOption(), removedItem.getQuantity());

        System.out.println(removedItem.getDisplayName() + " removed from your cart.");
    }

    // This method allows the user to choose a cart item by number or name for updating quantity or removing, 
    // similar to previous choice methods
    private static CartItem chooseCartItem(Scanner sc, List<CartItem> cart) {
        List<ChoiceOption> options = new ArrayList<>();
        for (int i = 0; i < cart.size(); i++) {
            CartItem item = cart.get(i);
            // User can choose cart item by number or by product name
            options.add(new ChoiceOption(i + 1, item.getDisplayName(), item.getProduct().getAliases()));
        }
        System.out.printf("%2d. Back%n", cart.size() + 1);
        options.add(new ChoiceOption(cart.size() + 1, "Back", "b", "back", "backs", "cart", "return"));

        ChoiceOption choice = readChoice(sc, "Choose item number or name: ", options);
        if (choice.getNumber() == cart.size() + 1) {
            return null;
        }
        return cart.get(choice.getNumber() - 1);
    }

    // This method prints a stock message when the user tries to buy more than the available stock, or if the stock is already zero
    private static void printStockMessage(Product product) {
        printStockMessage(product, null);
    }

    // This method prints a stock message when the user tries to buy more than the available stock, or if the stock is already zero, 
    // with option details if applicable
    private static void printStockMessage(Product product, ProductOption option) {
        if (product.getStock(option) == 0) {
            // Exact zero stock message
            System.out.println("We're out of that item.");
            return;
        }

        // If may stock pero kulang sa requested quantity, ipapakita yung remaining stock
        System.out.println("Sorry, we only have " + product.getStock(option) + " " + product.getDisplayName(option) + "(s) left.");
    }

    // =========================
    // Payment and Display Methods
    // =========================

    // This method handles the checkout flow where the user chooses payment method, enters payment amount if cash, 
    // and returns a Payment object with the details for receipt printing
    private static Payment collectPayment(Scanner sc, List<CartItem> cart) {
        int totalPrice = calculateTotalPrice(cart);

        if (totalPrice == 0) {
            // If empty ang cart during checkout, no payment is needed
            return new Payment("None", 0, 0);
        }
        
        System.out.println("\n==================== Payment Summary ====================");
        printPaymentDetailBox(
                new String[] {"Amount to pay"},
                new String[] {formatPrice(totalPrice)});
        System.out.println("=========================================================");

        // This list contains the available payment method options, which are Card or Cash, and their respective aliases for user input
        List<ChoiceOption> paymentOptions = Arrays.asList(
                new ChoiceOption(1, "Card", "card", "cards", "credit card", "credit cards", "debit card", "debit cards"),
                new ChoiceOption(2, "Cash", "cash", "money"),
                new ChoiceOption(3, "Back to Categories", "b", "Back", "Go Back", "Cancel", "Return"));
        System.out.println("\n+----+----------------+");
        System.out.println("| No | Payment Method |");
        System.out.println("+----+----------------+");
        System.out.println("| 1  | Card           |");
        System.out.println("| 2  | Cash           |");
        System.out.println("| 3  | Back           |");
        System.out.println("+----+----------------+");
        ChoiceOption paymentChoice = readChoice(sc, "\nPayment method (1/Card or 2/Cash or 3/Back): ", paymentOptions);

        if (paymentChoice.getNumber() == 1) {
            if (!askYesNo(sc, "Are you sure you want to pay by card? (yes/no): ")) {
                return new Payment("Back", 0, 0);
            }
            // Card is automatically approved in this simple program
            System.out.println("Card payment approved for " + formatPrice(totalPrice) + ".");
            return new Payment("Card", totalPrice, 0);
        } else if (paymentChoice.getNumber() == 2) {
            // Cash payment requires entering the amount paid, and calculates change
            Integer amountPaid = readPaymentAmount(sc, "Enter cash amount (or Back): ");
            if (amountPaid == null) {
                return new Payment("Back", 0, 0);
            }
            while (amountPaid < totalPrice) {
                System.out.println("Insufficient cash. Please enter an amount of at least " + formatPrice(totalPrice) + ".");
                amountPaid = readPaymentAmount(sc, "Enter cash amount (or Back): ");
                if (amountPaid == null) {
                    return new Payment("Back", 0, 0);
                }
            }
            int change = amountPaid - totalPrice;
            System.out.println("Cash payment accepted. Change: " + formatPrice(change) + ".");
            return new Payment("Cash", amountPaid, change);
        } else {
            // Back to categories option during payment will just return a Payment object with method "Back" so the receipt can show that no payment was made
            return new Payment("Back", 0, 0);
        }
    
        
    }

    private static List<ReceiptItem> createReceiptItems(List<CartItem> cart) {
        List<ReceiptItem> receiptItems = new ArrayList<>();
        for (CartItem item : cart) {
            receiptItems.add(new ReceiptItem(item));
        }
        return receiptItems;
    }

    private static boolean handlePostPaymentOptions(Scanner sc, List<Receipt> receiptHistory) {
        boolean choosingAfterPayment = true;
        boolean receiptPrinted = false;

        while (choosingAfterPayment) {
            System.out.println("\n========== After Payment ==========");
            List<ChoiceOption> options = new ArrayList<>();
            int nextOption = 1;
            int printOption = -1;

            if (!receiptPrinted) {
                printOption = nextOption;
                System.out.println(nextOption + ". Print Receipt");
                options.add(new ChoiceOption(nextOption, "Print Receipt", "print", "receipt", "print receipt"));
                nextOption++;
            }

            int shopAgainOption = nextOption;
            System.out.println(nextOption + ". Shop Again");
            options.add(new ChoiceOption(nextOption, "Shop Again", "shop", "again", "continue", "continue shopping"));
            nextOption++;

            int doneOption = nextOption;
            System.out.println(nextOption + ". Done");
            options.add(new ChoiceOption(nextOption, "Done", "finish", "exit", "quit"));

            ChoiceOption choice = readChoice(sc, "Choose an option: ", options);

            if (choice.getNumber() == printOption) {
                printAllReceipts(receiptHistory);
                receiptPrinted = true;
            } else if (choice.getNumber() == shopAgainOption) {
                System.out.println("\nStarting a new shopping session.");
                return true;
            } else if (choice.getNumber() == doneOption) {
                if (!receiptPrinted) {
                    System.out.println("Thank you for shopping!");
                }
                return false;
            }
        }

        return false;
    }

    // This method prints the cart items in a table format with their details, and also shows the total quantity and 
    // total price at the bottom.
    private static void printCart(List<CartItem> cart) {
        int totalQuantity = 0;
        int totalPrice = 0;

        System.out.println("\n========== Cart ==========");

        if (cart.isEmpty()) {
            System.out.println("Your cart is empty.");
            return;
        }

        System.out.printf("%-5s %-18s %-26s %-18s %5s %12s %12s%n", "No.", "Store", "Product", "Choice", "Qty", "Price", "Subtotal");
        System.out.println("------------------------------------------------------------------------------------------------");

        // Each cart item is printed with its details, and the total quantity and price are updated while printing
        for (int i = 0; i < cart.size(); i++) {
            CartItem item = cart.get(i);
            // These totals are updated while each cart item is printed
            totalQuantity += item.getQuantity();
            totalPrice += item.getSubtotal();

            // Each cart item is printed in a formatted way showing store, product, choice, quantity, price, and subtotal
            System.out.printf(
                    "%-5d %-18s %-26s %-18s %5d %12s %12s%n",
                    i + 1,
                    item.getProduct().getStoreName(),
                    item.getProduct().getName(),
                    item.getChoiceDisplay(),
                    item.getQuantity(),
                    formatPrice(item.getProduct().getPrice()),
                    formatPrice(item.getSubtotal()));
        }

        System.out.println("------------------------------------------------------------------------------------------------");
        System.out.println("Total items: " + totalQuantity);
        System.out.println("Total price: " + formatPrice(totalPrice));
    }

    // This method prints the final receipt with all the cart items and their details, as well as the total quantity, 
    // total price, payment method, amount paid, and change.
    private static void printAllReceipts(List<Receipt> receiptHistory) {
        System.out.println("\n==================== All Receipts ====================");
        for (Receipt receipt : receiptHistory) {
            printReceipt(receipt);
        }
        System.out.println("Thank you for shopping!");
    }

    private static void printReceipt(Receipt receipt) {
        int totalQuantity = 0;
        int totalPrice = 0;

        System.out.println("\n======================== Receipt #" + receipt.number + " =======================");
        System.out.printf("%-18s %-26s %-18s %5s %12s %12s%n", "Store", "Product", "Choice", "Qty", "Price", "Subtotal");
        System.out.println("-------------------------------------------------------------------------------------------");

        for (ReceiptItem item : receipt.items) {
            // Receipt totals are recomputed here para final summary matches the printed items
            totalQuantity += item.quantity;
            totalPrice += item.subtotal;

            System.out.printf(
                    "%-18s %-26s %-18s %5d %12s %12s%n",
                    item.storeName,
                    item.productName,
                    item.choice,
                    item.quantity,
                    formatPrice(item.price),
                    formatPrice(item.subtotal));
        }

        System.out.println("-------------------------------------------------------------------------------------------");
        System.out.println("Total items: " + totalQuantity);
        System.out.println("Total price: " + formatPrice(totalPrice));
        printPaymentDetailBox(
                new String[] {"Payment method", "Amount paid", "Exchange"},
                new String[] {receipt.payment.getMethod(), formatPrice(receipt.payment.getAmountPaid()), formatPrice(receipt.payment.getChange())});
        }


    private static void printPaymentDetailBox(String[] labels, String[] values) {
        int labelWidth = 14;
        int valueWidth = 14;

        for (String label : labels) {
            labelWidth = Math.max(labelWidth, label.length());
        }
        for (String value : values) {
            valueWidth = Math.max(valueWidth, value.length());
        }

        String border = "+" + repeat("-", labelWidth + 2) + "+" + repeat("-", valueWidth + 2) + "+";
        System.out.println(border);
        for (int i = 0; i < labels.length; i++) {
            System.out.printf("| %-" + labelWidth + "s | %" + valueWidth + "s |%n", labels[i], values[i]);
        }
        System.out.println(border);
    }

    private static String repeat(String text, int times) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < times; i++) {
            result.append(text);
        }
        return result.toString();
    }

    // This method calculates the total price of all items in the cart by summing their subtotals, which is used for payment and receipt
    private static int calculateTotalPrice(List<CartItem> cart) {
        int totalPrice = 0;
        for (CartItem item : cart) {
            // Adds every item's subtotal to get the full cart price
            totalPrice += item.getSubtotal();
        }
        return totalPrice;
    }

    // This method formats the price as currency with commas and a currency symbol, like P1,299 instead of just 1299
    private static String formatPrice(int price) {
        // Formats prices like P1,299 instead of plain 1299
        return CURRENCY + String.format("%,d", price);
    }

    private static String formatStock(int stock) {
        return stock == 0 ? "Out of stock" : String.valueOf(stock);
    }

    // This method normalizes user input for easier comparison by converting to lowercase, replacing symbols, 
    // and converting plurals to singulars, so that inputs like "Shirts", "shirt", or "Shirt!" can all match the same product choice
    private static String normalizeChoice(String text) {
        // Makes user input easier to compare by lowering case and removing symbols
        String cleaned = text.toLowerCase().replace("&", " and ").replaceAll("[^a-z0-9 ]", " ").trim();
        if (cleaned.isEmpty()) {
            return cleaned;
        }

        // Converts plural words to singular words so that user inputs like "shirts" can match product aliases like "shirt"
        String[] words = cleaned.split("\\s+");
        StringBuilder normalized = new StringBuilder();
        for (String word : words) {
            // Converts plural words to simple singular words, like shirts to shirt
            if (normalized.length() > 0) {
                normalized.append(' ');
            }
            normalized.append(singularWord(word));
        }
        return normalized.toString();
    }

    // This method converts plural words to singular words for better matching of user input to product 
    // choices, so that "dresses" can match "dress", "boxes" can match "box", and "shirts" can match "shirt"
    private static String singularWord(String word) {
        // Small plural fixer para "dresses", "boxes", and "shirts" can match singular aliases
        if (word.length() > 3 && word.endsWith("ies")) {
            return word.substring(0, word.length() - 3) + "y";
        }
        // "sses" is a special case para hindi siya ma-singularize as "s"
        if (word.length() > 4 && word.endsWith("sses")) {
            return word.substring(0, word.length() - 2);
        }
        // "ches", "shes", "xes", and "zes" are common plural endings that can be singularized by removing the "es"
        if (word.length() > 4
                && (word.endsWith("ches") || word.endsWith("shes") || word.endsWith("xes") || word.endsWith("zes"))) {
            return word.substring(0, word.length() - 2);
        }
        // For regular plurals that just end with "s", the singular form can be obtained by removing the last "s"
        if (word.length() > 2 && word.endsWith("s") && !word.endsWith("ss")) {
            return word.substring(0, word.length() - 1);
        }

        return word;
    }
    // This method is a simple helper to create an array of aliases from a variable number 
    // of string arguments, used for product options and choices
    private static String[] alias(String... aliases) {
        return aliases;
    }

    // This method creates an array of ProductOption objects from an array of option names, 
    //  assigning aliases and starting stock to each option,
    private static ProductOption[] choices(String... names) {
        // Converts simple text choices into ProductOption objects with their own stock
        ProductOption[] options = new ProductOption[names.length];
        for (int i = 0; i < names.length; i++) {
            options[i] = new ProductOption(names[i], optionAliases(names[i]));
        }
        return options;
    }

    private static String[] optionAliases(String name) {
        String normalized = normalizeChoice(name);

        if (normalized.endsWith("kg")) {
            return alias(normalized.replace("kg", " kg"));
        }

        if (normalized.endsWith("oz")) {
            return alias(normalized.replace("oz", " oz"), normalized.replace(" oz", " ounce"), normalized.replace(" oz", " ounces"));
        }

        // Adds word aliases for sizes so user can type "medium" instead of just "M"
        if (name.equalsIgnoreCase("XS")) {
            return alias("extra small", "x small");
        }
        if (name.equalsIgnoreCase("S")) {
            return alias("small");
        }
        if (name.equalsIgnoreCase("M")) {
            return alias("medium");
        }
        if (name.equalsIgnoreCase("L")) {
            return alias("large");
        }
        if (name.equalsIgnoreCase("XL")) {
            return alias("extra large", "x large");
        }
        if (name.equalsIgnoreCase("XXL")) {
            return alias("double extra large", "2xl", "two xl");
        }
        return alias();
    }

    // =========================
    // Helper Classes
    // =========================

    // This class represents a choice option in the menus, which can be selected by number or by matching any of its aliases, 
    // allowing for flexible user input
    private static class ChoiceOption {
        private final int number;
        private final List<String> aliases;

        ChoiceOption(int number, String label, String... aliases) {
            this.number = number;
            this.aliases = new ArrayList<>();
            // The displayed label is also saved as a valid input choice
            this.aliases.add(normalizeChoice(label));
            for (String alias : aliases) {
                // Each alias is normalized so matching is consistent
                this.aliases.add(normalizeChoice(alias));
            }
        }

        int getNumber() {
            return number;
        }

        boolean matches(String input) {
            // Checks if the user's word input matches any saved alias
            for (String alias : aliases) {
                if (alias.equals(input)) {
                    return true;
                }
            }
            return false;
        }
    }

    // This class represents a menu choice, which can be a category, cart, or checkout option,
    // and stores the relevant index or action type for the main loop to handle the user's navigation through the menus
    private static class MenuChoice {
        private final int index;
        private final String action;

        private MenuChoice(int index, String action) {
            this.index = index;
            this.action = action;
        }

        static MenuChoice category(int index) {
            // Category keeps the selected category index
            return new MenuChoice(index, "category");
        }

        static MenuChoice cart() {
            // Cart and checkout use action names instead of category index
            return new MenuChoice(-1, "cart");
        }

        static MenuChoice checkout() {
            return new MenuChoice(-1, "checkout");
        }

        int getIndex() {
            return index;
        }

        boolean isCart() {
            return action.equals("cart");
        }

        boolean isCheckout() {
            return action.equals("checkout");
        }
    }

    // This class represents a payment made by the user, storing the payment method, amount paid, and change,
    //  so that the receipt can display the payment details after checkout
    private static class Payment {
        private final String method;
        private final int amountPaid;
        private final int change;

        Payment(String method, int amountPaid, int change) {
            // Stores payment details so receipt can print them later
            this.method = method;
            this.amountPaid = amountPaid;
            this.change = change;
        }

        String getMethod() {
            return method;
        }

        int getAmountPaid() {
            return amountPaid;
        }

        int getChange() {
            return change;
        }

        boolean isCompleted() {
            return !method.equals("Back") && !method.equals("None");
        }
    }

    private static class Receipt {
        private final int number;
        private final List<ReceiptItem> items;
        private final Payment payment;

        Receipt(int number, List<ReceiptItem> items, Payment payment) {
            this.number = number;
            this.items = items;
            this.payment = payment;
        }
    }

    private static class ReceiptItem {
        private final String storeName;
        private final String productName;
        private final String choice;
        private final int quantity;
        private final int price;
        private final int subtotal;

        ReceiptItem(CartItem item) {
            this.storeName = item.getProduct().getStoreName();
            this.productName = item.getProduct().getName();
            this.choice = item.getChoiceDisplay();
            this.quantity = item.getQuantity();
            this.price = item.getProduct().getPrice();
            this.subtotal = item.getSubtotal();
        }
    }

    // This class represents a product option, which has its own name, aliases for user input, and separate stock quantity,
    //  allowing products to have different choices like sizes or SPF levels with their own stock management
    private static class ProductOption {
        private static final int STARTING_STOCK = 100;

        private final String name;
        private final String[] aliases;
        private int stock;

        ProductOption(String name, String... aliases) {
            this.name = name;
            this.aliases = aliases;
            // Each option like Medium or 50 SPF starts with its own stock
            this.stock = STARTING_STOCK;
        }

        String getName() {
            return name;
        }

        String[] getAliases() {
            List<String> names = new ArrayList<>();
            names.add(name);
            // Adds the option's real name plus extra aliases like "medium"
            names.addAll(Arrays.asList(aliases));
            return names.toArray(new String[0]);
        }

        int getStock() {
            return stock;
        }

        boolean removeStock(int quantity) {
            if (quantity > stock) {
                // Returning false tells the caller na kulang ang stock
                return false;
            }

            // Stock is reduced only after passing the stock check
            stock -= quantity;
            return true;
        }

        void addStock(int quantity) {
            stock += quantity;
            if (stock > STARTING_STOCK) {
                // Stock should not go above the original starting stock
                stock = STARTING_STOCK;
            }
        }
    }

    // This class represents a product sold in the store, which has a name, price, stock quantity, 
    // and optional choices like sizes or SPF levels, and methods to manage stock and display product 
    // information, allowing the main program to handle products with or without options in a consistent way
    private static class Product {
        private static final int STARTING_STOCK = 100;

        private final String storeName;
        private final String name;
        private final String optionLabel;
        private final List<ProductOption> options;
        private final int price;
        private final String[] aliases;
        private int stock;
        // For products without options, stock is managed at the product level. For products with options, 
        // stock is managed at the option level, and the product's own stock is not used for purchases.
        Product(String storeName, String name, String optionLabel, ProductOption[] options, int price, String... aliases) {
            this.storeName = storeName;
            this.name = name;
            this.optionLabel = optionLabel;
            // Options are stored in a List para madaling i-loop sa menus
            this.options = new ArrayList<>(Arrays.asList(options));
            this.price = price;
            this.aliases = aliases;
            this.stock = STARTING_STOCK;
        }

        String getStoreName() {
            return storeName;
        }

        String getName() {
            return name;
        }

        String getDisplayName() {
            return storeName + " " + name;
        }

        String getDisplayName(ProductOption option) {
            if (option == null) {
                // Products without size/type just use store + product name
                return getDisplayName();
            }

            // Products with options include the selected choice in the display name
            return getDisplayName() + " (" + optionLabel + ": " + option.getName() + ")";
        }

        String getOptionLabel() {
            return optionLabel;
        }

        String getOptionLabelDisplay() {
            return hasOptions() ? optionLabel : "-";
        }

        List<ProductOption> getOptions() {
            return options;
        }

        boolean hasOptions() {
            // True kapag product has choices like Size or Weight
            return !options.isEmpty();
        }

        String getShortOptions() {
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < options.size(); i++) {
                // Builds display text like S, M, L, XL
                if (i > 0) {
                    text.append(", ");
                }
                text.append(options.get(i).getName());
            }
            return text.toString();
        }

        String getShortOptionsDisplay() {
            return hasOptions() ? getShortOptions() : "-";
        }

        int getPrice() {
            return price;
        }

        int getStock() {
            return stock;
        }

        int getStock(ProductOption option) {
            if (option == null) {
                // Products without options use the product's own stock
                return stock;
            }

            // Products with options use the selected option's stock
            return option.getStock();
        }

        String getStockDisplay() {
            // Product table shows "100 each" when every option has separate stock
            return hasOptions() ? getOptionStockDisplay() : formatStock(stock);
        }

        private String getOptionStockDisplay() {
            boolean allOutOfStock = true;
            for (ProductOption option : options) {
                if (option.getStock() > 0) {
                    allOutOfStock = false;
                    break;
                }
            }

            return allOutOfStock ? "Out of stock" : "100 each";
        }

        String[] getAliases() {
            List<String> names = new ArrayList<>();
            names.add(name);
            names.add(storeName + " " + name);
            // Adds extra names so user can type shortcuts like shirt, shoes, or laptop
            names.addAll(Arrays.asList(aliases));
            return names.toArray(new String[0]);
        }

        boolean removeStock(ProductOption option, int quantity) {
            if (option != null) {
                // If product has a selected option, stock is removed from that option only
                return option.removeStock(quantity);
            }

            if (quantity > stock) {
                // Returning false means the requested quantity is too high
                return false;
            }

            // For products without options, reduce the main product stock
            stock -= quantity;
            return true;
        }

        void addStock(ProductOption option, int quantity) {
            if (option != null) {
                // Returned cart items go back to the exact option stock
                option.addStock(quantity);
                return;
            }

            stock += quantity;
            if (stock > STARTING_STOCK) {
                // Prevents stock from going above the starting value
                stock = STARTING_STOCK;
            }
        }
    }

    // This class represents a store brand, which has a name, aliases for user input, and a list of products sold under that brand,
    //  allowing the store to group products by brand and let users choose a brand when browsing 
    // products, with flexible input matching through aliases
    private static class StoreBrand {
        private final String name;
        private final String[] aliases;
        private final List<Product> products;

        StoreBrand(String name, String[] aliases, Product... products) {
            this.name = name;
            this.aliases = aliases;
            // Products are grouped under one store/brand
            this.products = new ArrayList<>(Arrays.asList(products));
        }

        String getName() {
            return name;
        }

        String[] getAliases() {
            List<String> names = new ArrayList<>();
            names.add(name);
            // Store aliases help match alternate spellings or shorter names
            names.addAll(Arrays.asList(aliases));
            return names.toArray(new String[0]);
        }

        List<Product> getProducts() {
            return products;
        }
    }

    // This class represents like the StoreBrand class represents the Categories
    private static class Category {
        private final String name;
        private final String[] aliases;
        private final List<StoreBrand> storeBrands;

        Category(String name, String[] aliases, StoreBrand... storeBrands) {
            this.name = name;
            this.aliases = aliases;
            // Store brands are grouped under one category
            this.storeBrands = new ArrayList<>(Arrays.asList(storeBrands));
        }

        String getName() {
            return name;
        }

        String[] getAliases() {
            List<String> names = new ArrayList<>();
            names.add(name);
            // Category aliases let the user type words like clothes, gadgets, or toys
            names.addAll(Arrays.asList(aliases));
            return names.toArray(new String[0]);
        }

        List<StoreBrand> getStoreBrands() {
            return storeBrands;
        }
    }

    // This class represents an item in the shopping cart, which has a product, a selected option, and a quantity
    private static class CartItem {
        private final Product product;
        private final ProductOption option;
        private int quantity;

        CartItem(Product product, ProductOption option, int quantity) {
            // Cart item remembers product, selected option, and quantity bought
            this.product = product;
            this.option = option;
            this.quantity = quantity;
        }

        Product getProduct() {
            return product;
        }

        ProductOption getOption() {
            return option;
        }

        int getQuantity() {
            return quantity;
        }

        String getDisplayName() {
            return product.getDisplayName(option);
        }

        String getChoiceDisplay() {
            // Receipt/cart table shows dash if the product has no option
            return option == null ? "-" : option.getName();
        }

        boolean matches(Product otherProduct, ProductOption otherOption) {
            if (product != otherProduct) {
                // Different product means this cart line is not the same item
                return false;
            }

            if (option == null || otherOption == null) {
                // Both options must be null to match products without choices
                return option == otherOption;
            }

            // Same product and same option name means quantity can be combined
            return option.getName().equals(otherOption.getName());
        }

        void addQuantity(int quantity) {
            this.quantity += quantity;
        }

        int getSubtotal() {
            // Subtotal is price times how many pieces are in this cart item
            return product.getPrice() * quantity;
        }
    }

    // This class represents the entire store, which contains a list of categories, and builds all the category, brand, 
    // and product data when initialized,
    //  allowing the main program to access the store's inventory and structure through a single Store object, and keeping
    //  all the data organized in one place with helper methods to create products with different options and categories
    //  with different brands
    private static class Store {
        private final List<Category> categories;

        Store() {
            categories = new ArrayList<>();
            // Builds all category, store, and product data when Store is created
            addProducts();
        }

        // This method returns the list of categories in the store, which is used by the main program 
        // to display category options and access products
        List<Category> getCategories() {
            return categories;
        }

        private Product product(String storeName, String name, String optionLabel, String[] options, int price, String... aliases) {
            // Creates a product with options like Size, SPF, or Weight
            return new Product(storeName, name, optionLabel, choices(options), price, aliases);
        }

        private Product product(String storeName, String name, int price, String... aliases) {
            // Creates a product without extra choices/options
            return new Product(storeName, name, "", new ProductOption[0], price, aliases);
        }

        private StoreBrand brand(String name, String[] aliases, Product... products) {
            // Shortcut method para mas maiksi gumawa ng StoreBrand sa product list
            return new StoreBrand(name, aliases, products);
        }

        private Product clothing(String storeName, String name, int price, String... aliases) {
            // Clothing products use the common clothing sizes
            return product(storeName, name, "Size", CLOTHING_SIZES, price, aliases);
        }

        private Product socks(String storeName, int price) {
            // Socks have their own size list
            return product(storeName, "Socks", "Size", SOCK_SIZES, price, "sock");
        }

        private Product shoes(String storeName, String name, int price, String... aliases) {
            // Regular shoes use number sizes 6 to 10
            return product(storeName, name, "Size", SHOE_SIZES, price, aliases);
        }

        private Product euroShoes(String storeName, String name, int price, String... aliases) {
            // Some brands use European shoe sizes
            return product(storeName, name, "Size", EURO_SHOE_SIZES, price, aliases);
        }

        private Product jeans(String storeName, String[] sizes, int price) {
            // Jeans can pass different waist size lists per brand
            return product(storeName, "Jeans", "Size", sizes, price, "jean", "pants", "pant");
        }

        private Product beauty(String storeName, String name, String label, String[] options, int price, String... aliases) {
            // Beauty products are created through one helper so the category list is cleaner
            return product(storeName, name, price, aliases);
        }

        private Product home(String storeName, String name, int price, String... aliases) {
            // Home products do not need sizes/options
            return product(storeName, name, price, aliases);
        }

        private Product electronic(String storeName, String name, int price, String... aliases) {
            // Electronics products do not need sizes/options
            return product(storeName, name, price, aliases);
        }

        private Product sportSized(String storeName, String name, int price, String... aliases) {
            // Some sports products use sizes
            return product(storeName, name, "Size", new String[] {"S", "M", "L", "XL", "XXL"}, price, aliases);
        }

        private Product sportWeighted(String storeName, String name, int price, String... aliases) {
            // Some sports products use weight options like 2 kg or 10 kg
            return product(storeName, name, "Weight", WEIGHT_CHOICES, price, aliases);
        }

        private Product sport(String storeName, String name, int price, String... aliases) {
            // Regular sports products do not need options
            return product(storeName, name, price, aliases);
        }

        private Product tumbler(String storeName, int price) {
            return product(storeName, "Tumbler", "Size", TUMBLER_SIZES, price, "water bottle", "bottle", "tumblers");
        }

        private Product toy(String storeName, String name, int price, String... aliases) {
            // Toy products do not need options
            return product(storeName, name, price, aliases);
        }

        private void addProducts() {
            // Adds all main categories to the store
            addClothing();
            addBeauty();
            addHomeAndLiving();
            addElectronics();
            addSports();
            addToys();
        }

        private void addClothing() {
            // Clothing category contains all clothing stores/brands
            categories.add(new Category(
                    "Clothing",
                    alias("clothes", "apparel", "wear"),
                    clothingBrand("H&M", alias("hm", "h and m")),
                    clothingBrand("Adidas", alias()),
                    clothingBrand("Uniqlo", alias()),
                    clothingBrand("Chanel", alias()),
                    zaraBrand(),
                    lacosteBrand(),
                    clothingBrand("Gap", alias()),
                    clothingBrand("Puma", alias())));
        }

        private StoreBrand clothingBrand(String storeName, String[] aliases) {
            // Reusable clothing brand template so similar brands share the same product list
            return brand(storeName, aliases,
                    clothing(storeName, "T-shirt", 399, "shirt", "shirts", "tshirt", "tshirts"),
                    clothing(storeName, "Polo shirt", 699, "polo", "polos", "polo shirts"),
                    clothing(storeName, "Hoodie", 1199, "hoodies"),
                    jeans(storeName, JEAN_SIZES, 1299),
                    clothing(storeName, "Shorts", 599, "short"),
                    clothing(storeName, "Dress", 999, "dresses"),
                    clothing(storeName, "Jacket", 1499, "jackets"),
                    clothing(storeName, "Underwear", 299, "briefs", "brief"));
        }

        private StoreBrand zaraBrand() {
            String storeName = "Zara";
            // Zara has a custom product list and a different jeans size list
            return brand(storeName, alias(),
                    clothing(storeName, "T-shirt", 499, "shirt", "shirts"),
                    clothing(storeName, "Blazer", 2499, "blazers"),
                    clothing(storeName, "Dress", 1599, "dresses"),
                    jeans(storeName, ZARA_JEAN_SIZES, 1699),
                    clothing(storeName, "Skirt", 899, "skirts"),
                    clothing(storeName, "Jacket", 1899, "jackets"),
                    clothing(storeName, "Polo shirt", 799, "polo", "polos"),
                    euroShoes(storeName, "Shoes", 1999, "shoe"));
        }

        private StoreBrand lacosteBrand() {
            String storeName = "Lacoste";
            // Lacoste has custom products like sneakers, cap, and socks
            return brand(storeName, alias(),
                    clothing(storeName, "Polo shirt", 2499, "polo", "polos"),
                    clothing(storeName, "T-shirt", 1499, "shirt", "shirts"),
                    clothing(storeName, "Hoodie", 3999, "hoodies"),
                    clothing(storeName, "Shorts", 1999, "short"),
                    shoes(storeName, "Sneakers", 3499, "sneaker", "shoes", "shoe"),
                    product(storeName, "Cap", "Size", SOCK_SIZES, 999, "caps", "hat", "hats"),
                    clothing(storeName, "Jacket", 4999, "jackets"),
                    socks(storeName, 599));
        }

        private void addBeauty() {
            // Beauty category contains makeup, skincare, and personal care brands
            categories.add(new Category(
                    "Beauty & Personal Care",
                    alias("beauty", "personal care", "care"),
                    watsonsBrand(),
                    avonBrand(),
                    niveaBrand("Livea", alias("nivea")),
                    maybellineBrand("Maveline", alias("maybelline")),
                    benefitBrand(),
                    cliniqueBrand(),
                    skintificBrand(),
                    macBrand()));
        }

        private StoreBrand watsonsBrand() {
            String storeName = "Watsons";
            // Watsons product list with common personal care items
            return brand(storeName, alias(),
                    beauty(storeName, "Facial cleanser", "Type", alias("whitening", "acne control", "hydrating", "oil control", "sensitive"), 249, "cleanser"),
                    beauty(storeName, "Shampoo", "Type", alias("anti dandruff", "smoothening", "hair fall control", "volumizing", "color care"), 189),
                    beauty(storeName, "Body wash", "Type", alias("moisturizing", "whitening", "antibacterial", "exfoliating", "refreshing"), 220),
                    beauty(storeName, "Lotion", "Type", alias("whitening", "SPF", "moisturizing", "firming", "soothing"), 210),
                    beauty(storeName, "Toothpaste", "Type", alias("whitening", "sensitive", "gum care", "herbal", "total protection"), 120),
                    beauty(storeName, "Sunscreen", "SPF", SPF_CHOICES, 349),
                    beauty(storeName, "Face mask", "Type", alias("sheet", "clay", "peel off", "hydrating", "brightening"), 99),
                    beauty(storeName, "Lip balm", "Type", alias("tinted", "medicated", "SPF", "flavored", "moisturizing"), 139));
        }

        private StoreBrand avonBrand() {
            String storeName = "Avon";
            // Avon product list with makeup and fragrance items
            return brand(storeName, alias(),
                    beauty(storeName, "Lipstick", "Shade", alias("nude", "pink", "red", "coral", "plum"), 299),
                    beauty(storeName, "Foundation", "Shade", alias("light", "beige", "natural", "tan", "deep"), 499),
                    beauty(storeName, "Mascara", "Type", alias("volumizing", "lengthening", "waterproof", "curling", "defining"), 299),
                    beauty(storeName, "Perfume", "Variant", alias("floral", "fruity", "woody", "fresh", "oriental"), 699),
                    beauty(storeName, "Face powder", "Shade", alias("ivory", "beige", "natural", "honey", "deep"), 349),
                    beauty(storeName, "Lotion", "Variant", alias("whitening", "moisturizing", "firming", "scented", "SPF"), 249),
                    beauty(storeName, "Eyeliner", "Type", alias("pencil", "liquid", "gel", "waterproof", "smudge proof"), 229),
                    beauty(storeName, "Blush", "Shade", alias("pink", "peach", "coral", "rose", "berry"), 329));
        }

        private StoreBrand niveaBrand(String storeName, String[] aliases) {
            // Nivea/Livea uses aliases so either spelling can be accepted
            return brand(storeName, aliases,
                    beauty(storeName, "Body lotion", "Type", alias("whitening", "extra white", "nourishing", "firming", "aloe"), 259),
                    beauty(storeName, "Face wash", "Type", alias("acne control", "whitening", "oil control", "hydrating", "men"), 229),
                    beauty(storeName, "Deodorant", "Type", alias("roll on", "spray", "stick", "whitening", "invisible"), 189),
                    beauty(storeName, "Lip balm", "Variant", alias("original", "cherry", "strawberry", "blackberry", "SPF"), 149),
                    beauty(storeName, "Cream", "Type", alias("soft", "moisturizing", "intensive", "anti aging", "men"), 199),
                    beauty(storeName, "Sunscreen", "SPF", SPF_CHOICES, 379),
                    beauty(storeName, "Body serum", "Type", alias("whitening", "radiant", "repair", "firming", "UV protect"), 329),
                    beauty(storeName, "Shower gel", "Variant", alias("fresh", "moisturizing", "relaxing", "exfoliating", "men"), 249));
        }

        private StoreBrand maybellineBrand(String storeName, String[] aliases) {
            // Maybelline/Maveline uses aliases so typed brand names still match
            return brand(storeName, aliases,
                    beauty(storeName, "Foundation", "Shade", alias("light", "natural", "beige", "warm", "deep"), 499),
                    beauty(storeName, "Lipstick", "Shade", alias("nude", "pink", "red", "mauve", "brown"), 349),
                    beauty(storeName, "Mascara", "Type", alias("volumizing", "lengthening", "waterproof", "curling", "washable"), 399),
                    beauty(storeName, "Concealer", "Shade", alias("light", "medium", "honey", "caramel", "deep"), 379),
                    beauty(storeName, "Eyeliner", "Type", alias("liquid", "gel", "pencil", "waterproof", "matte"), 299),
                    beauty(storeName, "Powder", "Shade", alias("light", "natural", "beige", "honey", "deep"), 349),
                    beauty(storeName, "Blush", "Shade", alias("pink", "coral", "peach", "berry", "rose"), 329),
                    beauty(storeName, "Brow pencil", "Shade", alias("light brown", "brown", "dark brown", "gray", "black"), 299));
        }

        private StoreBrand benefitBrand() {
            String storeName = "Benefit";
            // Benefit product list with brow, blush, and primer products
            return brand(storeName, alias(),
                    beauty(storeName, "Brow gel", "Shade", alias("light", "medium", "dark", "deep", "clear"), 899),
                    beauty(storeName, "Mascara", "Type", alias("volumizing", "lengthening", "curling", "waterproof", "mini"), 999),
                    beauty(storeName, "Blush", "Shade", alias("pink", "peach", "coral", "rose", "berry"), 1299),
                    beauty(storeName, "Primer", "Type", alias("pore minimizing", "hydrating", "matte", "brightening", "smoothing"), 1499),
                    beauty(storeName, "Highlighter", "Shade", alias("pearl", "champagne", "gold", "pink", "bronze"), 1399),
                    beauty(storeName, "Lip tint", "Shade", alias("rose", "cherry", "coral", "nude", "berry"), 899),
                    beauty(storeName, "Setting spray", "Type", alias("matte", "dewy", "long wear", "hydrating", "refreshing"), 1199),
                    beauty(storeName, "Brow pencil", "Shade", alias("blonde", "light brown", "brown", "dark brown", "black"), 999));
        }

        private StoreBrand cliniqueBrand() {
            String storeName = "Clinique";
            // Clinique product list with higher priced skincare and makeup
            return brand(storeName, alias(),
                    beauty(storeName, "Foundation", "Shade", alias("very light", "light", "medium", "tan", "deep"), 1899),
                    beauty(storeName, "Moisturizer", "Type", alias("gel", "lotion", "cream", "oil free", "hydrating"), 1499),
                    beauty(storeName, "Cleanser", "Type", alias("mild", "oily skin", "dry skin", "combo skin", "acne"), 1199),
                    beauty(storeName, "Lipstick", "Shade", alias("nude", "pink", "red", "berry", "coral"), 1299),
                    beauty(storeName, "Serum", "Type", alias("anti aging", "brightening", "hydrating", "repair", "firming"), 2199),
                    beauty(storeName, "Sunscreen", "SPF", alias("30", "40", "50", "60", "100"), 1699),
                    beauty(storeName, "Eye cream", "Type", alias("anti dark circle", "firming", "hydrating", "smoothing", "brightening"), 1999),
                    beauty(storeName, "Face mist", "Type", alias("hydrating", "refreshing", "calming", "glow", "oil control"), 999));
        }

        private StoreBrand skintificBrand() {
            String storeName = "Skintific";
            // Skintific product list with skincare focused products
            return brand(storeName, alias(),
                    beauty(storeName, "Moisturizer", "Type", alias("barrier repair", "hydrating", "brightening", "acne", "calming"), 599),
                    beauty(storeName, "Cleanser", "Type", alias("gentle", "acne control", "oil control", "hydrating", "sensitive"), 399),
                    beauty(storeName, "Sunscreen", "SPF", SPF_CHOICES, 499),
                    beauty(storeName, "Serum", "Type", alias("niacinamide", "salicylic", "brightening", "hydrating", "repair"), 599),
                    beauty(storeName, "Toner", "Type", alias("exfoliating", "hydrating", "soothing", "brightening", "acne"), 449),
                    beauty(storeName, "Cushion foundation", "Shade", alias("light", "natural", "beige", "honey", "deep"), 699),
                    beauty(storeName, "Face mask", "Type", alias("clay", "sheet", "calming", "brightening", "acne"), 349),
                    beauty(storeName, "Essence", "Type", alias("hydrating", "repairing", "glow", "calming", "anti aging"), 549));
        }

        private StoreBrand macBrand() {
            String storeName = "MAC";
            // MAC has an alias for "mac cosmetics"
            return brand(storeName, alias("mac cosmetics"),
                    beauty(storeName, "Lipstick", "Shade", alias("nude", "pink", "red", "brown", "plum"), 1299),
                    beauty(storeName, "Foundation", "Shade", alias("fair", "light", "medium", "tan", "deep"), 2499),
                    beauty(storeName, "Concealer", "Shade", alias("light", "medium", "warm", "tan", "deep"), 1599),
                    beauty(storeName, "Powder", "Shade", alias("light", "medium", "tan", "dark", "deep"), 1799),
                    beauty(storeName, "Eyeshadow", "Shade", alias("neutral", "bronze", "pink", "smoky", "colorful"), 1499),
                    beauty(storeName, "Blush", "Shade", alias("peach", "pink", "coral", "rose", "berry"), 1699),
                    beauty(storeName, "Highlighter", "Shade", alias("gold", "champagne", "pearl", "pink", "bronze"), 1899),
                    beauty(storeName, "Setting spray", "Type", alias("matte", "dewy", "long lasting", "hydrating", "fixing"), 1599));
        }

        private void addHomeAndLiving() {
            // Home and Living category contains furniture and household stores
            categories.add(new Category(
                    "Home and Living",
                    alias("home", "living", "house", "household"),
                    homeBrand("IKEA"),
                    homeBrand("Mandaue", alias("mandaue foam")),
                    homeBrand("SM Home"),
                    homeBrand("AllHome"),
                    homeBrand("Philux"),
                    homeBrand("CrateBarrel", alias("crate and barrel", "crate barrel")),
                    homeBrand("PotteryBarn", alias("pottery barn")),
                    homeBrand("San-Yang")));
        }

        private StoreBrand homeBrand(String storeName) {
            // If no aliases are needed, this sends an empty alias list
            return homeBrand(storeName, alias());
        }

        private StoreBrand homeBrand(String storeName, String[] aliases) {
            // Reusable home store template with furniture and decor products
            return brand(storeName, aliases,
                    home(storeName, "Sofa", 12999, "couch"),
                    home(storeName, "Dining table", 8999, "table"),
                    home(storeName, "Bed frame", 9999, "bed"),
                    home(storeName, "Mattress", 7999),
                    home(storeName, "Cabinet", 5999),
                    home(storeName, "Bookshelf", 3499, "shelf", "shelves"),
                    home(storeName, "Lamp", 1499, "lamps"),
                    home(storeName, "Curtains", 999, "curtain"));
        }

        private void addElectronics() {
            // Electronics category contains tech and appliance stores
            categories.add(new Category(
                    "Electronics",
                    alias("electronic", "gadgets", "tech", "technology"),
                    electronicsBrand("Octagon"),
                    electronicsBrand("ElectroWorld"),
                    electronicsBrand("Abenson"),
                    electronicsBrand("Robinsons", alias("robinson appliance", "robinsons appliance")),
                    electronicsBrand("5th Avenue", alias("5th avenue electronic city", "fifth avenue electronic city")),
                    electronicsBrand("ASUS"),
                    electronicsBrand("MSI"),
                    electronicsBrand("Dell")));
        }

        private StoreBrand electronicsBrand(String storeName) {
            // If no aliases are needed, this sends an empty alias list
            return electronicsBrand(storeName, alias());
        }

        private StoreBrand electronicsBrand(String storeName, String[] aliases) {
            // Reusable electronics store template with common gadgets
            return brand(storeName, aliases,
                    electronic(storeName, "Laptop", 35999, "laptops"),
                    electronic(storeName, "Monitor", 8999, "monitors"),
                    electronic(storeName, "Keyboard", 1499, "keyboards"),
                    electronic(storeName, "Wireless mouse", 899, "mouse", "mice"),
                    electronic(storeName, "Headset", 1999, "headsets"),
                    electronic(storeName, "Printer", 6499, "printers"),
                    electronic(storeName, "Tablet", 12999, "tablets"),
                    electronic(storeName, "Powerbank", 1499, "power bank", "power banks"));
        }

        private void addSports() {
            // Sports category contains sports and lifestyle brands
            categories.add(new Category(
                    "Sports & Lifestyle",
                    alias("sports", "sport", "lifestyle", "fitness"),
                    sportsBrand("Toby's", alias("tobys sport", "toby sport", "toby's sport", "toby's sports")),
                    sportsBrand("Planet Sports", alias("planet sport")),
                    sportsBrand("SportsCenter", alias("sports central", "sports center")),
                    sportsBrand("SportsHouse", alias("sports house")),
                    sportsBrand("JD Sport", alias("jd sports")),
                    sportsBrand("SportsDirect", alias("sports direct")),
                    sportsBrand("Gribd", alias()),
                    sportsBrand("Oakley", alias())));
        }

        private StoreBrand sportsBrand(String storeName, String[] aliases) {
            // Reusable sports store template with equipment, shoes, and training items
            return brand(storeName, aliases,
                    sport(storeName, "Basketball", 999, "ball"),
                    shoes(storeName, "Running shoes", 2499, "shoes", "shoe"),
                    sportSized(storeName, "Training shirt", 699, "shirt"),
                    sport(storeName, "Gym bag", 899, "bag"),
                    sportWeighted(storeName, "Dumbbells", 1299, "dumbbell", "weights"),
                    sportWeighted(storeName, "Kettlebell", 1199, "kettlebells"),
                    sportWeighted(storeName, "Medicine ball", 1099, "medicine balls"),
                    tumbler(storeName, 299));
        }

        private void addToys() {
            // Toys category contains toy and entertainment stores
            categories.add(new Category(
                    "Toys & Entertainment",
                    alias("toys", "toy", "entertainment", "games", "game"),
                    toyBrand("ToyKingdom", "Toy Kingdom"),
                    toyBrand("Toys R Us", "Toys\"R'Us"),
                    toyBrand("Hamleys"),
                    toyBrand("Toytown"),
                    toyBrand("Filbar's"),
                    toyBrand("Funko"),
                    toyBrand("MR.TOY"),
                    toyBrand("PopMart")));
        }

        private StoreBrand toyBrand(String storeName) {
            // Uses the same name as the alias if no special alias is given
            return toyBrand(storeName, storeName);
        }

        private StoreBrand toyBrand(String storeName, String aliasName) {
            // Reusable toy store template with common toy products
            return brand(storeName, alias(aliasName),
                    toy(storeName, "Board game", 699, "board games"),
                    toy(storeName, "Action figure", 599, "figure"),
                    toy(storeName, "Puzzle set", 399, "puzzle"),
                    toy(storeName, "Building blocks", 999, "blocks"),
                    toy(storeName, "Toy car", 299, "car"),
                    toy(storeName, "Doll", 499, "dolls"),
                    toy(storeName, "Card game", 349, "cards"),
                    toy(storeName, "Collectible figure", 899, "collectible"));
        }
    }
}
