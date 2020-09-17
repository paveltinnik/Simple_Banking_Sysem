package banking;

import java.util.*;
import java.sql.*;
import org.sqlite.*;

public class Main extends AccountMenu{
    public static void main(String[] args) throws Exception {
        CreateTable.createTable();
        showStartMenu();
    }
}

class Connect {
    private static Connection con = null;

    public static Connection getConnection() throws Exception {
        if (con == null) {
            String url = "jdbc:sqlite:card.s3db";

            SQLiteDataSource dataSource = new SQLiteDataSource();
            dataSource.setUrl(url);

            con = dataSource.getConnection();
        }
        return con;
    }
}

class CreateTable {
    public static void createTable() throws Exception {
        try (Statement statement = Connect.getConnection().createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS card (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "number TEXT," +
                    "pin TEXT," +
                    "balance INTEGER DEFAULT 0)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

class Account {
    static List<Account> accounts = new ArrayList<>();

    private String cardNumber;

    Account(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    String getCardNumber() {
        return cardNumber;
    }
}

class AccountCreator extends StartMenu {
    static Random random = new Random();

    // Проверка алгоритмом Луна
    public static int luhnAlgorithm(String cardNumber) {
        // Разделяем номер карты на массив из String длиной 16
        String[] originalCardNumber = cardNumber.split("");

        // Создаем пустой массив длиной 16
        int[] cardNumberInt = new int[originalCardNumber.length];
        // Сумма цифр номера карты для проверки алгоритма
        int sum = 0;
        int returnNumber = 0;

        for (int i = 0; i < originalCardNumber.length; i++) {
            // Каждый второй номер маасива умножаем на 2
            if (i % 2 == 0) {
                cardNumberInt[i] = Integer.parseInt(originalCardNumber[i]) * 2;
            } else {
                cardNumberInt[i] = Integer.parseInt(originalCardNumber[i]);
            }
        }

        for (int i = 0; i < originalCardNumber.length; i++) {
            // Если i-тый номер массива больше 9, то отнимаем 9
            if (cardNumberInt[i] > 9) {
                cardNumberInt[i] -= 9;
            }
        }

        for (int i = 0; i < originalCardNumber.length; i++) {
            // Суммируем i-тую цифру массива
            sum += cardNumberInt[i];
        }

        if (sum % 10 != 0) {
            returnNumber = 10 - sum % 10;
        }

        return returnNumber;
    }

    // Генерируем номер карты
    public static String generateCardNumber() {

        // Сгенерировать рандомную карту
        String originalCardNumber = "400000" + String.format("%05d", random.nextInt(100000))
                + String.format("%04d", random.nextInt(10000)) + "0";

        StringBuilder cardNumber = new StringBuilder(originalCardNumber);

        // Заменить последнюю цифру на валидную
        cardNumber.replace(15, 16, String.valueOf(luhnAlgorithm(originalCardNumber)));

        return cardNumber.toString();
    }

    // Генерируем пин карты
    static String generatePin() {
        return String.format("%04d", random.nextInt(10000));
    }

    /** Создаем данные аккаунта и записываем в БД */
    public static void createAccount() throws Exception {
        // Сгенерируем номер карты и пин
        String cardNumber = generateCardNumber();
        String pin = generatePin();

        // Создадим запрос на добавление данных в таблицу
        String sql = "INSERT INTO card(number, pin) VALUES (?, ?)";

        try (PreparedStatement pstmt = Connect.getConnection().prepareStatement(sql)) {

            pstmt.setString(1, cardNumber);
            pstmt.setString(2, pin);

            pstmt.executeUpdate();

            System.out.println("\nYour card has been created");
            System.out.println("Your card number:\n" + cardNumber);
            System.out.println("Your card PIN:\n" + pin);
        }
        catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        showStartMenu();
    }
}

class StartMenu {

    public static boolean isExit = false;
    public static boolean isExitFromChildMenu = false;

    static Scanner scanner = new Scanner(System.in);

    // Показать стартовое меню
    public static void showStartMenu() throws Exception {
        System.out.println("\n1. Create an account\n" +
                "2. Log into account\n" +
                "0. Exit");

        while (!isExit) {
            switch (scanner.nextLine()) {
                case "1":
                    AccountCreator.createAccount();
                    break;
                case "2":
                    logIntoAccount();
                    break;
                case "0":
                    exit();
                    break;
            }
        }
    }

    public static void exit() {
        isExit = true;
        System.out.print("\nBye!\n");
        System.exit(0);
    }

    private static void logIntoAccount() throws Exception {
        System.out.println("\nEnter your card number:");
        String cardNumber = scanner.nextLine();

        System.out.println("Enter your PIN:");
        String cardPin = scanner.nextLine();

        try (Statement stmt = Connect.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM card")) {

            // Производим чтение данных из БД
            while (rs.next()) {
                // Retrieve column values
                String number = rs.getString("number");
                String pin = rs.getString("pin");

                // Сравниваем номер карты и пин, чтобы войти в аакаунт
                if (cardNumber.equals(number) && cardPin.equals(pin)) {

                    System.out.println("\nYou have successfully logged in!");
                    Account.accounts.add(new Account(number));
                    AccountMenu.showAccountMenu();

                    break;
                }
            }

            System.out.println("\nWrong card number or PIN!");
            showStartMenu();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}

class AccountMenu extends StartMenu {

    // Показать меню аккаунта
    public static void showAccountMenu() throws Exception {
        isExitFromChildMenu = false;

        System.out.println("\n1. Balance\n" +
                "2. Add income\n" +
                "3. Do transfer\n" +
                "4. Close account\n" +
                "5. Log out\n" +
                "0. Exit");

        while (!isExitFromChildMenu) {
            switch (scanner.nextLine()) {
                case "1":
                    showBalance();
                    break;
                case "2":
                    addIncome();
                    break;
                case "3":
                    doTransfer();
                    break;
                case "4":
                    closeAccount();
                    break;
                case "5":
                    logOutFromAccount();
                    break;
                case "0":
                    exit();
                    break;
            }
        }
    }

    public static void showBalance() throws Exception {

        String cardNumber = Account.accounts.get(0).getCardNumber();

        // Получаем значение баланса у выбранной карты
        String sql = "SELECT balance FROM card WHERE number = ?";

        try (PreparedStatement pstmt = Connect.getConnection().prepareStatement(sql)) {

            pstmt.setString(1, cardNumber);
            ResultSet rs = pstmt.executeQuery();
            String balance = rs.getString("balance");

            System.out.println("\nBalance: " + balance);

            showAccountMenu();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void addIncome() throws Exception {
        System.out.println("\nEnter income:");
        int balance = scanner.nextInt();
        System.out.println("Income was added!");

        String cardNumber = Account.accounts.get(0).getCardNumber();

        // Создадим запрос на изменение баланса у данной карты
        String sql = "UPDATE card SET balance = balance + ? WHERE number = ?";

        try (PreparedStatement pstmt = Connect.getConnection().prepareStatement(sql)) {

            pstmt.setInt(1, balance);
            pstmt.setString(2, cardNumber);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        showAccountMenu();
    }

    public static void doTransfer() throws Exception {
        System.out.println("\nEnter card number:");
        String transferToCard = scanner.next();

        String cardNumber = Account.accounts.get(0).getCardNumber();

        // Сначала проверяем по алгоритму Луна
        if (AccountCreator.luhnAlgorithm(transferToCard) == 0) {

            // Создадим запрос на поиск карты, на которую производится перевод
            String sql = "SELECT COUNT(*) FROM card WHERE number = ?";

            try (PreparedStatement pstmt = Connect.getConnection().prepareStatement(sql)) {

                pstmt.setString(1, transferToCard);
                ResultSet rs = pstmt.executeQuery();

                // Проверяем имеется ли карта в базе данных
                int count = 0;

                try {
                    rs.next();
                    count = rs.getInt(1);
                } catch (SQLException e) {
                    System.out.println(e.getMessage());
                }

                // Если карта имеется в БД
                if (count != 0) {
                    // Получаем значение, которое пользователь хочет перевести
                    System.out.println("Enter how much money you want to transfer:");
                    int transferMoney = scanner.nextInt();

                    String sql2 = "SELECT balance FROM card WHERE number = ?";

                    try (PreparedStatement pstmt2 = Connect.getConnection().prepareStatement(sql2)) {

                        pstmt2.setString(1, cardNumber);
                        ResultSet rs2 = pstmt2.executeQuery();
                        String balance = rs2.getString("balance");

                        // Если недостаточно средств для перевода
                        if (Integer.parseInt(balance) - transferMoney < 0) {
                            System.out.println("Not enough money!");
                        } else {
                            // Создадим запрос на изменение баланса
                            String sql3 = "UPDATE card SET balance = balance + ? WHERE number = ?";
                            String sql4 = "UPDATE card SET balance = balance - ? WHERE number = ?";

                            try (PreparedStatement pstmt3 = Connect.getConnection().prepareStatement(sql3)) {

                                pstmt3.setInt(1, transferMoney);
                                pstmt3.setString(2, transferToCard);
                                pstmt3.executeUpdate();
                            }

                            try (PreparedStatement pstmt4 = Connect.getConnection().prepareStatement(sql4)) {

                                pstmt4.setInt(1, transferMoney);
                                pstmt4.setString(2, cardNumber);
                                pstmt4.executeUpdate();
                            }
                            System.out.println("Success!");
                        }
                    }
                    // Если карта не имеется в базе данных
                } else {
                    System.out.println("Such a card does not exist.");
                }
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
            // Если введенная карта не проходит по алгоритму Луна
        } else {
            System.out.println("\nProbably you made mistake in the card number. Please try again!");
        }
        showAccountMenu();
    }

    public static void closeAccount() throws Exception {

        String cardNumber = Account.accounts.get(0).getCardNumber();

        // Создадим запрос на удаление аккаунта
        String sql = "DELETE FROM card WHERE number = ?";

        try (PreparedStatement pstmt = Connect.getConnection().prepareStatement(sql)) {

            pstmt.setString(1, cardNumber);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        // Очищаем лист, в котором записан аккаунт
        Account.accounts.clear();

        System.out.println("\nThe account has been closed!");

        isExitFromChildMenu = true;
        showStartMenu();
    }

    public static void logOutFromAccount() throws Exception{
        isExitFromChildMenu = true;

        // Очищаем лист, в котором записан аккаунт
        Account.accounts.clear();
        showStartMenu();
    }
}
