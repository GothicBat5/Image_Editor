#include <cmath>
#include <cstdlib>
#include <cstring>
#include <iostream>
#include <string>
#include <vector>

class Movement
{
public:
    int x = 0;
    int y = 0;

    void Left()
    {
        x--;
    }

    void Right()
    {
        x++;
    }

    void Up()
    {
        y++;
    }

    void Down()
    {
        y--;
    }

    void ShowPosition()
    {
        std::cout <<"Position: x="<<x<<" y="<<y<<'\n';
    }
};

int main()
{
    Movement mov;

    int ch;

    std::cout << "=== Coordinate Simulator ===\n";
    std::cout << "1 = Left\n";
    std::cout << "2 = Right\n";
    std::cout << "3 = Up\n";
    std::cout << "4 = Down\n";
    std::cout << "0 = Exit\n";

    while (true)
    {
        std::cout << "\nInput >> ";
        std::cin >> ch;

        switch (ch)
        {
            case 1: mov.Left(); break;

            case 2: mov.Right(); break;

            case 3: mov.Up(); break;

            case 4: mov.Down(); break;

            case 0: std::cout << "Goodbye!\n"; return 0;

            default: std::cout << "Unknown command.\n"; continue;
        }

        mov.ShowPosition();
    }
}
