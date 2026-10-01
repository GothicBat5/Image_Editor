#include <iostream>
#include <array>
#include <map>
#include <limits>
#include <string>

struct Room {
    std::string customer = "Vacant"; 

    bool isVacant() const
    {
        return customer == "Vacant";
    }
};

bool LogIn()
{
    
    std::map<std::string, std::string> users = {
        {"Emma", "5655"},
        {"Justine", "8902"},
        {"Claude", "1236"},
        {"Miller", "3279"},
        {"Laura", "4350"}
    };

    std::string userN, passP;

    std::cout << "LOG IN\n";
    std::cout << "Enter username: ";
    std::cin >> userN;

    std::cout << "Pin pass: ";
    std::cin >> passP;

    auto it = users.find(userN);  
    
    // Fix 1: 'if' → 'it'
    if (it != users.end() && it-> second == passP)
    {
        std::cout << "Welcome " << userN << "!\n";
        return true;
    }

    std::cout << "Invalid Input.\nMake sure it was correct.\n";
    return false;
}

int getValidatedInt(const std::string& message)
{
    int value;

    while (true)
    {
        std::cout << message;
        std::cin >> value;

        if (std::cin.fail() || value <= 0)
        {
            std::cin.clear();
            std::cin.ignore(std::numeric_limits<std::streamsize>::max(), '\n');
            std::cout << "\nInvalid Input. Try again.\n";
        }
            
        else
        {
            return value;
        }
    }
}

void HandleRev(std::array<Room, 5>& rooms)
{
    int roomN;

    std::cout << "Enter room number: ";
    std::cin >> roomN;

    if (roomN < 1 || roomN > 5)
    {
        std::cout << "\nInvalid room number.\n";
        return;
    }

    Room& selected = rooms[roomN - 1];

    if (!selected.isVacant())  //added ()
    {
        std::cout << "Room is already occupied by " << selected.customer << "\n";
        return;
    }

    std::cout << "Enter name: ";
    std::cin >> selected.customer;

    int hrs = getValidatedInt("Enter number of hours: ");
    int rate = getValidatedInt("Enter rate per hour: ");
    int total = hrs * rate;

    std::cout << "Total Cost: P" << total << "\n";

    int payment = getValidatedInt("Enter payment: ");
    
    if (payment >= total)
    {
        std::cout << "Change: P" << (payment - total) << "\n";
    }
    
    else
    {
        std::cout << "\nInsufficient payment.\n";
        selected.customer = "Vacant";
        return;
    }

    std::cout << "Reservation Successful.\n";
}

void showR(const std::array<Room, 5>& rooms)
{
    std::cout << "\nRoom Status:\n";
    
    for (size_t i = 0; i < rooms.size(); ++i)
    {
        std::cout << "Room " << i + 1 << ": " << rooms[i].customer << "\n";
    }
    std::cout << "\n";
}

int main()
{
    std::array<Room, 5> rooms;

    while (true)
    {
        if (!LogIn())
        {
            continue;
        }

        char choice;

        do {
            showR(rooms);

            std::cout << "[1] Check In\n[2] Exit\n";
            std::cout << "Choice: ";
            std::cin  >> choice;

            switch (choice)
            {
                case '1': HandleRev(rooms); break;
                case '2': std::cout << "\nGoodbye. Program Ended.\n"; return 0;
                default:  std::cout << "\nInvalid Choice.\n";
            }
        }
        while (true);
    }
}
