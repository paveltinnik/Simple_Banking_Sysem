# Simple Banking System
To run the application without progress line type in cmd "gradle --console plain run"

This application simulates a simple banking system. There are three items in the start menu: 
1. Create an account (Generates bank card number and pin code. The card number passes the Luhn algorithm. Data is written to the database)
2. Log into account (Requests the card number and pin code to enter the account)
0. Exit

When you login in the account, the following menu appears:
1. Balance (Shows your balance)
2. Add income (Requests the amount and adds to your balance)
3. Do transfer (Transfer funds to another account)
4. Close account (Removes an account from the database)
5. Log out (Log out and then the start menu will appear)
0. Exit

When making a transfer, first the compliance with the Luhn algorithm is checked, then the existence of the account is checked, then the transfer amount is entered. 
The transfer is made with a sufficient amount of funds in your account.
